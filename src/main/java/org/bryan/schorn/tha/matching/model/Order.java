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

import org.bryan.schorn.tha.matching.model.impl.OrderImpl;
import org.bryan.schorn.tha.matching.product.Products;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;


/**
 *
 */
public interface Order {

    Product product();
    int orderQty();
    int unfilledQty();
    void fill(Fill fill);
    Side side();
    double price();
    OrderType orderType();


    /**
     * Field Exception during build
     */
    static public class FieldException extends Exception {
        Exception exception;
        String fieldName;
        String fieldValue;

        public FieldException(String fieldName, String fieldValue, Exception exception) {
            super(exception);
            this.fieldName = fieldName;
            this.fieldValue = fieldValue;
            this.exception = exception;
        }

        @Override
        public String getMessage() {
            return String.format("%s: %s caught exception: %s", fieldName, fieldValue, exception.getMessage());
        }
    }


    /**
     * Order Builder Interface
     */
    interface Builder {
        Builder setSendTime(String sendTime);
        Builder setSendTime(LocalDateTime sendTime);
        Builder setSenderId(String senderId);
        Builder setTargetId(String targetId);
        Builder setClOrdId(String clOrdId);
        Builder setSymbol(String symbol);
        Builder setProduct(Product product);
        Builder setSide(String side);
        Builder setSide(Side side);
        Builder setOrderType(String orderType);
        Builder setOrderType(OrderType orderType);
        Builder setPrice(String price);
        Builder setPrice(double price);
        Builder setOrderQty(String orderQty);
        Builder setOrderQty(int orderQty);

        Order build() throws Exception;

        boolean hasExceptions();
        List<Exception> getExceptions();
    }

    /**
     * Creates an Order.Builder for creating Order instances
     * @return
     */
    static Order.Builder builder() {
        return new OrderImpl.BuilderImpl();
    }

    /**
     * Rejected Order (original order wrapped with reason included)
     */
    interface Rejected extends Order {
        String reason();
        Order order();
    }

    /**
     * Creates an Order.Rejected by containing an Order that has been
     * rejected along with a reason.
     *
     * @param order
     * @param reason
     * @return
     */
    static Order.Rejected reject(Order order, String reason) {
        return new OrderImpl.RejectedImpl(order, reason);
    }
}
