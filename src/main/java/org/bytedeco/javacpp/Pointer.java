/*
 * Copyright (C) 2011,2012,2013,2014 Samuel Audet
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

package org.bytedeco.javacpp;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.bytedeco.javacpp.tools.Generator;

/**
 * All peer classes to native types must be descended from Pointer, the topmost class.
 * It can be thought as mapping the native C++ {@code void*}, which can point to any
 * {@code struct}, {@code class}, or {@code union}. All Pointer classes get parsed
 * by {@link Generator} to produce proper wrapping JNI code, but this base class also
 * provides functionality to access native array elements as well as utility methods
 * and classes to let users benefit from garbage collection.
 * <p>
 * For examples of subclasses, please refer to the following:
 *
 * @see BytePointer
 * @see ShortPointer
 * @see IntPointer
 * @see LongPointer
 * @see FloatPointer
 * @see DoublePointer
 * @see CharPointer
 * @see PointerPointer
 * @see BoolPointer
 * @see CLongPointer
 * @see SizeTPointer
 *
 * @author Samuel Audet
 */
public class Pointer {
    /** Default constructor that does nothing. */
    public Pointer() {
        // Make sure our top enclosing class is initialized and the
        // associated native library loaded. This code is actually quite efficient,
        // but it still does not cover all corner cases...
//        Class cls = getClass();
//        String className = cls.getName();
//        int topIndex = className.indexOf('$');
//        if (topIndex > 0 && Loader.loadedLibraries.
//                get(className.substring(0, topIndex)) == null) {
//            Loader.load(cls);
//        }
    }
    /**
     * Copies the address, position, limit, and capacity of another Pointer.
     * Also keeps a reference to it to prevent its memory from getting deallocated.
     * <p>
     * This copy constructor basically acts as a {@code reinterpret_cast}, at least
     * on plain old data (POD) {@code struct}, so we need to be careful with it.
     *
     * @param p the other Pointer to reference
     */
    public Pointer(final Pointer p) {
        if (p != null) {
            address = p.address;
            position = p.position;
            limit = p.limit;
            capacity = p.capacity;
            if (p.deallocator != null) {
                deallocator = new Deallocator() { public void deallocate() { p.deallocate(); } };
            }
        }
    }

    /**
     * Copies the address, position, limit, and capacity of a direct NIO {@link Buffer}.
     * Also keeps a reference to it to prevent its memory from getting deallocated.
     *
     * @param b the Buffer object to reference
     */
    public Pointer(final Buffer b) {
        if (b != null) {
            allocate(b);
        }
        if (!isNull()) {
            position = b.position();
            limit = b.limit();
            capacity = b.capacity();
            deallocator = new Deallocator() { Buffer bb = b; public void deallocate() { bb = null; } };
        }
    }
    private native void allocate(Buffer b);

    /**
     * Called by native libraries to initialize the object fields.
     *
     * @param allocatedAddress the new address value of allocated native memory
     * @param allocatedCapacity the amount of elements allocated (initial limit and capacity)
     * @param deallocatorAddress the pointer to the native deallocation function
     * @see NativeDeallocator
     */
    void init(long allocatedAddress, int allocatedCapacity, long deallocatorAddress) {
        address = allocatedAddress;
        position = 0;
        limit = allocatedCapacity;
        capacity = allocatedCapacity;
        deallocator(new NativeDeallocator(this, deallocatorAddress));
    }

    /** The interface to implement to produce a Deallocator usable by Pointer. */
    protected interface Deallocator {
        void deallocate();
    }

    /**
     * A utility method to register easily a {@link CustomDeallocator} with a Pointer.
     *
     * @param p the Pointer with which to register the deallocator
     * @return the Pointer
     */
    protected static <P extends Pointer> P withDeallocator(P p) {
        return (P)p.deallocator(new CustomDeallocator(p));
    }

    /**
     * A {@link Deallocator} that calls, during garbage collection, a method with signature
     * {@code static void deallocate()} from the Pointer object passed to the constructor
     * and that accepts it as argument. Uses reflection to locate and call the method.
     *
     * @see #withDeallocator(Pointer)
     */
    protected static class CustomDeallocator extends DeallocatorReference implements Deallocator {
        public CustomDeallocator(Pointer p) {
            super(p, null);
            this.deallocator = this;
            Class<? extends Pointer> cls = p.getClass();
            for (Method m : cls.getDeclaredMethods()) {
                Class[] parameters = m.getParameterTypes();
                if (Modifier.isStatic(m.getModifiers()) && m.getReturnType().equals(void.class) &&
                        m.getName().equals("deallocate") && parameters.length == 1 &&
                        Pointer.class.isAssignableFrom(parameters[0])) {
                    m.setAccessible(true);
                    method = m;
                    break;
                }
            }
            if (method == null) {
                throw new RuntimeException(new NoSuchMethodException("static void " +
                        cls.getCanonicalName() + ".deallocate(" + Pointer.class.getCanonicalName() + ")"));
            }
            try {
                Constructor<? extends Pointer> c = cls.getConstructor(Pointer.class);
                c.setAccessible(true);
                pointer = c.newInstance(p);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        Pointer pointer = null;
        Method method = null;

        @Override public void deallocate() {
            try {
                method.invoke(null, pointer);
                pointer.setNull();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * A {@link Deallocator} that calls, during garbage collection, a native function.
     * Passes as argument the {@link #address} of the Pointer passed to the constructor.
     */
    protected static class NativeDeallocator extends DeallocatorReference implements Deallocator {
        NativeDeallocator(Pointer p, long deallocatorAddress) {
            super(p, null);
            this.deallocator = this;
            this.allocatedAddress = p.address;
            this.deallocatorAddress = deallocatorAddress;
        }

        private long allocatedAddress;
        private long deallocatorAddress;

        public void deallocate() {
            if (allocatedAddress != 0 && deallocatorAddress != 0) {
//                System.out.println("deallocating 0x" + Long.toHexString(allocatedAddress) +
//                        " 0x" + Long.toHexString(deallocatorAddress));
                deallocate(allocatedAddress, deallocatorAddress);
                allocatedAddress = deallocatorAddress = 0;
            }
        }

        private native void deallocate(long allocatedAddress, long deallocatorAddress);
    }

    /**
     * A subclass of {@link PhantomReference} that also acts as a linked
     * list to keep their references alive until they get garbage collected.
     */
    private static class DeallocatorReference extends PhantomReference<Pointer> {
        DeallocatorReference(Pointer p, Deallocator deallocator) {
            super(p, referenceQueue);
            this.deallocator = deallocator;
        }

        static DeallocatorReference head = null;
        DeallocatorReference prev = null, next = null;
        Deallocator deallocator;

        final void add() {
            synchronized (DeallocatorReference.class) {
                if (head == null) {
                    head = this;
                } else {
                    next = head;
                    next.prev = head = this;
                }
            }
        }

        final void remove() {
            synchronized (DeallocatorReference.class) {
                if (prev == this && next == this) {
                    return;
                }
                if (prev == null) {
                    head = next;
                } else {
                    prev.next = next;
                }
                if (next != null) {
                    next.prev = prev;
                }
                prev = next = this;
            }
        }

        @Override public void clear() {
            super.clear();
            if (deallocator != null) {
                deallocator.deallocate();
                deallocator = null;
            }
        }
    }

    /** The {@link ReferenceQueue} used by {@link DeallocatorReference}. */
    private static final ReferenceQueue<Pointer> referenceQueue = new ReferenceQueue<Pointer>();

    /** Clears, deallocates, and removes all garbage collected objects from the {@link #referenceQueue}. */
    public static void deallocateReferences() {
        DeallocatorReference r = null;
        while ((r = (DeallocatorReference)referenceQueue.poll()) != null) {
            r.clear();
            r.remove();
        }
    }

    /** The native address of this Pointer, which can be an array. */
    protected long address = 0;
    /** The index of the element of a native array that should be accessed. */
    protected int position = 0;
    /** The index of the first element that should not be accessed, or 0 if unknown. */
    protected int limit = 0;
    /** The number of elements contained in this native array, or 0 if unknown. */
    protected int capacity = 0;
    /** The deallocator associated with this Pointer that should be called on garbage collection. */
    private Deallocator deallocator = null;

    /** @return {@code address == 0} */
    public boolean isNull() {
        return address == 0;
    }
    /** Sets {@link #address} to 0. */
    public void setNull() {
        address = 0;
    }

    /** @return {@link #address} */
    public long address() {
        return address;
    }

    /** @return {@link #position} */
    public int position() {
        return position;
    }
    /**
     * Sets the position and returns this. That makes the {@code array.position(i)}
     * statement sort of equivalent to the {@code array[i]} statement in C++.
     *
     * @param position the new position
     * @return this
     */
    public <P extends Pointer> P position(int position) {
        this.position = position;
        return (P)this;
    }

    /** @return {@link #limit} */
    public int limit() {
        return limit;
    }
    /**
     * Sets the limit and returns this.
     * Used to limit the size of an operation on this object.
     *
     * @param limit the new limit
     * @return this
     */
    public <P extends Pointer> P limit(int limit) {
        this.limit = limit;
        return (P)this;
    }

    /** @return {@link #capacity} */
    public int capacity() {
        return capacity;
    }
    /**
     * Sets the capacity and returns this.
     * Should not be called more than once after allocation.
     *
     * @param capacity the new capacity
     * @return this
     */
    public <P extends Pointer> P capacity(int capacity) {
        this.limit = capacity;
        this.capacity = capacity;
        return (P)this;
    }

    /** @return {@link #deallocator} */
    protected Deallocator deallocator() {
        return deallocator;
    }
    /**
     * Sets the deallocator and returns this. Also clears current deallocator
     * if not {@code null}. That is, it deallocates previously allocated memory.
     * Should not be called more than once after allocation.
     *
     * @param deallocator the new deallocator
     * @return this
     */
    protected <P extends Pointer> P deallocator(Deallocator deallocator) {
        if (this.deallocator != null) {
            this.deallocator.deallocate();
            this.deallocator = null;
        }
        deallocateReferences();
        if (deallocator != null && !deallocator.equals(null)) {
            this.deallocator = deallocator;
            DeallocatorReference r = deallocator instanceof DeallocatorReference ?
                    (DeallocatorReference)deallocator :
                    new DeallocatorReference(this, deallocator);
            r.add();
        }
        return (P)this;
    }

    /** @see #deallocate(boolean) */
    public void deallocate() {
        deallocate(true);
    }
    /**
     * Explicitly deallocates native memory without waiting after the garbage collector.
     * Has no effect if no deallocator was previously set with {@link #deallocator(Deallocator)}.
     * @param deallocate if false, does not deallocate, rather disabling garbage collection
     */
    public void deallocate(boolean deallocate) {
        if (deallocate && deallocator != null) {
            deallocator.deallocate();
            address = 0;
        } else synchronized(DeallocatorReference.class) {
            DeallocatorReference r = DeallocatorReference.head;
            while (r != null) {
                if (r.deallocator == deallocator) {
                    r.deallocator = null;
                    r.clear();
                    r.remove();
                    break;
                }
                r = r.next;
            }
        }
    }

    /** @return {@code Loader.offsetof(getClass(), member)} or -1 on error */
    public int offsetof(String member) {
        int offset = -1;
        try {
            Class<? extends Pointer> c = getClass();
            if (c != Pointer.class) {
                offset = Loader.offsetof(c, member);
            }
        } catch (NullPointerException e) { }
        return offset;
    }

    /** @return 1 for Pointer or BytePointer else {@code Loader.sizeof(getClass())} or -1 on error */
    public int sizeof() {
        Class c = getClass();
        if (c == Pointer.class || c == BytePointer.class) {
            // default to 1 byte
            return 1;
        } else {
            return offsetof("sizeof");
        }
    }

    private native ByteBuffer asDirectBuffer();
    /**
     * Returns a ByteBuffer covering the memory space from this.position to this.limit.
     * If limit == 0, it uses position + 1 instead. The way the methods were designed
     * allows constructs such as {@code this.position(13).limit(42).asByteBuffer()}.
     *
     * @return the direct NIO {@link ByteBuffer} created
     */
    public ByteBuffer asByteBuffer() {
        if (isNull()) {
            return null;
        }
        if (limit > 0 && limit < position) {
            throw new IllegalArgumentException("limit < position: (" + limit + " < " + position + ")");
        }
        int valueSize = sizeof();
        int arrayPosition = position;
        int arrayLimit = limit;
        position = valueSize * arrayPosition;
        limit = valueSize * (arrayLimit <= 0 ? arrayPosition + 1 : arrayLimit);
        ByteBuffer b = asDirectBuffer().order(ByteOrder.nativeOrder());
        position = arrayPosition;
        limit = arrayLimit;
        return b;
    }
    /**
     * Same as {@link #asByteBuffer()}, but can be overridden to return subclasses of Buffer.
     *
     * @return {@code asByteBuffer()}
     * @see BytePointer#asBuffer()
     * @see ShortPointer#asBuffer()
     * @see IntPointer#asBuffer()
     * @see LongPointer#asBuffer()
     * @see FloatPointer#asBuffer()
     * @see DoublePointer#asBuffer()
     * @see CharPointer#asBuffer()
     */
    public Buffer asBuffer() {
        return asByteBuffer();
    }

    public static native Pointer memchr(Pointer p, int ch, long size);
    public static native int memcmp(Pointer p1, Pointer p2, long size);
    public static native Pointer memcpy(Pointer dst, Pointer src, long size);
    public static native Pointer memmove(Pointer dst, Pointer src, long size);
    public static native Pointer memset(Pointer dst, int ch, long size);
    /**
     * Calls in effect {@code memcpy(this.address + this.position, p.address + p.position, length)},
     * where {@code length = sizeof(p) * (p.limit - p.position)}.
     * If limit == 0, it uses position + 1 instead. The way the methods were designed
     * allows constructs such as {@code this.position(0).put(p.position(13).limit(42))}.
     *
     * @param p the Pointer from which to copy memory
     * @return this
     */
    public <P extends Pointer> P put(Pointer p) {
        if (p.limit > 0 && p.limit < p.position) {
            throw new IllegalArgumentException("limit < position: (" + p.limit + " < " + p.position + ")");
        }
        int size = sizeof();
        int psize = p.sizeof();
        int length = psize * (p.limit <= 0 ? 1 : p.limit - p.position);
        position *= size;
        p.position *= psize;
        memcpy(this, p, length);
        position /= size;
        p.position /= psize;
        return (P)this;
    }
    /**
     * Calls in effect {@code memset(address + position, b, length)},
     * where {@code length = sizeof() * (limit - position)}.
     * If limit == 0, it uses position + 1 instead. The way the methods were designed
     * allows constructs such as {@code this.position(0).limit(13).fill(42)};
     *
     * @param b the byte value to fill the memory with
     * @return this
     */
    public <P extends Pointer> P fill(int b) {
        if (limit > 0 && limit < position) {
            throw new IllegalArgumentException("limit < position: (" + limit + " < " + position + ")");
        }
        int size = sizeof();
        int length = size * (limit <= 0 ? 1 : limit - position);
        position *= size;
        memset(this, b, length);
        position /= size;
        return (P)this;
    }
    /** @return {@code fill(0)} */
    public <P extends Pointer> P zero() { return (P)fill(0); }

    /**
     * Checks for equality with argument. Defines obj to be equal if {@code
     *     (obj == null && this.address == 0) ||
     *     (obj.address == this.address && obj.position == this.position)}.
     *
     * @param obj the object to compare this Pointer to
     * @return true if obj is equal
     */
    @Override public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null) {
            return isNull();
        } else if (obj.getClass() != getClass()) {
            return false;
        } else {
            Pointer other = (Pointer)obj;
            return address == other.address && position == other.position;
        }
    }

    /** @return {@code (int)address} */
    @Override public int hashCode() {
        return (int)address;
    }

    /** @return a {@link String} representation of {@link #address},
     * {@link #position}, {@link #limit}, {@link #capacity}, and {@link #deallocator} */
    @Override public String toString() {
        return getClass().getName() + "[address=0x" + Long.toHexString(address) +
                ",position=" + position + ",limit=" + limit + ",capacity=" + capacity + ",deallocator=" + deallocator + "]";
    }
}
