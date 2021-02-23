/*
 * The MIT License
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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.bryan.schorn.tha.matching.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author bschorn
 */
public class CommandLineArgs {
    static private final Logger LGR = LoggerFactory.getLogger(CommandLineArgs.class);
    static public CommandLineArgs create(String[] args) {
        return new CommandLineArgs(args);
    }
    static public String APPLICATION_PROPERTIES = "/application.properties";

    private final Properties properties = new Properties();
    private final Map<String, Object> propertiesMap = new HashMap<>();

    private CommandLineArgs(String[] args) {
        String paramName = null;
        String paramValue = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                if (paramName != null && paramValue != null) {
                    this.properties.setProperty(paramName, paramValue);
                    Object obj = this.propertiesMap.get(paramName);
                    if (obj == null) {
                        this.propertiesMap.put(paramName, paramValue);
                    }
                    paramName = null;
                    paramValue = null;
                } else if (paramName != null) {
                    this.properties.setProperty(paramName, Boolean.TRUE.toString());
                    paramName = null;
                }
            }
            if (arg.startsWith("--")) {
                paramName = arg.substring(2);
            } else if (arg.startsWith("-")) {
                paramName = arg.substring(1);
            } else {
                paramValue = arg;
            }
        }
        if (paramName != null && paramValue != null) {
            this.properties.setProperty(paramName, paramValue);
        }
        try {
            InputStream inputStream = CommandLineArgs.class.getResourceAsStream(APPLICATION_PROPERTIES);
            this.properties.load(inputStream);
        } catch (Exception ex) {
            LGR.error(ToString.stackTrace(ex));
        }
    }
    public Properties getProperties() {
        return this.properties;
    }

    public boolean hasParameterFlag(String flagName) {
        return Boolean.valueOf(this.properties.getProperty(flagName, Boolean.FALSE.toString()));
    }

    public String getParameterValue(String parameterName) {
        return this.properties.getProperty(parameterName);
    }
}
