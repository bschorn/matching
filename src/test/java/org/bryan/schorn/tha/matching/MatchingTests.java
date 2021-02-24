package org.bryan.schorn.tha.matching;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofMinutes;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


import org.bryan.schorn.tha.matching.engine.Engine;
import org.bryan.schorn.tha.matching.engine.OrderThrottleRule;
import org.bryan.schorn.tha.matching.engine.ProductHalted;
import org.bryan.schorn.tha.matching.model.Order;
import org.bryan.schorn.tha.matching.model.OrderType;
import org.bryan.schorn.tha.matching.model.Product;
import org.bryan.schorn.tha.matching.model.Side;
import org.bryan.schorn.tha.matching.model.impl.OrderImpl;
import org.bryan.schorn.tha.matching.util.ToString;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MatchingTests {

    static private final Logger LGR = LoggerFactory.getLogger(MatchingTests.class);

    String targetId = "ENG";
    String senderId = "TEST";
    String clOrdid = "test01";

    enum Symbol { ABC(false),XYZ(true);
        boolean isHalted;
        Symbol(boolean isHalted) { this.isHalted = isHalted; }
        boolean isHalted() { return this.isHalted; }
    }
    enum Scenario {
        REJECTED1,
        REJECTED2,
        MATCHED1,
        MATCHED2,
        END;
    }

    Map<Symbol,Product> products = new HashMap<>();;
    Engine engine;
    Map<Scenario,List<Order>> scenarios = new HashMap<>();

    MatchingTests() {
        try {
            for (Symbol symbol : Symbol.values()) {
                products.put(symbol, new Product.Builder().setSymbol(symbol.name()).setHalted(symbol.isHalted()).build());
            }
            engine = new Engine(products.values());
            scenarios.put(Scenario.REJECTED1,Arrays.asList(new Order[]{
                    createOrder(Symbol.ABC, OrderType.MARKET,Side.BUY,0),
                    createOrder(Symbol.XYZ, OrderType.LIMIT, Side.BUY,10)
            }));
        } catch (Exception ex) {
            LGR.error(ToString.stackTrace(ex));
        }
    }

    Order createOrder(Symbol symbol, OrderType orderType, Side side, double price) {
        try {
            return new OrderImpl.BuilderImpl().setProduct(products.get(symbol)).setOrderQty(1)
                    .setOrderType(orderType).setPrice(price).setSide(side)
                    .setTargetId(targetId).setSenderId(senderId).setClOrdId(clOrdid).build();

        } catch (Exception ex) {
            LGR.error(ToString.stackTrace(ex));
        }
        return null;
    }

    @Test
    void testRejectScenarios() {
        try {
            this.engine.addRule(ProductHalted.PRODUCTED_HALTED);
            this.engine.startLoop();
            for (Order order : scenarios.get(Scenario.REJECTED1)) {
                this.engine.accept(order);
                Thread.sleep(50);
            }
            Thread.sleep(1000);
            this.engine.stopLoop();
            for (Order order : scenarios.get(Scenario.REJECTED1)) {
                Order.Rejected rejectedOrder = this.engine.rejected().get();
                if (rejectedOrder != null) {
                    assertEquals(rejectedOrder.order(), order);
                }
            }
            this.engine.removeRule(ProductHalted.PRODUCTED_HALTED);
        } catch (Exception ex) {
            LGR.error(ToString.stackTrace(ex));
        }
    }

    void engineRules() {
        // add rule for trade halts
        this.engine.addRule(ProductHalted.PRODUCTED_HALTED);
        // add rule for the 3 orders in one second
        this.engine.addRule(OrderThrottleRule.MAX_THREE_PER_SECOND);

    }

    @Test
    void testOrderCreation() {
        try {
            Order.Builder builder = new OrderImpl.BuilderImpl();
            Order order01 = builder.setProduct(products.get(Symbol.ABC)).setOrderQty(1)
                    .setOrderType(OrderType.LIMIT).setPrice(10.0).setSide(Side.BUY)
                    .setTargetId(targetId).setSenderId(senderId).setClOrdId(clOrdid).build();
            assertAll("order",
                    () -> assertEquals(Symbol.ABC.name(), order01.product().symbol()),
                    () -> assertEquals(Symbol.ABC.isHalted(), order01.product().isHalted()),
                    () -> assertEquals(1, order01.orderQty()),
                    () -> assertEquals(OrderType.LIMIT, order01.orderType()),
                    () -> assertEquals(Side.BUY, order01.side()),
                    () -> assertEquals(10.0, order01.price()),
                    () -> assertEquals(1, order01.orderQty())
            );
        } catch (Exception ex) {
            LGR.error(ToString.stackTrace(ex));
        }
    }

    @Test
    void standardAssertions() {
        //assertEquals(4, engine.multiply(2, 2), "The optional failure message is now the last parameter");
        assertTrue('a' < 'b', () -> "Assertion messages can be lazily evaluated -- "
                + "to avoid constructing complex messages unnecessarily.");
    }

    @Test
    void groupedAssertions() {
        // In a grouped assertion all assertions are executed, and all
        // failures will be reported together.
        /*
        assertAll("product",
                () -> assertEquals("XYZ", product.symbol()),
                () -> assertEquals(false, product.isHalted())
        );
        */
    }



    private static String greeting() {
        return "Matching Tests";
    }

}