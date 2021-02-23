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

import org.bryan.schorn.tha.matching.model.Product;
import org.bryan.schorn.tha.matching.product.ProductFeed;
import org.bryan.schorn.tha.matching.product.ProductParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

/**
 * Product Reader (from file)
 */
public class MockProductFeed extends ProductFeed.AbstractProductFeed {

    static private final Logger LGR = LoggerFactory.getLogger(MockProductFeed.class);

    private Properties properties;
    private Queue<Product> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void connect() throws Exception {
        ProductParser parser = new MockProductFeedParser();
        String productFile = this.properties.getProperty("Product.File");
        Path path = Paths.get(productFile);
        Predicate<String> skipHeader = (s) -> !s.startsWith("symbol");
        Predicate<Product> skipNulls = (o) -> o != null;
        Files.lines(path)
                .filter(skipHeader)
                .map(parser)
                .filter(skipNulls)
                .forEachOrdered(o -> queue.add(o));

        for (ProductParser.ProductParseException ppe : parser.getExceptions()) {
            LGR.error(String.format("%s,%s", ppe.inputLine(), ppe.rootException().getMessage()));
        }
    }

    /**
     * Get next Product (Supplier interface)
     *
     * @return
     */
    @Override
    public Product get() {
        return this.queue.poll();
    }

}
