/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.pmd.exec;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import net.sourceforge.pmd.internal.Slf4jSimpleConfiguration;
import org.apache.maven.cli.logging.Slf4jConfiguration;
import org.apache.maven.cli.logging.Slf4jConfigurationFactory;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class Executor {
    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

    /**
     * Configures the appropriate log levels for PMD or disables
     * PMD logging.
     *
     * @param showPmdLog whether the PMD logs should appear in the maven log output
     * @param logLevel the maven log level, e.g. "info" or "debug".
     */
    protected void setupPmdLogging(boolean showPmdLog, String logLevel) {
        if (!showPmdLog) {
            System.setProperty("org.slf4j.simpleLogger.log.net.sourceforge.pmd", "off");
        } else {
            System.clearProperty("org.slf4j.simpleLogger.log.net.sourceforge.pmd");
        }

        // When slf4j-simple is in use, the log level is cached at each logger and the logger
        // instances are usually static. So they don't see any configuration changes at runtime
        // normally. This call will go through each logger and reinitialize these with the
        // freshly determined log level from the configuration.
        // Note: This only works for slf4j-simple.
        Slf4jSimpleConfiguration.reconfigureDefaultLogLevel(null);
    }

    /**
     * Initializes the maven logging system. This is only needed when toolchain is in use. In that
     * case we fork a new Java process to run PMD.
     *
     * @param logLevel the desired log level, e.g. "debug" or "info".
     */
    protected void setupLogLevel(String logLevel) {
        ILoggerFactory slf4jLoggerFactory = LoggerFactory.getILoggerFactory();
        Slf4jConfiguration slf4jConfiguration = Slf4jConfigurationFactory.getConfiguration(slf4jLoggerFactory);
        if ("debug".equalsIgnoreCase(logLevel)) {
            slf4jConfiguration.setRootLoggerLevel(Slf4jConfiguration.Level.DEBUG);
        } else if ("info".equalsIgnoreCase(logLevel)) {
            slf4jConfiguration.setRootLoggerLevel(Slf4jConfiguration.Level.INFO);
        } else {
            slf4jConfiguration.setRootLoggerLevel(Slf4jConfiguration.Level.ERROR);
        }
        slf4jConfiguration.activate();
    }

    protected static String buildClasspath() {
        StringBuilder classpath = new StringBuilder();

        // plugin classpath needs to come first
        ClassLoader pluginClassloader = Executor.class.getClassLoader();
        buildClasspath(classpath, pluginClassloader);

        ClassLoader coreClassloader = ConsoleLogger.class.getClassLoader();
        buildClasspath(classpath, coreClassloader);

        return classpath.toString();
    }

    static void buildClasspath(StringBuilder classpath, ClassLoader cl) {
        if (cl instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader) cl).getURLs()) {
                if ("file".equalsIgnoreCase(url.getProtocol())) {
                    try {
                        String filename = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8.name());
                        classpath.append(new File(filename).getPath()).append(File.pathSeparatorChar);
                    } catch (UnsupportedEncodingException e) {
                        LOG.warn("Ignoring " + url + " in classpath due to UnsupportedEncodingException", e);
                    }
                }
            }
        }
    }

    protected static class ProcessStreamHandler implements Runnable {
        private static final int BUFFER_SIZE = 8192;

        private final BufferedInputStream in;
        private final BufferedOutputStream out;

        public static void start(InputStream in, OutputStream out) {
            Thread t = new Thread(new ProcessStreamHandler(in, out));
            t.start();
        }

        private ProcessStreamHandler(InputStream in, OutputStream out) {
            this.in = new BufferedInputStream(in);
            this.out = new BufferedOutputStream(out);
        }

        @Override
        public void run() {
            byte[] buffer = new byte[BUFFER_SIZE];
            try {
                int count = in.read(buffer);
                while (count != -1) {
                    out.write(buffer, 0, count);
                    out.flush();
                    count = in.read(buffer);
                }
                out.flush();
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }
}
