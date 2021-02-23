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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Trade Execution (Product, Qty, Price, Timestamp)
 */
public class Trade {

    static public DateTimeFormatter TRADE_TIME_FORMAT = DateTimeFormatter.ISO_DATE_TIME;

    private final Product product;
    private final int quantity;
    private final double tradePrice;
    private final LocalDateTime timestamp;
    private final Fill takeFill;
    private final Fill provideFill;

    public Trade(Product product, int quantity, double tradePrice, LocalDateTime timestamp, Order takeOrder, Order provideOrder) {
        this.product = product;
        this.quantity = quantity;
        this.tradePrice = tradePrice;
        this.timestamp = timestamp;
        this.takeFill = new Fill.Buy(this, takeOrder);
        this.provideFill = new Fill.Sell(this, provideOrder);
    }

    public String symbol() {
        return this.product.symbol();
    }
    public int quantity() {
        return this.quantity;
    }
    public double price() {
        return this.tradePrice;
    }
    public LocalDateTime timestamp() {
        return this.timestamp;
    }

    public Fill takeFill() {
        return this.takeFill;
    }
    public Fill provideFill() {
        return this.provideFill;
    }


    @Override
    public String toString() {
        return String.format("%s,%.2f,%s", product.symbol(), tradePrice, TRADE_TIME_FORMAT.format(timestamp));
    }

}
