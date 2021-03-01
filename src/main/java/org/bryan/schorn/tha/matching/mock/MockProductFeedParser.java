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
import org.bryan.schorn.tha.matching.product.ProductParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Product Parser Implementation for a line in CSV format
 */
public class MockProductFeedParser implements ProductParser {
    static private final Logger LGR = LoggerFactory.getLogger(MockProductFeedParser.class);

    static final String HEADER = "symbol,is_halted";
    static final Map<String,Integer> FIELD_MAP = new HashMap<>();
    static {
        String[] fields = HEADER.split(",");
        for (int i = 0; i < fields.length; i++) {
            FIELD_MAP.put(fields[i],i);
        }
    }

    private List<ProductParseException> exceptions = new ArrayList<>();

    /**
     * Parser and convert a String in CSV format representing a single Product.
     *
     * @param line
     * @return
     */
    @Override
    public Product apply(String line) {
        try {
            String[] values = line.split(",");
            if (values.length != 2 && values.length != FIELD_MAP.size()) {
                throw new Exception(String.format("Found %d fields was expecting %d fields", values.length, FIELD_MAP.size()));
            }
            Product product = Product.create(values[FIELD_MAP.get("symbol")]);
            product.setHalted(Boolean.valueOf(values[FIELD_MAP.get("is_halted")]));
            return product;
        } catch (Exception ex) {
            this.exceptions.add(new ProductParseException(ex, line));
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
    public List<ProductParseException> getExceptions() {
        return this.exceptions;
    }

}
