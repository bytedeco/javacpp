/*
 * Copyright (C) 2020 Matteo Di Giovinazzo, Samuel Audet
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

package org.bytedeco.javacpp.indexer;

/**
 * An Index that supports only one size (a single dimension).
 *
 * @author Matteo Di Giovinazzo
 */
public class OneIndex extends Index {

    /** Constructor to set the {@link #sizes}. */
    public OneIndex(long size) {
        super(size);
    }

    /** Returns {@code i}. */
    @Override public long index(long i) {
        return i;
    }

    /** Throws {@code new UnsupportedOperationException()}. */
    @Override public long index(long i, long j) {
        throw new UnsupportedOperationException();
    }

    /** Throws {@code new UnsupportedOperationException()}. */
    @Override public long index(long i, long j, long k) {
        throw new UnsupportedOperationException();
    }

    /** Returns {@code indices[0]} if {@code indices.length == 1} or throws {@code new UnsupportedOperationException()}. */
    @Override public long index(long... indices) {
        if (indices.length == 1) {
            return indices[0];
        }
        throw new UnsupportedOperationException();
    }
}
