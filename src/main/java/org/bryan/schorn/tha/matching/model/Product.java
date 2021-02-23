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

import java.util.ArrayList;
import java.util.List;

/**
 * Product
 */
public class Product {

    private final String symbol;
    private boolean isHalted;

    private Product(Builder builder) {
        this.symbol = builder.symbol;
        this.isHalted = builder.isHalted;
    }

    /**
     * Product's exchange recognized symbol
     *
     * @return
     */
    public String symbol() {
        return this.symbol;
    }

    /**
     * Product's current trading status.
     *
     * @return
     */
    public boolean isHalted() {
        return this.isHalted;
    }


    /**
     * Product Builder
     */
    static public class Builder {
        String symbol;
        boolean isHalted;
        List<Exception> exceptions = new ArrayList<>();

        public Builder setSymbol(String symbol) {
            this.symbol = symbol;
            return this;
        }
        public Builder setHalted(String isHalted) {
            this.isHalted = Boolean.valueOf(isHalted);
            return this;
        }
        public Product build() throws Exception {
            if (this.exceptions.isEmpty()) {
                return new Product(this);
            }
            throw new Exception(String.format("Order build failed with %d exceptions.",
                    this.exceptions.size()));
        }
    }
}
