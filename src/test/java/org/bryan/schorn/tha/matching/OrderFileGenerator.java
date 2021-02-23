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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class OrderFileGenerator {

    static final String TARGET_ID = "THA";
    static final String[] SIDES = new String[]{"BUY","SELL"};
    static final String[] ORDER_TYPES = new String[]{"MARKET","LIMIT"};
    static final String[] HEADER = new String[]{"send_time","sender_id","target_id","cl_ord_id","symbol","side","order_type","price","order_qty"};
    static final DateTimeFormatter SEND_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.S");
    static final DateTimeFormatter CL_ORD_FORMAT = DateTimeFormatter.ofPattern("yyMMddHHmmssS");

    static class PriceRange {
        String symbol;
        double loPrice;
        double hiPrice;
    }

    private final Map<String,PriceRange> priceRanges = new HashMap<>();
    private final List<String> senders = new ArrayList<>();

    private double marketOrderFactor = 0.15;
    OrderFileGenerator(double marketOrderFactor) {
        this.marketOrderFactor = marketOrderFactor;
    }
    OrderFileGenerator() {}

    void setSenders(String...senders) {
        for (String sender : senders) {
            this.senders.add(sender);
        }
    }
    void addParticipant(String symbol, double loPrice, double hiPrice) {
        PriceRange priceRange = new PriceRange();
        priceRange.symbol = symbol;
        priceRange.loPrice = loPrice;
        priceRange.hiPrice = hiPrice;
        this.priceRanges.put(symbol,priceRange);
    }

    void createFile(String filename, int count) throws Exception {
        List<String> sendTimes = getSendTimes(count);
        StringJoiner headJoiner = new StringJoiner(",","","\n");
        for (String field : HEADER) {
            headJoiner.add(field);
        }
        StringJoiner fileJoiner = new StringJoiner("\n",headJoiner.toString(),"");
        for (String sendTime : sendTimes) {
            String symbol = getRandom("symbol");
            String side = getRandom("side");
            String senderId = getRandom("sender_id");
            String orderType = getOrderType();
            Map<String,String> line = new HashMap<>();
            line.put("send_time", sendTime);
            line.put("sender_id",senderId);
            line.put("target_id",TARGET_ID);
            line.put("cl_ord_id",getClOrdId(senderId, sendTime));
            line.put("symbol",symbol);
            line.put("side",side);
            line.put("order_type",orderType);
            line.put("price",getPrice(symbol, side, orderType));
            line.put("order_qty","1");
            StringJoiner lineJoiner = new StringJoiner(",","","");
            for (String field : HEADER) {
                lineJoiner.add(line.get(field));
            }
            fileJoiner.add(lineJoiner.toString());
        }
        Path path = Paths.get(filename);
        Files.write(path, fileJoiner.toString().getBytes());
    }

    /**
     * Generate a list with 'count' number of send times, each 1 second apart starting now.
     *
     * @param count
     * @return
     */
    List<String> getSendTimes(int count) {
        List<String> times = new ArrayList<>();
        LocalDateTime startTime = LocalDateTime.now();
        for (int i = 0; i < count; i++) {
            times.add(SEND_TIME_FORMAT.format(startTime.plusSeconds(i)));
        }
        return times;
    }

    /**
     * Concatentate the senderId and sendTime to generate a client order id.
     *
     * @param senderId
     * @param sendTime
     * @return
     */
    String getClOrdId(String senderId, String sendTime) {
        return String.format("%s%s", senderId, CL_ORD_FORMAT.format(SEND_TIME_FORMAT.parse(sendTime)));
    }

    /**
     * Randomize the sequence of Order Types but keep the market orders % of total aligned with the factor.
     * @return
     */
    String getOrderType() {
        double randomNumber = Math.random();
        if (randomNumber <= this.marketOrderFactor) {
            return ORDER_TYPES[0];
        } else {
            return ORDER_TYPES[1];
        }
    }

    /**
     * Market orders are 0 and non-market orders are randomized with buys in bottom 2/3 of price
     * range and sells in top 2/3 of price range.
     *
     * @param symbol
     * @param side
     * @param orderType
     * @return
     */
    String getPrice(String symbol, String side, String orderType) {
        if (orderType.equals("MARKET")) {
            return "0";
        }
        PriceRange priceRange = this.priceRanges.get(symbol);
        double spread = (priceRange.hiPrice - priceRange.loPrice) / 3;
        if (side.equals("BUY")) {
            return getRandomPrice(priceRange.loPrice, priceRange.loPrice+(2*spread));
        } else {
            return getRandomPrice(priceRange.hiPrice-(2*spread), priceRange.hiPrice);
        }
    }

    /**
     * Randomize parameters
     *
     * @param parameter
     * @return
     * @throws Exception
     */
    String getRandom(String parameter) throws Exception {
        int idx = 0;
        switch (parameter) {
            case "sender_id":
                idx = getRandomNumber(0, this.senders.size()-1);
                return this.senders.get(idx);
            case "symbol":
                idx = getRandomNumber(0, this.priceRanges.size()-1);
                return this.priceRanges.values().stream()
                        .map(r -> r.symbol)
                        .collect(Collectors.toList())
                        .get(idx);
            case "side":
                idx = getRandomNumber(0,2);
                idx = idx == 2 ? 1 : idx;
                return SIDES[idx];
        }
        throw new Exception(String.format("getRandom() - parameter: %s not understood", parameter == null ? "null" : parameter));
    }

    private String getRandomPrice(double min, double max) {
        return String.format("%.2f", (Math.random() * (max - min)) + min);
    }
    private int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    static public void create(String filename, int count) {
        try {
            OrderFileGenerator ofg = new OrderFileGenerator(0.75);
            ofg.addParticipant("AMZN",3239.0, 3242.0);
            ofg.addParticipant("AAPL",130.0, 134.0);
            ofg.addParticipant("GOOG",2114.0, 2120.0);
            ofg.addParticipant("TSLA",775.0, 781.0);
            ofg.addParticipant("FB",264.0, 268.0);
            ofg.setSenders("DEN","YYZ","MCI");
            ofg.createFile(filename, count);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    static public void main(String[] args) {
        OrderFileGenerator.create("orders1.csv", 100);
    }
}
