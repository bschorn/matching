/*
 *  The MIT License
 *
 * Copyright 2021 bschorn.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package org.bryan.schorn.tha.matching;

import org.bryan.schorn.tha.matching.engine.*;
import org.bryan.schorn.tha.matching.engine.rule.CheckRequiredFields;
import org.bryan.schorn.tha.matching.engine.rule.OrderThrottleRule;
import org.bryan.schorn.tha.matching.engine.rule.ProductHalted;
import org.bryan.schorn.tha.matching.model.Order;
import org.bryan.schorn.tha.matching.model.Trade;
import org.bryan.schorn.tha.matching.order.OrderFeed;
import org.bryan.schorn.tha.matching.product.ProductFeed;
import org.bryan.schorn.tha.matching.order.Orders;
import org.bryan.schorn.tha.matching.product.Products;
import org.bryan.schorn.tha.matching.util.CommandLineArgs;
import org.bryan.schorn.tha.matching.util.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.*;


/**
 * Entry Point
 */
public class MEService {

    static private final Logger LGR = LoggerFactory.getLogger(MEService.class);

    private Properties properties;

    enum State {
        INIT,
        START,
        RUNNING,
        STOP;
    }
    private State state = State.INIT;
    private ExecutorService executorService = null;
    // Orders
    private OrderFeed orderFeed = null;
    private Future<Integer> futureOrderFeed = null;
    // Engine
    private Engine engine = null;
    private Future<Integer> futureEngine = null;
    // Order Reject File
    private ActivityLog<Order.Reject> activityLogOrderReject = null;
    private Future<Integer> futureActivityLogOrderReject = null;
    // Trade File
    private ActivityLog<Trade> activityLogTrade = null;
    private Future<Integer> futureActivityLogTrade = null;

    /**
     * Configuration Entry Point
     *
     * @param properties
     */
    MEService(Properties properties) {
        this.properties = properties;
    }

    /**
     * Init Service
     *
     * @throws Exception
     */
    public void init() throws Exception {
        if (this.state != State.INIT) {
            LGR.error("Invalid State for calling init()");
            return;
        }
        /**
         * There are four working threads.
         * 1) OrderFeed
         * 2) Engine (Matching)
         * 3) Logging Trades
         * 4) Logging Rejects
         */
        this.executorService = Executors.newFixedThreadPool(4);

        /**
         * Products are read/loaded completely on the main thread before
         * any activity is started.
         */
        ProductFeed productFeed = ProductFeed.create(this.properties);
        productFeed.connect();
        Products.setFeed(productFeed);

        /**
         * Orders are streamed from a file and will be running
         * in its own thread.
         */
        this.orderFeed = OrderFeed.create(this.properties);
        this.orderFeed.connect();
        Orders.setFeed(this.orderFeed);

        /**
         * Engine reads orders from a supplier in its own thread.
         * Each new order is either rejected, matched or placed
         * in the OrderBook.
         */
        // create single engine with all products
        this.engine = new Engine(Products.findAll());

        /**
         * Engine rules can be custom built by deriving from the
         * Engine.Rule interface and added to the Engine.
         * The following rules are prebuilt and have static
         * instances declared that can be readily used.
         */
        // add rule for enforcing required fields
        this.engine.addRule(CheckRequiredFields.CHECK_REQUIRED_FIELDS);
        // add rule for trade halts
        this.engine.addRule(ProductHalted.PRODUCTED_HALTED);
        // add rule for the 3 orders in one second
        this.engine.addRule(OrderThrottleRule.MAX_THREE_PER_SECOND);

        /**
         * Activity Logs can be used to read from a supplier
         * and write to a file. Since we can write Trades and
         * Rejects as soon as they happen, we have these two
         * pulling Trades and Rejects from the Engine as
         * they are being created.
         */
        // create ActivityLog instance to log trades to file
        this.activityLogTrade = new ActivityLog<>(
                this.engine.getSupplier(Trade.class),
                this.properties.getProperty("TradeFile"),
                this.properties.getProperty("TradeFileHeader"));

        // create ActivityLog instance to log order rejects to file
        this.activityLogOrderReject = new ActivityLog<>(
                this.engine.getSupplier(Order.Reject.class),
                this.properties.getProperty("RejectedFile"),
                this.properties.getProperty("RejectedFileHeader"));


        // update state
        this.state = State.START;
    }

    /**
     * Start Service
     *
     * @throws Exception
     */
    public void start() {
        if (this.state != State.START) {
            LGR.error("Invalid State for calling start()");
            return;
        }
        this.state = State.RUNNING;

        /**
         * Submit the working instances to the Executor Service
         * to be run and keep a Future instance for later.
         */
        this.futureOrderFeed = executorService.submit(this.orderFeed);
        this.futureEngine = executorService.submit(this.engine);
        this.futureActivityLogTrade = executorService.submit(this.activityLogTrade);
        this.futureActivityLogOrderReject = executorService.submit(this.activityLogOrderReject);

        /**
         * Loop, Check Status, Wait
         */
        Integer ordersReceived = null;
        Integer ordersProcessed = null;
        while (this.state == State.RUNNING) {
            try {
                if (ordersReceived == null && this.futureOrderFeed.isDone()) {
                    ordersReceived = this.futureOrderFeed.get();
                    // Since that's the end of the OrderFeed, we tell the Engine
                    // to stop where there are no more Orders.
                    this.engine.stop();
                } else if (ordersProcessed == null && this.futureEngine.isDone()) {
                    ordersProcessed = this.futureEngine.get();
                    // the engine has completed, so let's start shutting down
                    // this service
                    this.stop();
                } else {
                    Thread.sleep(200);
                }
            } catch (InterruptedException ie) {
                LGR.warn(ie.getMessage());
            } catch (Exception ex) {
                LGR.error(ToString.stackTrace(ex));
            }
        }
        LGR.info("{} orders received", ordersReceived);
        LGR.info("{} orders processed", ordersProcessed);
    }


    /**
     * Stop Service
     */
    public void stop() {
        if (this.state != State.RUNNING) {
            LGR.error("Invalid State for calling stop()");
            return;
        }
        /**
         * The Activity Logs (Trades,Rejects) don't know when to quit
         * so they have to be told.
         */
        this.activityLogOrderReject.stop();
        this.activityLogTrade.stop();
        this.state = State.STOP;
    }

    public void close() {
        try {
            /**
             * Once everything has completed we can capture the OrderBooks' state.
             */
            LGR.info("Writing order books...");
            ActivityLog<OrderBook.PriceLevel> activityLogPriceLevel = new ActivityLog<>(
                    this.engine.getSupplier(OrderBook.PriceLevel.class),
                    this.properties.getProperty("OrderBookFile"),
                    this.properties.getProperty("OrderBookFileHeader"));
            activityLogPriceLevel.stop();
            Integer count = activityLogPriceLevel.call();
            LGR.info("{} price levels written", count);

        } catch (Exception ex) {
            LGR.error(ToString.stackTrace(ex));
        }
        LGR.info("exiting");
    }

    public static void main(String[] args) {
        try {
            MEService service = new MEService(CommandLineArgs.create(args).getProperties());
            service.init();
            service.start();
            service.close();
            System.exit(0);
        } catch (Throwable throwable) {
            System.err.println(ToString.stackTrace(throwable));
            System.exit(1);
        }
    }
}
