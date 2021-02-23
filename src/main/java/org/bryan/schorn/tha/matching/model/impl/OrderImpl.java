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

package org.bryan.schorn.tha.matching.model.impl;

import org.bryan.schorn.tha.matching.model.*;
import org.bryan.schorn.tha.matching.product.Products;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Order Implementation
 */
public class OrderImpl implements Order {
    private LocalDateTime sendTime;
    private String senderId;
    private String targetId;
    private String clOrdId;
    private Product product;
    private Side side;
    private final OrderType orderType;
    private double price;
    private int orderQty;
    private List<Fill> fills = new ArrayList<>();
    private int filledQty = 0;


    private OrderImpl(BuilderImpl builder) {
        this.sendTime = builder.sendTime;
        this.senderId = builder.senderId;
        this.targetId = builder.targetId;
        this.clOrdId = builder.clOrdId;
        this.product = builder.product;
        this.side = builder.side;
        this.orderType = builder.orderType;
        this.price = builder.price;
        this.orderQty = builder.orderQty;
    }

    public Product product() {
        return this.product;
    }
    public int orderQty() {
        return this.orderQty;
    }
    public int unfilledQty() {
        return this.orderQty - this.filledQty;
    }
    public void fill(Fill fill) {
        this.fills.add(fill);
        this.filledQty += fill.fillQty();
    }
    public Side side() {
        return this.side;
    }
    public double price() {
        return this.price;
    }
    public OrderType orderType() {
        return this.orderType;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner("\n","","");
        joiner.add(String.format("%s,%s,%s,%s,%s,%s,%d,%.2f,%d,%d",
                DateTimeFormatter.ISO_DATE_TIME.format(this.sendTime),
                this.clOrdId,
                this.senderId,
                this.product.symbol(),
                this.orderType.name(),
                this.side.name(),
                this.orderQty,
                this.price,
                this.filledQty,
                this.unfilledQty()));
        if (!this.fills.isEmpty()) {
            for (Fill fill : this.fills) {
                joiner.add(fill.toString());
            }
        }
        return joiner.toString();
    }


    /**
     * Order Builder Implementation
     */
    static public class BuilderImpl implements Order.Builder {
        static final DateTimeFormatter SEND_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.S");
        private LocalDateTime sendTime;
        private String senderId;
        private String targetId;
        private String clOrdId;
        private Product product;
        private Side side;
        private OrderType orderType;
        private double price;
        private int orderQty;
        List<Exception> exceptions = new ArrayList<>();

        public Order.Builder setSendTime(String sendTime) {
            try {
                this.sendTime = LocalDateTime.from(SEND_TIME_FORMAT.parse(sendTime));
            } catch (Exception ex) {
                this.exceptions.add(new Order.FieldException("send_time", sendTime, ex));
            }
            return this;
        }
        public Order.Builder setSenderId(String senderId) {
            this.senderId = senderId;
            return this;
        }
        public Order.Builder setTargetId(String targetId) {
            this.targetId = targetId;
            return this;
        }
        public Order.Builder setClOrdId(String clOrdId) {
            this.clOrdId = clOrdId;
            return this;
        }
        public Order.Builder setSymbol(String symbol) {
            try {
                this.product = Products.find(symbol);
                if (this.product == null) {
                    throw new Exception("Product was not found.");
                }
            } catch (Exception ex) {
                this.exceptions.add(new Order.FieldException("symbol", symbol, ex));
            }
            return this;
        }
        public Order.Builder setSide(String side) {
            try {
                this.side = Side.valueOf(side);
            } catch (Exception ex) {
                this.exceptions.add(new Order.FieldException("side", side, ex));
            }
            return this;
        }
        public Order.Builder setOrderType(String orderType) {
            try {
                this.orderType = OrderType.valueOf(orderType);
            } catch (Exception ex) {
                this.exceptions.add(new Order.FieldException("order_type", orderType, ex));
            }
            return this;
        }
        public Order.Builder setPrice(String price) {
            try {
                this.price = Double.valueOf(price);
            } catch (Exception ex) {
                this.exceptions.add(new Order.FieldException("price", price, ex));
            }
            return this;
        }
        public Order.Builder setOrderQty(String orderQty) {
            try {
                this.orderQty = Integer.valueOf(orderQty);
            } catch (Exception ex) {
                this.exceptions.add(new Order.FieldException("order_qty", orderQty, ex));
            }
            return this;
        }

        public Order build() throws Exception {
            if (this.orderType == OrderType.MARKET) {
                this.price = 0.0;
            }
            if (this.exceptions.isEmpty()) {
                return new OrderImpl(this);
            }
            throw new Exception(String.format("Order build failed with %d exceptions.",
                    this.exceptions.size()));
        }

        public boolean hasExceptions() {
            return !this.exceptions.isEmpty();
        }

        public List<Exception> getExceptions() {
            return this.exceptions;
        }

    }

    static public class RejectedImpl implements Order.Rejected {

        private final Order order;
        private final String reason;

        public RejectedImpl(Order order, String reason) {
            this.order = order;
            this.reason = reason;
        }

        @Override
        public Product product() {
            return this.order.product();
        }

        @Override
        public int orderQty() {
            return this.orderQty();
        }

        @Override
        public int unfilledQty() {
            return this.order.unfilledQty();
        }

        @Override
        public void fill(Fill fill) {
            throw new UnsupportedOperationException("Fills can not be applied to rejected orders");
        }

        @Override
        public Side side() {
            return this.order.side();
        }

        @Override
        public double price() {
            return this.order.price();
        }

        @Override
        public OrderType orderType() {
            return this.order.orderType();
        }

        @Override
        public String reason() {
            return this.reason;
        }

        @Override
        public String toString() {
            return String.format("%s,%s", this.order.toString(), this.reason);
        }
    }

}
