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
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bryan.schorn.tha.matching.TestProperties;
import org.bryan.schorn.tha.matching.engine.rule.CheckRequiredFields;
import org.bryan.schorn.tha.matching.engine.rule.OrderThrottleRule;
import org.bryan.schorn.tha.matching.engine.rule.ProductHalted;
import org.bryan.schorn.tha.matching.model.Order;
import org.bryan.schorn.tha.matching.model.Product;
import org.bryan.schorn.tha.matching.model.Trade;
import org.bryan.schorn.tha.matching.order.OrderFeed;
import org.bryan.schorn.tha.matching.order.Orders;
import org.bryan.schorn.tha.matching.product.ProductFeed;
import org.bryan.schorn.tha.matching.product.Products;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

public class EngineTest {

    /**
     * Test A: Sell Limit + Buy Market = Trade
     */
    @Test
    void sendTestOrdersA() {
        try {
            prepareOrders("inputs-test/orders-a.csv");
        } catch (Exception ex) {
            fail("Failed when testing orders 'a' ", ex);
        }
        try {
            runEngine();
            List<Trade> trades = getTrades();
            assertEquals(1, trades.size());
            assertEquals(3171.87, trades.get(0).price());
        } catch (Exception ex) {
            fail("", ex);
        }
    }

    /**
     * Test B: 3 Buy Limits in less than one second
     */
    @Test
    void sendTestOrdersB() {
        try {
            prepareOrders("inputs-test/orders-b.csv");
        } catch (Exception ex) {
            fail("Failed when preparing orders 'b' ", ex);
        }
        try {
            runEngine();
            List<Trade> trades = getTrades();
            assertEquals(0, trades.size());
            List<Order.Reject> rejects = getRejects();
            assertEquals(1, rejects.size());
            assertEquals("TSLA", rejects.get(0).order().symbol());
            assertEquals(659.7, rejects.get(0).order().price());
        } catch (Exception ex) {
            fail("", ex);
        }
    }

    /**
     * Test C: 1 halted symbol
     */
    @Test
    void sendTestOrdersC() {
        try {
            prepareOrders("inputs-test/orders-c.csv");
        } catch (Exception ex) {
            fail("Failed when testing orders 'c' ", ex);
        }
        try {
            runEngine();
            List<Trade> trades = getTrades();
            assertEquals(0, trades.size());
            List<Order.Reject> rejects = getRejects();
            assertEquals(1, rejects.size());
            assertEquals("GOOG", rejects.get(0).order().symbol());
            assertEquals("product-halted", rejects.get(0).reason());
        } catch (Exception ex) {
            fail("", ex);
        }
    }

    /**
     * Test D: 1 halted symbol
     */
    @Test
    void sendTestOrdersD() {
        try {
            prepareOrders("inputs-test/orders-d.csv");
        } catch (Exception ex) {
            fail("Failed when testing orders 'd' ", ex);
        }
        try {
            runEngine();
            List<Trade> trades = getTrades();
            assertEquals(0, trades.size());
            List<Order.Reject> rejects = getRejects();
            assertEquals(1, rejects.size());
            assertEquals("AMZN", rejects.get(0).order().symbol());
            assertEquals("missing-required-field", rejects.get(0).reason());
        } catch (Exception ex) {
            fail("", ex);
        }
    }

    Engine engine;
    Properties properties;

    EngineTest() {
        this.properties = TestProperties.getProperties();
        try {
            // get product feed created/connected/attached
            ProductFeed productFeed = ProductFeed.create(this.properties);
            productFeed.connect();
            Products.setFeed(productFeed);
        } catch (Exception ex) {
            fail("Unable to load products", ex);
        }
        List<Product> products = Products.findAll();
        assertEquals(5, products.size());
        this.engine = new Engine(Products.findAll());
        this.engine.addRule(OrderThrottleRule.MAX_THREE_PER_SECOND);
        this.engine.addRule(CheckRequiredFields.CHECK_REQUIRED_FIELDS);
        this.engine.addRule(ProductHalted.PRODUCTED_HALTED);

    }

    void prepareOrders(String orderFileName) throws Exception {
        this.properties.setProperty("OrderFile", orderFileName);
        OrderFeed orderFeed = OrderFeed.create(this.properties);
        orderFeed.connect();
        Orders.setFeed(orderFeed);
        orderFeed.call();
    }
    void runEngine() throws Exception {
        this.engine.recycle();
        Thread thread = new Thread(() -> this.engine.call());
        thread.start();
        Thread.sleep(100);
        this.engine.stop();
        thread.join();
    }
    List<Trade> getTrades() throws Exception {
        Supplier<Trade> ts = this.engine.getSupplier(Trade.class);
        List<Trade> trades = new ArrayList<>();
        for (Trade trade = ts.get(); trade != null; trade = ts.get()) {
            trades.add(trade);
        }
        return trades;
    }
    List<Order.Reject> getRejects() throws Exception {
        Supplier<Order.Reject> ors = this.engine.getSupplier(Order.Reject.class);
        List<Order.Reject> rejects = new ArrayList<>();
        for (Order.Reject reject = ors.get(); reject != null; reject = ors.get()) {
            rejects.add(reject);
        }
        return rejects;
    }

}
