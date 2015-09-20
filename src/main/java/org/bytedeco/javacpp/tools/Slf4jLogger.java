/*
 * Copyright (C) 2015 Samuel Audet
 *
 * Licensed either under the Apache License, Version 2.0, or (at your option)
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation (subject to the "Classpath" exception),
 * either version 2, or any later version (collectively, the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     http://www.gnu.org/licenses/
 *     http://www.gnu.org/software/classpath/license.html
 *
 * or as provided in the LICENSE.txt file that accompanied this code.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bytedeco.javacpp.tools;

/**
 * A logger that delegates messages to <a href="http://www.slf4j.org/">SLF4J</a>.
 *
 * @author Samuel Audet
 */
public class Slf4jLogger extends org.bytedeco.javacpp.tools.Logger {
    final org.slf4j.Logger logger;

    /** Calls {@link org.slf4j.LoggerFactory#getLogger(Class)}. */
    public Slf4jLogger(Class cls) {
        logger = org.slf4j.LoggerFactory.getLogger(cls);
    }

    /** Returns {@link org.slf4j.Logger#isDebugEnabled()}. */
    @Override public boolean isDebugEnabled() { return logger.isDebugEnabled(); }
    /** Returns {@link org.slf4j.Logger#isInfoEnabled()}. */
    @Override public boolean isInfoEnabled()  { return logger.isInfoEnabled();  }
    /** Returns {@link org.slf4j.Logger#isWarnEnabled()}. */
    @Override public boolean isWarnEnabled()  { return logger.isWarnEnabled();  }
    /** Returns {@link org.slf4j.Logger#isErrorEnabled()}. */
    @Override public boolean isErrorEnabled() { return logger.isErrorEnabled(); }

    /** Calls {@link org.slf4j.Logger#debug(String)}. */
    @Override public void debug(String s) { logger.debug(s); }
    /** Calls {@link org.slf4j.Logger#info(String)}. */
    @Override public void info(String s)  { logger.info(s);  }
    /** Calls {@link org.slf4j.Logger#warn(String)}. */
    @Override public void warn(String s)  { logger.warn(s);  }
    /** Calls {@link org.slf4j.Logger#error(String)}. */
    @Override public void error(String s) { logger.error(s); }
}
