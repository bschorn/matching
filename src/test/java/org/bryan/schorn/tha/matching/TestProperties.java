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

package org.bryan.schorn.tha.matching;

import java.util.Properties;

public enum TestProperties {
    ProductFeed("org.bryan.schorn.tha.matching.mock.MockProductFeed"),
    OrderFeed("org.bryan.schorn.tha.matching.mock.MockOrderFeed"),
    MockOrderParser("org.bryan.schorn.tha.matching.mock.MockOrderFeedParser"),
    OrderFileHeader("symbol,side,type,price,timestamp"),
    RejectedFileHeader("symbol,side,type,price,timestamp,reason"),
    TradeFileHeader("symbol,price,timestamp"),
    ProductFileHeader("symbol,is_halted"),
    OrderBookFileHeader("symbol,price,buys,sells"),
    ProductFile("inputs-test/symbols.csv"),
    OrderFile("inputs-test/orders.csv"),
    TradeFile("trades-test.txt"),
    RejectedFile("rejected-test.txt"),
    OrderBookFile("order_book-test.txt"),
    OutputDir("outputs-test"),
    ReferenceDir("reference-test");

    private final String value;
    TestProperties(String value) {
        this.value = value;
    }
    public String value() {
        return this.value;
    }
    public String key() {
        return this.name();
    }
    static public Properties getProperties() {
        Properties properties = new Properties();
        for (TestProperties x : TestProperties.values()) {
            properties.setProperty(x.key(), x.value());
        }
        return properties;
    }
}
