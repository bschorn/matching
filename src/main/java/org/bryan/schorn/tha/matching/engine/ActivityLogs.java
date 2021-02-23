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
import org.bryan.schorn.tha.matching.model.Trade;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Supplier;

/**
 *  Activity Logs
 */
public interface ActivityLogs {

    static void logTrades(Supplier<Trade> trades, String filename, String header) throws Exception {
        Path tradeFilePath = Paths.get(filename);
        try (BufferedWriter writer = Files.newBufferedWriter(tradeFilePath, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
            writer.write(header + System.lineSeparator());
            Trade trade = trades.get();
            while (trade != null) {
                writer.write(trade.toString());
                writer.write(System.lineSeparator());
                trade = trades.get();
            }
        }
    }

    static void logRejects(Supplier<Order.Rejected> rejects, String filename, String header) throws Exception {
        Path rejectFilePath = Paths.get(filename);
        try (BufferedWriter writer = Files.newBufferedWriter(rejectFilePath, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
            writer.write(header + System.lineSeparator());
            Order.Rejected rejected = rejects.get();
            while (rejected != null) {
                writer.write(rejected.toString());
                writer.write(System.lineSeparator());
                rejected = rejects.get();
            }
        }

    }

    static void logOrderBook(List<OrderBook> orderBookList, String filename, String header) throws Exception {
        Path orderBookFilePath = Paths.get(filename);
        try (BufferedWriter writer = Files.newBufferedWriter(orderBookFilePath, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
            writer.write(header + System.lineSeparator());
            for (OrderBook orderBook : orderBookList) {
                String orderBookContents = orderBook.toString();
                writer.write(orderBookContents);
            }
        }
    }

}
