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

/**
 * Product
 */
public interface Product {

    String symbol();
    Boolean isHalted();
    void setHalted(Boolean halted);

    /**
     *
     * @param symbol
     * @return
     */
    static Product create(String symbol) {
        return new Impl(symbol);
    }

    /**
     * Product Implementation
     */
    class Impl implements Product {
        private final String symbol;
        private Boolean isHalted;

        private Impl(String symbol) {
            this.symbol = symbol;
            this.isHalted = true;
        }

        public String symbol() {
            return this.symbol;
        }

        public Boolean isHalted() {
            return this.isHalted;
        }

        public void setHalted(Boolean halted)  {
            this.isHalted = halted;
        }

        @Override
        public String toString() {
            return String.format("%s,%s",this.symbol,this.isHalted.toString());
        }
    }
}
