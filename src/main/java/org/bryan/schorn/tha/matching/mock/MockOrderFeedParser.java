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

package org.bryan.schorn.tha.matching.mock;

import org.bryan.schorn.tha.matching.model.Order;
import org.bryan.schorn.tha.matching.order.OrderParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Order Parser Implementation for a line in CSV format
 */
public class MockOrderFeedParser implements OrderParser {
    static private final Logger LGR = LoggerFactory.getLogger(MockOrderFeedParser.class);

    static public final String HEADER = "send_time,sender_id,target_id,cl_ord_id,symbol,side,order_type,price,order_qty";
    static final Map<String,Integer> FIELD_MAP = new HashMap<>();
    static {
        String[] fields = HEADER.split(",");
        for (int i = 0; i < fields.length; i++) {
            FIELD_MAP.put(fields[i],i);
        }
    }

    private List<OrderParseException> exceptions = new ArrayList<>();

    /**
     * Parser and convert a String in CSV format representing a single order.
     *
     * @param line
     * @return
     */
    @Override
    public Order apply(String line) {
        try {
            String[] values = line.split(",");
            if (values.length != FIELD_MAP.size()) {
                throw new Exception(String.format("Found %d fields was expecting %d fields", values.length, FIELD_MAP.size()));
            }
            Order.Builder builder = Order.builder();
            builder.setSendTime(values[FIELD_MAP.get("send_time")]);
            builder.setSenderId(values[FIELD_MAP.get("sender_id")]);
            builder.setTargetId(values[FIELD_MAP.get("target_id")]);
            builder.setClOrdId(values[FIELD_MAP.get("cl_ord_id")]);
            builder.setSymbol(values[FIELD_MAP.get("symbol")]);
            builder.setSide(values[FIELD_MAP.get("side")]);
            builder.setOrderType(values[FIELD_MAP.get("order_type")]);
            builder.setPrice(values[FIELD_MAP.get("price")]);
            builder.setOrderQty(values[FIELD_MAP.get("order_qty")]);
            return builder.build();
        } catch (Exception ex) {
            this.exceptions.add(new OrderParseException(ex, line));
        }
        return null;
    }

    /**
     * Were there any exceptions?
     *
     * @return
     */
    public boolean hasExceptions() {
        return !this.exceptions.isEmpty();
    }

    /**
     * List of all the exceptions (one for each line that had exception).
     * @return
     */
    public List<OrderParseException> getExceptions() {
        return this.exceptions;
    }

}
