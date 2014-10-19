/*
 * Copyright (C) 2014 Samuel Audet
 *
 * This file is part of JavaCPP.
 *
 * JavaCPP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * JavaCPP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaCPP.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.bytedeco.javacpp.indexer;

/**
 * An interface implemented to let users access data classes via an {@link Indexer}.
 * The class implementing this interface can choose the type (byte, short, etc.) of
 * the indexer, and whether it is array-based or direct. The {@link Indexer#release()}
 * method should also be overridden to copy back any data written in the case of
 * non-direct indexers.
 *
 * @author Samuel Audet
 */
public interface Indexable {
    /**
     * Factory method called by the user to get an indexer to access the data.
     * Eventually, {@link Indexer#release()} should be called to have changes
     * reflected in the underlying data.
     *
     * @param <I> the type of the returned object
     * @param direct a hint for the implementation, leaving the choice up to the user, since
     *               buffers are slower than arrays on Android, but not with OpenJDK, for example
     * @return a concrete {@link Indexer}
     */
    <I extends Indexer> I createIndexer(boolean direct);
}
