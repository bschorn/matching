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

package org.bryan.schorn.tha.matching.product;

import org.bryan.schorn.tha.matching.model.Product;
import org.bryan.schorn.tha.matching.product.ProductFeed;

import java.util.*;
import java.util.stream.Collectors;

/**
 *  Products Container
 */
public class Products {

    static final private Helper HELPER = new Helper();

    static public void setFeed(ProductFeed productFeed) {
        HELPER.set(productFeed);
    }

    static public boolean isHalted(String symbol) {
        Product product = HELPER.products.get(symbol);
        return product != null ? product.isHalted() : true;
    }
    static public Product find(String symbol) {
        return HELPER.products.get(symbol);
    }

    static public List<Product> findAll() {
        return Collections.unmodifiableList(HELPER.products.values().stream().collect(Collectors.toList()));
    }

    static public class Helper {
        final private Map<String, Product> products = new HashMap<>();
        void set(ProductFeed productFeed) {
            Product product = productFeed.get();
            while (product != null) {
                products.put(product.symbol(), product);
                product = productFeed.get();
            }
        }
    }
}
