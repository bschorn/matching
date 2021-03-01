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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Trade Execution (Product, Qty, Price, Timestamp)
 */
public interface Trade {

    Instant timestamp();
    String symbol();
    Double price();
    Integer quantity();

    /**
     * Create Trade
     *
     * @param product
     * @param quantity
     * @param tradePrice
     * @param timestamp
     * @return
     */
    static Trade create(String symbol, Integer quantity, Double tradePrice, Instant timestamp) {
        return new Impl(symbol, quantity, tradePrice, timestamp);
    }


    /**
     * Trade Implementation
     */
    class Impl implements Trade {

        private final String symbol;
        private final Integer quantity;
        private final Double tradePrice;
        private final Instant timestamp;

        private Impl(String symbol, Integer quantity, Double tradePrice, Instant timestamp) {
            this.symbol = symbol;
            this.quantity = quantity;
            this.tradePrice = tradePrice;
            this.timestamp = timestamp;
        }

        public String symbol() { return this.symbol; }

        public Integer quantity() {
            return this.quantity;
        }

        public Double price() {
            return this.tradePrice;
        }

        public Instant timestamp() {
            return this.timestamp;
        }


        @Override
        public String toString() {
            return String.format("%s,%.2f,%d.%d",
                    this.symbol,
                    this.tradePrice,
                    this.timestamp.getEpochSecond(),
                    this.timestamp.getNano());
        }
    }
}
