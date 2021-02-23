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
import org.bryan.schorn.tha.matching.order.OrderFeed;
import org.bryan.schorn.tha.matching.order.OrderParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

/**
 * Order Reader (from file)
 */
public class MockOrderFeed extends OrderFeed.AbstractOrderFeed {

    static private final Logger LGR = LoggerFactory.getLogger(MockOrderFeed.class);

    private Properties properties;
    private Queue<Order> queue = new ConcurrentLinkedQueue<>();
    private List<Exception> exceptions = new ArrayList<>();

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void connect() throws Exception {
        MockOrderFeedParser parser = new MockOrderFeedParser();
        String orderFile = this.properties.getProperty("Order.File");
        String rejectedFile = this.properties.getProperty("Rejected.File");
        Path path = Paths.get(orderFile);
        Predicate<String> skipHeader = (s) -> !s.startsWith("send_time");
        Predicate<Order> skipNulls = (o) -> o != null;
        Files.lines(path)
                .filter(skipHeader)
                .map(parser)
                .filter(skipNulls)
                .forEachOrdered(o -> queue.add(o));

        if (parser.hasExceptions()) {
            Path rejectedPath = Paths.get(rejectedFile);
            try (BufferedWriter writer = Files.newBufferedWriter(rejectedPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
                for (OrderParser.OrderParseException ope : parser.getExceptions()) {
                    writer.write(String.format("%s,%s", ope.inputLine(), ope.rootException().getMessage()));
                }
            }
        }
    }

    /**
     * Get next Order (Supplier interface)
     *
     * @return
     */
    @Override
    public Order get() {
        return this.queue.poll();
    }

}
