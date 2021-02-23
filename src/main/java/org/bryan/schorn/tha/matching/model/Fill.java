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
 * Fill
 */
public abstract class Fill {

    private final Trade trade;
    private Fill(Trade trade) {
        this.trade = trade;
    }

    public int fillQty() {
        return this.trade.quantity();
    }
    public double fillPrice() { return this.trade.price(); }
    public LocalDateTime timestamp() {
        return this.trade.timestamp();
    }
    abstract public Side side();

    @Override
    public String toString() {
        return String.format("%s,%s,%d,%.2f",
                DateTimeFormatter.ISO_DATE_TIME.format(this.timestamp()),
                this.side(),
                this.fillQty(),
                this.fillPrice()
                );
    }


    /**
     * Buy Fill
     */
    static public class Buy extends Fill {

        private final Order order;
        public Buy(Trade trade, Order order) {
            super(trade);
            this.order = order;
        }
        public Side side() {
            return Side.BUY;
        }
    }

    /**
     * Sell Fill
     */
    static public class Sell extends Fill {

        private final Order order;
        public Sell(Trade trade, Order order) {
            super(trade);
            this.order = order;
        }
        public Side side() {
            return Side.SELL;
        }
    }
}
