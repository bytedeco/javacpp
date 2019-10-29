/*
 * Copyright (C) 2018-2019 Samuel Audet
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

package org.bytedeco.javacpp;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import org.bytedeco.javacpp.tools.Logger;

/**
 * {@link Pointer} objects attach themselves automatically on {@link Pointer#init} to the first {@link PointerScope}
 * found in {@link #scopeStack} that they can to based on the classes found in {@link #forClasses}. The user can then
 * call {@link #deallocate()}, or rely on {@link #close()} to release in a timely fashion all attached Pointer objects,
 * instead of relying on the garbage collector.
 */
public class PointerScope implements AutoCloseable {
    private static final Logger logger = Logger.create(PointerScope.class);

    /** A thread-local stack of {@link PointerScope} objects. Pointer objects attach themselves
     * automatically on {@link Pointer#init} to the first one they can to on the stack. */
    static final ThreadLocal<Deque<PointerScope>> scopeStack = new ThreadLocal<Deque<PointerScope>>() {
        @Override protected Deque initialValue() {
            return new ArrayDeque<PointerScope>();
        }
    };

    /** Returns {@code scopeStack.get().peek()}, the last opened scope not yet closed. */
    public static PointerScope getInnerScope() {
        return scopeStack.get().peek();
    }

    /** Returns {@code scopeStack.get().iterator()}, all scopes not yet closed. */
    public static Iterator<PointerScope> getScopeIterator() {
        return scopeStack.get().iterator();
    }

    /** The stack keeping references to attached {@link Pointer} objects. */
    Deque<Pointer> pointerStack = new ArrayDeque<Pointer>();

    /** When not empty, indicates the classes of objects that are allowed to be attached. */
    Class<? extends Pointer>[] forClasses = null;

    /** When set to true, the next call to {@link #close()} does not release but resets this variable. */
    boolean extend = false;

    /** Initializes {@link #forClasses}, and pushes itself on the {@link #scopeStack}. */
    public PointerScope(Class<? extends Pointer>... forClasses) {
        if (logger.isDebugEnabled()) {
            logger.debug("Opening " + this);
        }
        this.forClasses = forClasses;
        scopeStack.get().push(this);
    }

    public Class<? extends Pointer>[] forClasses() {
        return forClasses;
    }

    /** Pushes the Pointer onto the {@link #pointerStack} of this Scope and calls {@link Pointer#retainReference()}.
     * @throws IllegalArgumentException when it is not an instance of a class in {@link #forClasses}. */
    public PointerScope attach(Pointer p) {
        if (logger.isDebugEnabled()) {
            logger.debug("Attaching " + p + " to " + this);
        }
        if (forClasses != null && forClasses.length > 0) {
            boolean found = false;
            for (Class<? extends Pointer> c : forClasses) {
                if (c != null && c.isInstance(p)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException(p + " is not an instance of a class in forClasses: " + Arrays.toString(forClasses));
            }
        }
        pointerStack.push(p);
        p.retainReference();
        return this;
    }

    /** Removes the Pointer from the {@link #pointerStack} of this Scope
     * and calls {@link Pointer#releaseReference()}. */
    public PointerScope detach(Pointer p) {
        if (logger.isDebugEnabled()) {
            logger.debug("Detaching " + p + " from " + this);
        }
        pointerStack.remove(p);
        p.releaseReference();
        return this;
    }

    /** Extends the life of this scope past the next call
     * to {@link #close()} by setting the {@link #extend} flag. */
    public PointerScope extend() {
        if (logger.isDebugEnabled()) {
            logger.debug("Extending " + this);
        }
        extend = true;
        return this;
    }

    /** Pops from {@link #pointerStack} all attached pointers,
     * calls {@link Pointer#releaseReference()} on them, unless extended,
     * in which case it only resets the {@link #extend} flag instead,
     * and finally removes itself from {@link #scopeStack}. */
    @Override public void close() {
        if (logger.isDebugEnabled()) {
            logger.debug("Closing " + this);
        }
        if (extend) {
            extend = false;
        } else {
            while (pointerStack.size() > 0) {
                pointerStack.pop().releaseReference();
            }
        }
        scopeStack.get().remove(this);
    }

    /** Pops from {@link #pointerStack} all attached pointers,
     * and calls {@link Pointer#deallocate()} on them. */
    public void deallocate() {
        if (logger.isDebugEnabled()) {
            logger.debug("Deallocating " + this);
        }
        while (pointerStack.size() > 0) {
            pointerStack.pop().deallocate();
        }
    }
}
