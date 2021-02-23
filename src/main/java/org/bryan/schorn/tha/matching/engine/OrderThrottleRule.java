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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;


/**
 *  An Engine Rule with the intent of limiting (by symbol) a specified number of orders
 *  allowed within a specified duration of time (in milliseconds).
 *
 */
public class OrderThrottleRule implements Engine.Rule {

    /**
     * Pre-defined Engine.Rule for limiting 3 orders per 1 second window.
     */
    static public OrderThrottleRule MAX_THREE_PER_SECOND = new OrderThrottleRule(3,1000);


    // how many orders?
    int frequency;
    // for how long?
    long durationMS;
    // what is the descriptive reason be for rule returning false?
    String reason;
    public OrderThrottleRule(int frequency, long durationMS) {
        this.frequency = frequency;
        this.durationMS = durationMS;
        this.reason = String.format("order throttled: %d per %d ms window", this.frequency, this.durationMS);
    }

    /**
     * Last N orders where N == OrderThrottleRule.frequency of parent instance.
     * tracks the timestamp of the last N orders. Once N orders have been collected,
     * new orders are checked if they occurred within the the time limit.
     */
    class LastN {
        TreeSet<Long> orderTimes = new TreeSet<>();
        boolean check(Long orderTime) {
            if (orderTimes.size() == OrderThrottleRule.this.frequency) {
                if (orderTime - orderTimes.first() < OrderThrottleRule.this.durationMS) {
                    return false;
                } else {
                    orderTimes.remove(orderTimes.first());
                }
            }
            orderTimes.add(orderTime);
            return true;
        }
    }

    // collection map by product
    private Map<Product,LastN> monitor = new HashMap<>();

    @Override
    public boolean test(Order order) {
        LastN lastN = monitor.get(order.product());
        if (lastN == null) {
            lastN = new LastN();
            monitor.put(order.product(),lastN);
        }
        return lastN.check(System.currentTimeMillis());
    }

    @Override
    public String getReason(Order order) {
        return this.reason;
    }
}
