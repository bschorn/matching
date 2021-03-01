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

package org.bryan.schorn.tha.matching.engine.rule;

import org.bryan.schorn.tha.matching.engine.Engine;
import org.bryan.schorn.tha.matching.model.Order;
import org.bryan.schorn.tha.matching.model.OrderType;
import org.bryan.schorn.tha.matching.model.Side;
import org.bryan.schorn.tha.matching.product.Products;


/**
 *
 */
public class CheckRequiredFields implements Engine.Rule {

    static public CheckRequiredFields CHECK_REQUIRED_FIELDS = new CheckRequiredFields();

    @Override
    public boolean test(Order order) {
        switch (order.orderType()) {
            case LIMIT:
                if (order.price() == null || order.price() <= 0.0) {
                    return false;
                }
            case MARKET:
                if (order.orderQty() == null || order.orderQty() <= 0) {
                    return false;
                }
                if (order.symbol() == null || Products.find(order.symbol()) == null) {
                    return false;
                }
                if (order.side() == null || order.side() == Side.UNKNOWN) {
                    return false;
                }
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public String getReason(Order order) {
        return "missing-required-field";
    }

    @Override
    public String ruleDescription() {
        return "Reject orders that are missing required fields.";
    }
}
