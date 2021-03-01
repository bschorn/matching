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

package org.bryan.schorn.tha.matching.order;

import org.bryan.schorn.tha.matching.model.Order;
import org.bryan.schorn.tha.matching.util.ClassLocator;

import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Order Feed Interface
 */
public interface OrderFeed extends Supplier<Order>, Callable<Integer> {
    @Override
    Order get();

    void connect() throws Exception;

    static OrderFeed create(Properties properties) throws Exception {
        ClassLocator classLocator = ClassLocator.create(properties);
        AbstractOrderFeed orderFeed = (AbstractOrderFeed) classLocator.newInstance(OrderFeed.class.getSimpleName());
        orderFeed.setProperties(properties);
        return orderFeed;
    }

    abstract class AbstractOrderFeed implements OrderFeed {
        protected abstract void setProperties(Properties properties) throws Exception;
    }

    /**
     * OrderFeed.Parser Interface
     * @param <T>
     */
    interface Parser<T> extends Function<T, Order> {
        @Override
        Order apply(T t);

        void setHeader(String header);
    }
}
