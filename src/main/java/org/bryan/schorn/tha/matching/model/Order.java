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

package org.bryan.schorn.tha.matching.model;

import java.time.Instant;
import java.util.StringJoiner;


/**
 *
 */
public interface Order {
    Instant timestamp();
    String symbol();
    OrderType orderType();
    Side side();
    Double price();
    Integer orderQty();


    static Order create(Instant timestamp,
                        String symbol,
                        Side side,
                        OrderType orderType,
                        Double price,
                        Integer orderQty) {
        return new Impl(timestamp, symbol, side, orderType, price, orderQty);
    }

    class Impl implements Order {
        private final Instant timestamp;
        private final String symbol;
        private final Side side;
        private final OrderType orderType;
        private final Double price;
        private final Integer orderQty;

        private Impl(Instant timestamp,
                         String symbol,
                         Side side,
                         OrderType orderType,
                         Double price,
                         Integer orderQty) {
            this.timestamp = timestamp;
            this.symbol = symbol;
            this.side = side;
            this.orderType = orderType;
            this.price = price;
            this.orderQty = orderQty;
        }

        public Instant timestamp() { return this.timestamp; }
        public String symbol() {
            return this.symbol;
        }
        public Integer orderQty() {
            return this.orderQty;
        }
        public Side side() {
            return this.side;
        }
        public Double price() {
            return this.price;
        }
        public OrderType orderType() {
            return this.orderType;
        }

        @Override
        public String toString() {
            StringJoiner joiner = new StringJoiner("\n","","");
            //symbol,side,type,price,timestamp
            joiner.add(String.format("%s,%s,%s,%.2f,%d.%d",
                    this.symbol,
                    this.side.name(),
                    this.orderType.name(),
                    this.price,
                    this.timestamp.getEpochSecond(),
                    this.timestamp.getNano()));
            return joiner.toString();
        }

    }

    static Reject reject(Order order, String reason) {
        return new Reject(order, reason);
    }
    /**
     * Reject Order
     */
    class Reject {

        private final Order order;
        private final String reason;

        private Reject(Order order, String reason) {
            this.order = order;
            this.reason = reason;
        }
        public Order order() { return this.order; }

        public String reason() { return this.reason; }

        @Override
        public String toString() {
            return String.format("%s,%s", this.order.toString(), this.reason());
        }
    }

}

