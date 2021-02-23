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

package org.bryan.schorn.tha.matching.order;

import org.bryan.schorn.tha.matching.model.Order;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 *
 * Orders Conduit
 *
 */
public class Orders {

    static final private Helper HELPER = new Helper();

    static public void setFeed(OrderFeed orderFeed) {
        HELPER.set(orderFeed);
    }

    static public Supplier<Order> asSupplier() { return HELPER; }

    static public class Helper implements Supplier<Order> {
        private final Queue<Order> queue = new ConcurrentLinkedQueue<>();
        void set(OrderFeed orderFeed) {
            Order order = orderFeed.get();
            while (order != null) {
                queue.add(order);
                order = orderFeed.get();
            }
        }
        @Override
        public Order get() {
            try {
                Thread.sleep(200);
            } catch (Exception ex) {

            }
            return this.queue.poll();

        }
    }

}
