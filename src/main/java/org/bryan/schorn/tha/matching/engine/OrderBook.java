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

package org.bryan.schorn.tha.matching.engine;

import org.bryan.schorn.tha.matching.model.Order;
import org.bryan.schorn.tha.matching.model.Product;
import org.bryan.schorn.tha.matching.model.Side;

import java.util.*;
import java.util.function.Consumer;

/**
 * This data structure is not thread-safe. It is assumed that a
 * single Engine (with a single thread) will access the OrderBook.
 *
 * 1 Product -> 1 Engine -> 1 OrderBook
 *
 * This is not an 'ideal' OrderBook as it's single purpose is to service
 * the Engine and is not designed for publishing OrderBook data.
 *
 * A true OrderBook structure is beyond the scope of a 3-hour project.
 */
public class OrderBook {

    static final private Comparator<Double> SORT_BUYS = (a, b) -> a == 0.0 || b == 0.0 ? Double.compare(a, b) : Double.compare(b, a);
    static final private Comparator<Double> SORT_SELLS = (a, b) -> Double.compare(a, b);

    private final Product product;
    private final TreeMap<Double, Deque<Order>> buys = new TreeMap<>(SORT_BUYS);
    private final TreeMap<Double, Deque<Order>> sells = new TreeMap<>(SORT_SELLS);

    // reusable list
    private final List<Order> takeList = new ArrayList<>();

    /**
     * Order Books are maintained by Product
     *
     * @param product
     */
    public OrderBook(Product product) {
        this.product = product;
    }

    public Product product() {
        return this.product;
    }

    void accept(Order order) {
        if (order.side() == Side.BUY) {
            Deque<Order> q = this.buys.get(order.price());
            if (q == null) {
                q = new ArrayDeque<>();
                this.buys.put(order.price(), q);
            }
            q.offer(order);
        } else {
            Deque<Order> q = this.sells.get(order.price());
            if (q == null) {
                q = new ArrayDeque<>();
                this.sells.put(order.price(), q);
            }
            q.offer(order);
        }
    }

    List<Order> take(Side side, int quantity, double price) {
        if (side == Side.BUY) {
            return takeBuy(quantity, price);
        } else {
            return takeSell(quantity, price);
        }
    }

    private List<Order> takeSell(int takeQty, double takePrice) {
        takeList.clear();
        for (Double price : this.sells.keySet()) {
            if (takePrice > 0 && price > takePrice) break;
            Queue<Order> q = this.sells.get(price);
            while (takeQty > 0 && !q.isEmpty()) {
                Order order = q.peek();
                if (takeQty >= order.unfilledQty()) {
                    q.remove();
                }
                takeQty -= Math.min(takeQty, order.unfilledQty());
                takeList.add(order);
            }
            if (takeQty == 0) {
                return takeList;
            }
        }
        return takeList;
    }

    private List<Order> takeBuy(int takeQty, double takePrice) {
        takeList.clear();
        for (Double price : this.buys.keySet()) {
            if (takePrice > 0 && price < takePrice) break;
            Queue<Order> q = this.buys.get(price);
            while (takeQty > 0 && !q.isEmpty()) {
                Order order = q.peek();
                if (takeQty >= order.unfilledQty()) {
                    q.remove();
                }
                takeQty -= Math.min(takeQty, order.unfilledQty());
                takeList.add(order);
            }
            if (takeQty == 0) {
                return takeList;
            }
        }
        return takeList;
    }

    @Override
    public String toString() {
        int sellSize = this.sells.keySet().size();
        Double[] sellPrices = new Double[sellSize];
        for (Double price : this.sells.keySet()) {
            sellPrices[--sellSize] = price;
        }
        StringJoiner joiner = new StringJoiner(System.lineSeparator(),"","");
        for (Double price : sellPrices) {
            int qty = this.sells.get(price).stream().map(o -> o.unfilledQty()).reduce(0, Integer::sum);
            if (qty > 0)
                joiner.add(String.format("%s,%.2f,%d,%d", this.product.symbol(), price, 0, qty));
        }
        for (Double price : this.buys.keySet()) {
            int qty = this.buys.get(price).stream().map(o -> o.unfilledQty()).reduce(0, Integer::sum);
            if (qty > 0)
                joiner.add(String.format("%s,%.2f,%d,%d", this.product.symbol(), price, qty, 0));
        }
        joiner.add("");
        return joiner.toString();
    }
}
