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
import org.bryan.schorn.tha.matching.util.ClassLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * Mock Order Feed (from file)
 */
public class MockOrderFeed extends OrderFeed.AbstractOrderFeed {

    static private final Logger LGR = LoggerFactory.getLogger(MockOrderFeed.class);

    /**
     * Mock Order Parser Interface
     */
    interface MockOrderParser extends OrderFeed.Parser<String> {
        @Override
        Order apply(String line);
    }

    /**
     * Members
     */
    private Properties properties;
    private Queue<Order> queue = new ConcurrentLinkedQueue<>();
    private List<Exception> exceptions = new ArrayList<>();
    private MockOrderParser parser = null;
    private Path orderFilePath = null;

    /**
     * Config
     * @param properties
     * @throws Exception
     */
    @Override
    public void setProperties(Properties properties) throws Exception {
        this.properties = properties;
        ClassLocator classLocator = ClassLocator.create(properties);
        this.parser = (MockOrderParser) classLocator.newInstance(MockOrderParser.class.getSimpleName());
        this.parser.setHeader(properties.getProperty("OrderFileHeader"));
    }

    /**
     * Connect
     *
     *
     *
     * @throws Exception
     */
    @Override
    public void connect() throws Exception {
        String orderFile = this.properties.getProperty("OrderFile");
        if (orderFile == null) {
            throw new Exception(String.format("There was no file order file specified.\n"
                    +"Please specify the order file in application.properties:\n"
                    +"Order.File=<filepath>\n"));
        }
        this.orderFilePath = Paths.get(orderFile);
        if (!Files.exists(this.orderFilePath)) {
            throw new Exception(String.format("%s file not found: %s",
                    MockOrderFeed.class.getSimpleName(),
                    orderFile));
        }
    }

    @Override
    public Integer call() throws Exception {
        final AtomicInteger records = new AtomicInteger(0);
        Predicate<String> skipHeader = (s) -> !s.startsWith("symbol");
        Predicate<Order> skipNulls = (o) -> o != null;
        Files.lines(this.orderFilePath)
                .filter(skipHeader)
                .map(this.parser)
                .filter(skipNulls)
                .forEachOrdered(o -> {
                    records.incrementAndGet();
                    queue.add(o);
                });
        // return the count of records read
        return records.get();
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
