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

import org.bryan.schorn.tha.matching.util.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 *  Activity Logs
 */
public class ActivityLog<E> implements Callable<Integer> {
    static private final Logger LGR = LoggerFactory.getLogger(ActivityLog.class);

    Supplier<E> supplier;
    String filename;
    String header;
    boolean stop = false;
    public ActivityLog(Supplier<E> supplier, String filename, String header) {
        this.supplier = supplier;
        this.filename = filename;
        this.header = header;
        LGR.info("Created logging instance to file: {}", this.filename);
    }

    public void stop() {
        this.stop = true;
    }

    @Override
    public Integer call() {
        Integer writeCount = 0;
        try {
            Path tradeFilePath = Paths.get(filename);
            E entity = this.supplier.get();
            //LGR.info("Logging {} activity to {}", entity.getClass().getSimpleName(), tradeFilePath);
            try (BufferedWriter writer = Files.newBufferedWriter(tradeFilePath,
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
                writer.write(header + System.lineSeparator());
                while (!this.stop || entity != null) {
                    if (entity != null) {
                        writer.write(entity.toString());
                        writer.write(System.lineSeparator());
                        ++writeCount;
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (Exception ex) {
                            LGR.warn(ex.getMessage());
                        }
                    }
                    entity = this.supplier.get();
                }
            }
        } catch (Exception ex) {
            LGR.error(ToString.stackTrace(ex));
        }
        return writeCount;
    }

}
