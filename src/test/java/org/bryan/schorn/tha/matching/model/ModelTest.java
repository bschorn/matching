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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * Model/Entity Tests
 */
public class ModelTest {


    @Test
    void testTest() {
        assertEquals(1, 1);
    }

    @Test
    void createProduct() {
        //AAPL,false
        String symbol = "AAPL";
        Boolean halted = false;
        Product product = Product.create(symbol);
        product.setHalted(halted);

        assertAll("Product",
                () -> assertEquals(symbol, product.symbol()),
                () -> assertEquals(halted, product.isHalted()),
                () -> assertEquals("AAPL,false", product.toString())

        );

    }
    @Test
    void createOrder() {
        //AAPL,buy,limit,130.98,1608917400.7614357
        Instant timestamp = Instant.ofEpochSecond(1608917400, 7614357);
        String symbol = "AAPL";
        OrderType orderType = OrderType.LIMIT;
        Side side = Side.BUY;
        Double price = 130.98;
        Integer orderQty = 1;

        Order order = Order.create(timestamp, symbol, side, orderType, price, orderQty);
        assertAll("Order",
                () -> assertEquals(symbol, order.symbol()),
                () -> assertEquals(timestamp, order.timestamp()),
                () -> assertEquals(side, order.side()),
                () -> assertEquals(orderType, order.orderType()),
                () -> assertEquals(price, order.price()),
                () -> assertEquals(orderQty, order.orderQty()),
                () -> assertEquals("AAPL,BUY,LIMIT,130.98,1608917400.7614357", order.toString())
        );


    }

    @Test
    void createTrade() {
        //AAPL,130.98,2021-02-28T04:10:27.4071863
        String symbol = "AAPL";
        Integer quantity = 1;
        Double price = 130.98;
        Instant timestamp = Instant.now();
        String timestampStr = String.format("%d.%d", timestamp.getEpochSecond(), timestamp.getNano());
        String tradeStr = "AAPL,130.98," + timestampStr;

        Trade trade = Trade.create(symbol, quantity, price, timestamp);
        assertAll("Trade",
                () -> assertEquals(symbol, trade.symbol()),
                () -> assertEquals(timestamp, trade.timestamp()),
                () -> assertEquals(price, trade.price()),
                () -> assertEquals(quantity, trade.quantity()),
                () -> assertEquals(tradeStr, trade.toString())
        );
    }


    @Test
    void createSide() {
        assertAll("Side",
                () -> assertEquals(Side.BUY, Side.parse("buy")),
                () -> assertEquals(Side.SELL, Side.parse("sell")),
                () -> assertEquals(Side.BUY, Side.parse("B")),
                () -> assertEquals(Side.SELL, Side.parse("S")),
                () -> assertEquals(Side.BUY, Side.parse("Buy")),
                () -> assertEquals(Side.SELL, Side.parse("Sell")),
                () -> assertEquals(Side.SELL, Side.parse("buy").otherSide()),
                () -> assertEquals(Side.BUY, Side.parse("sell").otherSide()),
                () -> assertEquals(Side.SELL, Side.parse("B").otherSide()),
                () -> assertEquals(Side.BUY, Side.parse("S").otherSide()),
                () -> assertEquals(Side.SELL, Side.parse("Buy").otherSide()),
                () -> assertEquals(Side.BUY, Side.parse("Sell").otherSide()),
                () -> assertEquals(Side.UNKNOWN, Side.parse("garbage")),
                () -> assertEquals("BUY", Side.BUY.name()),
                () -> assertEquals("SELL", Side.SELL.name())
        );
    }

    @Test
    void createOrderType() {
        assertAll("OrderType",
                () -> assertEquals(OrderType.MARKET, OrderType.parse("market")),
                () -> assertEquals(OrderType.LIMIT, OrderType.parse("limit")),
                () -> assertEquals(OrderType.MARKET, OrderType.parse("mkt")),
                () -> assertEquals(OrderType.LIMIT, OrderType.parse("lmt")),
                () -> assertEquals(OrderType.MARKET, OrderType.parse("MARKET")),
                () -> assertEquals(OrderType.LIMIT, OrderType.parse("LIMIT")),
                () -> assertEquals(OrderType.MARKET, OrderType.parse("MKT")),
                () -> assertEquals(OrderType.LIMIT, OrderType.parse("LMT")),
                () -> assertEquals(OrderType.UNKNOWN, OrderType.parse("garbage")),
                () -> assertEquals("MARKET", OrderType.MARKET.name()),
                () -> assertEquals("LIMIT", OrderType.LIMIT.name())
        );
    }

    @Test
    void createOrderReject() {
        Instant timestamp = Instant.ofEpochSecond(1608917400, 7614357);
        String symbol = "AAPL";
        OrderType orderType = OrderType.LIMIT;
        Side side = Side.BUY;
        Double price = 130.98;
        Integer orderQty = 1;

        Order order = Order.create(timestamp, symbol, side, orderType, price, orderQty);
        Order.Reject reject = Order.reject(order, "rejected for testing");
        assertAll("OrderReject",
            () -> assertEquals("rejected for testing", reject.reason()),
            () -> assertEquals("AAPL,BUY,LIMIT,130.98,1608917400.7614357", reject.order().toString()),
            () -> assertEquals("AAPL,BUY,LIMIT,130.98,1608917400.7614357,rejected for testing", reject.toString())
        );

    }
}
