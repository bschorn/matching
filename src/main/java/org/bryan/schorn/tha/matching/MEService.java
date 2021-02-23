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

import org.bryan.schorn.tha.matching.engine.ActivityLogs;
import org.bryan.schorn.tha.matching.engine.Engine;
import org.bryan.schorn.tha.matching.engine.OrderThrottleRule;
import org.bryan.schorn.tha.matching.engine.ProductHalted;
import org.bryan.schorn.tha.matching.model.Order;
import org.bryan.schorn.tha.matching.order.OrderFeed;
import org.bryan.schorn.tha.matching.product.ProductFeed;
import org.bryan.schorn.tha.matching.order.Orders;
import org.bryan.schorn.tha.matching.product.Products;
import org.bryan.schorn.tha.matching.util.CommandLineArgs;
import org.bryan.schorn.tha.matching.util.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;


/**
 * Entry Point
 */
public class MEService implements Runnable {

    static private final Logger LGR = LoggerFactory.getLogger(MEService.class);

    private Properties properties;

    enum State {
        INIT,
        START,
        RUNNING,
        STOP;
    }
    private State state = State.INIT;
    private OrderFeed orderFeed = null;
    private Engine engine = null;

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
        // get product feed created/connected/attached
        ProductFeed productFeed = ProductFeed.create(properties);
        productFeed.connect();
        Products.setFeed(productFeed);

        // get order feed created/connected/attached
        this.orderFeed = OrderFeed.create(properties);
        this.orderFeed.connect();
        Orders.setFeed(this.orderFeed);

        // create single engine with all products
        this.engine = new Engine(Products.findAll());

        // add rule for trade halts
        this.engine.addRule(ProductHalted.PRODUCTED_HALTED);
        // add rule for the 3 orders in one second
        this.engine.addRule(OrderThrottleRule.MAX_THREE_PER_SECOND);

        // update state
        this.state = State.START;
    }

    /**
     * Service Thread
     *
     */
    @Override
    public void run() {
        this.state = State.RUNNING;
        this.engine.startLoop();
        while (this.state == State.RUNNING) {
            Order order = Orders.asSupplier().get();
            if (order != null) {
                this.engine.accept(order);
            } else {
                this.engine.stopLoop();
                this.stop();
            }
        }
    }

    /**
     * Start Service
     *
     * @throws Exception
     */
    public void start() throws Exception {
        if (this.state != State.START) {
            LGR.error("Invalid State for calling start()");
            return;
        }
        this.run();
    }

    /**
     * Stop Service
     */
    public void stop() {
        if (this.state != State.RUNNING) {
            LGR.error("Invalid State for calling stop()");
            return;
        }
        this.state = State.STOP;
    }

    public void close() {
        try {
            ActivityLogs.logTrades(this.engine.trades(),
                    this.properties.getProperty("Trade.File"),
                    this.properties.getProperty("Trade.File.Header"));
        } catch (Exception ex) {
            LGR.error(ToString.stackTrace(ex));
        }
        try {
            ActivityLogs.logRejects(this.engine.rejected(),
                    this.properties.getProperty("Rejected.File"),
                    this.properties.getProperty("Rejected.File.Header"));
        } catch (Exception ex) {
            LGR.error(ToString.stackTrace(ex));
        }
        try {
            ActivityLogs.logOrderBook(this.engine.getOrderBooks(),
                    this.properties.getProperty("OrderBook.File"),
                    this.properties.getProperty("OrderBook.File.Header"));
        } catch (Exception ex) {
            LGR.error(ToString.stackTrace(ex));
        }
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
