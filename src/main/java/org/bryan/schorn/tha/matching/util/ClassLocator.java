/*
 * The MIT License
 *
 * Copyright 2021 Bryan Schorn.
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

import java.util.Properties;
import java.util.StringJoiner;

/**
 *
 * Implementation Class Finder
 *
 * @author schorn
 *
 */
public interface ClassLocator {

    static public ClassLocator create(Properties properties) {
        return new Impl(properties);
    }

    public Class<?> getImplClass(String interfaceName) throws Exception;

    public Class<?> getImplClass(Class<?> interfaceClass) throws Exception;

    public <T> T newInstance(Class<T> interfaceClass);

    public Object newInstance(String interfaceName) throws Exception;

    /**
     *
     */
    static class Impl implements ClassLocator {

        static private final Logger LGR = LoggerFactory.getLogger(ClassLocator.class);

        private final Properties properties;

        private Impl(Properties properties) {
            this.properties = properties;
        }

        /**
         *
         * @param interfaceName
         * @return
         * @throws Exception
         */
        protected String getImplClassName(String interfaceName) throws Exception {
            String[] interfaceNameParts = interfaceName.split("\\.");
            int length = interfaceNameParts.length;
            for (int i = 0; i < length; ++i) {
                StringJoiner joiner = new StringJoiner(".", "", "");
                for (int j = i; j < length; ++j) {
                    joiner.add(interfaceNameParts[j]);
                }
                String implName = this.properties.getProperty(joiner.toString());
                if (implName != null) {
                    return implName;
                }
            }
            throw new Exception(String.format("%s.getImplClassName() - properties missing '%s' entry with class path of implementation.",
                    ClassLocator.class.getSimpleName(), interfaceName));
        }

        @Override
        public Class<?> getImplClass(String interfaceName) throws Exception {
            String implName = getImplClassName(interfaceName);
            try {
                Class<?> implClass = Class.forName(implName);
                return implClass;
            } catch (Exception ex) {
                String msg = String.format(
                        "%s.getImplClass() - failed to get an implementation class for interface '%s'. [%s]",
                        this.getClass().getSimpleName(),
                        interfaceName,
                        ex != null ? ex.getMessage() : "exception");
                throw new Exception(msg);
            }
        }

        @Override
        public Class<?> getImplClass(Class<?> interfaceClass) throws Exception {
            return getImplClass(interfaceClass.getSimpleName());
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T newInstance(Class<T> interfaceClass) {
            try {
                return (T) getImplClass(interfaceClass).getConstructor().newInstance();
            } catch (Exception ex) {
                LGR.error(ToString.stackTrace(ex));
            }
            return null;
        }

        /**
         *
         * @param interfaceName
         * @return
         * @throws Exception
         */
        @Override
        public Object newInstance(String interfaceName) throws Exception {
            return getImplClass(interfaceName).getConstructor().newInstance();
        }
    }
}
