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

package org.bryan.schorn.tha.matching.engine;

import org.bryan.schorn.tha.matching.model.Order;
import org.bryan.schorn.tha.matching.model.Product;
import org.bryan.schorn.tha.matching.model.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * Matching Engine
 *
 * An instance is dedicated to a collection of Product(s) and keeps an
 * OrderBook instance for each Product's orders.
 */
public class Engine implements Consumer<Order>, Callable<List<OrderBook>> {
    static private final Logger LGR = LoggerFactory.getLogger(Engine.class);

    // add-in rules interface
    public interface Rule extends Predicate<Order> {
        boolean test(Order order);
        String getReason(Order order);
        String ruleDescription();
    }
    // engine rules
    private final List<Rule> rules = new ArrayList<>();

    // inbound queue
    private final Queue<Order> inboundOrderQueue = new ConcurrentLinkedQueue<>();
    // outbound queue
    private final Queue<Order.Rejected> outboundRejectedQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Trade> outboundTradeQueue = new ConcurrentLinkedQueue<>();

    // order book
    private final Map<Product,OrderBook> orderBookMap = new HashMap<>();

    // life cycle
    private boolean keepLooping = true;
    private ExecutorService executorService = null;
    private Future<List<OrderBook>> future = null;

    // ctor
    public Engine(Collection<Product> products) {
        for (Product product : products) {
            this.orderBookMap.put(product, new OrderBook(product));
        }
        executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Add Engine Rule
     *
     * @param rule
     * @return
     */
    public boolean addRule(Rule rule) {
        boolean added = this.rules.add(rule);
        if (added) LGR.info("Rule added: "+rule.ruleDescription());
        return added;
    }

    /**
     * Remove Engine Rule
     *
     * @param rule
     * @return
     */
    public boolean removeRule(Rule rule) {
        boolean removed = this.rules.remove(rule);
        if (removed) LGR.info("Rule removed: "+rule.ruleDescription());
        return removed;
    }

    // incoming order place on inbound order queue
    public void accept(Order order) {
        this.inboundOrderQueue.offer(order);
    }
    // submits this instance to the ExecutorService
    public void startLoop() {
        future = executorService.submit(this);
    }
    // signals the end of cycle
    public void stopLoop() {
        this.keepLooping = false;
        executorService.shutdown();
    }
    // clear out
    public void recycle() {
        this.outboundRejectedQueue.clear();
        this.inboundOrderQueue.clear();
        this.outboundTradeQueue.clear();
        for (OrderBook orderBook : this.orderBookMap.values()) {
            orderBook.recycle();
        }
    }
    // retrieves the last state the order books
    public List<OrderBook> getOrderBooks() throws Exception {
        if (this.keepLooping) {
            throw new Exception("Access to OrderBooks can only occur after engine has stopped.");
        }
        return this.future.get();
    }

    // order event loop
    @Override
    public List<OrderBook> call() throws Exception {
        while (this.keepLooping) {
            Order order = this.inboundOrderQueue.poll();
            while (order != null) {
                if (passedRules(order)) {
                    switch (order.orderType()) {
                        case MARKET:
                            market(order);
                            break;
                        case LIMIT:
                            limit(order);
                            break;
                    }
                }
                order = this.inboundOrderQueue.poll();
            }
        }
        return this.orderBookMap.values().stream().collect(Collectors.toList());
    }

    /**
     * Pre-Trade Check all incoming Order(s) must run through all Engine.Rule(s)
     * where they may be rejected and sent to the rejected queue.
     *
     * @param order
     * @return
     */
    private boolean passedRules(Order order) {
        for (Rule rule : this.rules) {
            if (!rule.test(order)) {
                String reason = rule.getReason(order);
                Order.Rejected rejectedOrder = Order.reject(order,reason == null ? "unknown" : reason);
                LGR.info("Rejected: {}", rejectedOrder.toString());
                this.outboundRejectedQueue.offer(rejectedOrder);
                return false;
            }
        }
        return true;
    }

    /**
     * Market Order Execution
     *
     * Take the quantity from the other side of the OrderBook.
     *
     *
     * @param takeOrder
     */
    private void market(Order takeOrder) {
        LGR.info("Order: {}", takeOrder.toString());

        int takeQty = takeOrder.unfilledQty();

        List<Order> matchedOrders = this.orderBookMap.get(takeOrder.product())
                .take(takeOrder.side().otherSide(), takeQty, 0.0);

        LocalDateTime tradeTime = LocalDateTime.now();

        for (Order provideOrder : matchedOrders) {
            Trade trade = new Trade(takeOrder.product(),
                    Math.min(provideOrder.unfilledQty(),takeQty),
                    provideOrder.price(),
                    tradeTime,
                    takeOrder,
                    provideOrder);
            LGR.info("Trade: {}", trade.toString());
            takeOrder.fill(trade.takeFill());
            provideOrder.fill(trade.provideFill());
            this.outboundTradeQueue.offer(trade);
        }

        if (takeOrder.unfilledQty() > 0) {
            Order.Rejected rejectedOrder = Order.reject(takeOrder,"no-match");
            LGR.info("Rejected: {}", rejectedOrder.toString());
            this.outboundRejectedQueue.offer(rejectedOrder);
        }
    }

    /**
     * Limit Order Execution
     *
     * Take the quantity from the other side of the OrderBook but only if
     * the price is equal or better than the limit price.
     *
     * @param takeOrder
     */
    private void limit(Order takeOrder) {
        LGR.info("Order: {}", takeOrder.toString());

        int takeQty = takeOrder.unfilledQty();
        double takePrice = takeOrder.price();

        List<Order> matchedOrders = this.orderBookMap.get(takeOrder.product())
                .take(takeOrder.side().otherSide(), takeQty, takePrice);

        LocalDateTime tradeTime = LocalDateTime.now();

        for (Order provideOrder : matchedOrders) {
            Trade trade = new Trade(takeOrder.product(),
                    Math.min(provideOrder.unfilledQty(),takeQty),
                    provideOrder.price(),
                    tradeTime,
                    takeOrder,
                    provideOrder);
            LGR.info("Trade: {}", trade.toString());
            takeOrder.fill(trade.takeFill());
            provideOrder.fill(trade.provideFill());
            this.outboundTradeQueue.offer(trade);
        }
        if (takeOrder.unfilledQty() > 0) {
            this.orderBookMap.get(takeOrder.product()).accept(takeOrder);
        }
    }

    private final Supplier<Trade> tradeSupplier = () -> this.outboundTradeQueue.poll();
    /**
     * Trade Supplier
     *
     * @return
     */
    public Supplier<Trade> trades() {
        return this.tradeSupplier;
    }

    private final Supplier<Order.Rejected> rejectSupplier = () -> this.outboundRejectedQueue.poll();
    /**
     * Rejected Order Supplier
     *
     * @return
     */
    public Supplier<Order.Rejected> rejected() {
        return this.rejectSupplier;
    }


}
