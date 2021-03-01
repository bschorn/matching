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
import org.bryan.schorn.tha.matching.model.OrderType;
import org.bryan.schorn.tha.matching.model.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Order Parser Implementation for a line in CSV format
 */
public class MockOrderFeedParser implements MockOrderFeed.MockOrderParser {
    static private final Logger LGR = LoggerFactory.getLogger(MockOrderFeedParser.class);

    private String header = null;
    private Map<String,Integer> fieldMap;

    public void setHeader(String header) {
        this.header = header;
        this.fieldMap = new HashMap<>();
        String[] fields = this.header.split(",");
        for (int i = 0; i < fields.length; i++) {
            fieldMap.put(fields[i],i);
        }
    }
    /**
     * Parser and convert a String in CSV format representing a single order.
     *
     * @param line
     * @return
     */
    @Override
    public Order apply(String line) {
        String[] values = line.split(",");
        Instant timestamp = null;
        try {
            timestamp = parseEpochNanoTimestamp.apply(values[fieldMap.get("timestamp")]);
        } catch (DateTimeParseException ex) {
            LGR.error("Failed to parse timestamp.");
        }
        return Order.create(timestamp,
            values[fieldMap.get("symbol")],
            Side.parse(values[fieldMap.get("side")]),
            OrderType.parse(values[fieldMap.get("type")]),
            values[fieldMap.get("price")].length() > 0
                ? Double.valueOf(values[fieldMap.get("price")])
                : 0.0,
            1);
    }

    static final Function<String, Instant> parseEpochNanoTimestamp = (timeStr) -> {
        String[] timeParts = timeStr.split("\\.");
        long epochSeconds = Long.valueOf(timeParts[0]).longValue();
        //int nanoSeconds = Double.valueOf(1000000000.0 * Double.valueOf("0."+timeParts[1]).doubleValue()).intValue();
        int nanoSeconds = Integer.valueOf(timeParts[1]).intValue();
        return Instant.ofEpochSecond(epochSeconds, nanoSeconds);
    };
}
