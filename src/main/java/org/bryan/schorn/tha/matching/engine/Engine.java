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
import org.bryan.schorn.tha.matching.order.Orders;
import org.bryan.schorn.tha.matching.product.Products;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.function.Supplier;


/**
 * Matching Engine
 *
 * An instance is dedicated to a collection of Product(s) and keeps an
 * OrderBook instance for each Product's orders.
 */
public class Engine implements Callable<Integer> {
    static private final Logger LGR = LoggerFactory.getLogger(Engine.class);

    // add-in rules interface
    public interface Rule extends Predicate<Order> {
        boolean test(Order order);
        String getReason(Order order);
        String ruleDescription();
    }
    // engine rules
    private final List<Rule> rules = new ArrayList<>();

    // orders supplier
    private Supplier<Order> orderSupplier = null;
    // outbound queue
    private final Queue<Order.Reject> outboundRejectedQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Trade> outboundTradeQueue = new ConcurrentLinkedQueue<>();

    // order book
    private final Map<Product,OrderBook> orderBooks = new HashMap<>();

    // life cycle
    private boolean keepLooping = true;
    private boolean loopingStopped = true;

    // ctor
    public Engine(Collection<Product> productList) {
        for (Product product : productList)
            this.orderBooks.put(product, new OrderBook(product));
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

    // signals the end of cycle
    public void stop() {
        this.keepLooping = false;
    }
    // clear out
    public void recycle() {
        this.outboundRejectedQueue.clear();
        this.outboundTradeQueue.clear();
        for (OrderBook orderBook : this.orderBooks.values()) {
            orderBook.recycle();
        }
        this.keepLooping = true;
    }

    // order event loop
    @Override
    public Integer call() {
        int orderCount = 0;
        Supplier<Order> orderSupplier = Orders.getSupplier();
        Order order = null;
        while (order != null || this.keepLooping) {
            order = orderSupplier.get();
            while (order != null) {
                ++orderCount;
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
                order = orderSupplier.get();
            }
        }
        this.loopingStopped = true;
        return orderCount;
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
                Order.Reject rejectedOrder = Order.reject(order,reason == null ? "unknown" : reason);
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

        int takeQty = takeOrder.orderQty();
        double takePrice = takeOrder.price();

        Product product = Products.find(takeOrder.symbol());

        List<Order> matchedOrders = this.orderBooks.get(product)
                .take(takeOrder.side().otherSide(), takeQty, takePrice);

        if (matchedOrders.isEmpty()) {
            Order.Reject rejectedOrder = Order.reject(takeOrder,"no-match");
            LGR.info("Rejected: {}", rejectedOrder.toString());
            this.outboundRejectedQueue.offer(rejectedOrder);
        } else {
            Instant tradeTime = Instant.now();
            for (Order provideOrder : matchedOrders) {
                Trade trade = Trade.create(product.symbol(),
                        1,
                        provideOrder.price(),
                        tradeTime);
                LGR.info("Trade: {}", trade.toString());
                this.outboundTradeQueue.offer(trade);
            }
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

        int takeQty = takeOrder.orderQty();
        double takePrice = takeOrder.price();
        Product product = Products.find(takeOrder.symbol());

        List<Order> matchedOrders = this.orderBooks.get(product)
                .take(takeOrder.side().otherSide(), takeQty, takePrice);

        if (matchedOrders.isEmpty()) {
            this.orderBooks.get(product).accept(takeOrder);
        } else {
            Instant tradeTime = Instant.now();
            for (Order provideOrder : matchedOrders) {
                Trade trade = Trade.create(product.symbol(),
                        1,
                        provideOrder.price(),
                        tradeTime);
                LGR.info("Trade: {}", trade.toString());
                this.outboundTradeQueue.offer(trade);
            }
        }
    }

    /**
     *
     * @return
     */
    public <E> Supplier<E> getSupplier(Class<E> classOfE) throws Exception {
        if (classOfE.equals(OrderBook.PriceLevel.class)) {
            final Deque<OrderBook.PriceLevel> q = new ArrayDeque<>();
            if (this.loopingStopped) {
                for (OrderBook orderBook : this.orderBooks.values()) {
                    for (OrderBook.PriceLevel priceLevel : orderBook.getPriceLevels()) {
                        q.offer(priceLevel);
                    }
                }
            }
            return () -> classOfE.cast(q.poll());
        } else if (classOfE.equals(Order.Reject.class)) {
            return () -> classOfE.cast(this.outboundRejectedQueue.poll());
        } else if (classOfE.equals(Trade.class)) {
            return () -> classOfE.cast(this.outboundTradeQueue.poll());
        }
        throw new Exception(String.format("There is no supplier for entity: %s",
                classOfE.getSimpleName()));
    }

}
