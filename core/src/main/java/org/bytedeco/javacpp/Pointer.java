/*
 * Copyright (C) 2011-2018 Samuel Audet
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

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.bytedeco.javacpp.annotation.Name;
import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.tools.Generator;
import org.bytedeco.javacpp.tools.Logger;

/**
 * All peer classes to native types must be descended from Pointer, the topmost class.
 * It can be thought as mapping the native C++ {@code void*}, which can point to any
 * {@code struct}, {@code class}, or {@code union}. All Pointer classes get parsed
 * by {@link Generator} to produce proper wrapping JNI code, but this base class also
 * provides functionality to access native array elements as well as utility methods
 * and classes to let users benefit not only from from garbage collection, but also the
 * try-with-resources statement, since it implements the {@link AutoCloseable} interface.
 * <p>
 * It is also possible to use a {@link PointerScope} to keep track of a group of Pointer objects,
 * and have them deallocated in a transparent but deterministic manner.
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
@Platform
public class Pointer implements AutoCloseable {
    /** Default constructor that does nothing. */
    public Pointer() {}
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
            address -= b.position() * sizeof();
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
    void init(long allocatedAddress, long allocatedCapacity, long ownerAddress, long deallocatorAddress) {
        address = allocatedAddress;
        position = 0;
        limit = allocatedCapacity;
        capacity = allocatedCapacity;
        if (ownerAddress != 0 && deallocatorAddress != 0) {
            deallocator(new NativeDeallocator(this, ownerAddress, deallocatorAddress));
        }
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

        @Override public String toString() {
            return getClass().getName() + "[pointer=" + pointer + ",method=" + method + "]";
        }
    }

    /**
     * A {@link Deallocator} that calls, during garbage collection, a native function.
     * Passes as single argument the {@link #ownerAddress} passed to the constructor.
     */
    protected static class NativeDeallocator extends DeallocatorReference implements Deallocator {
        NativeDeallocator(Pointer p, long ownerAddress, long deallocatorAddress) {
            super(p, null);
            this.deallocator = this;
            this.ownerAddress = ownerAddress;
            this.deallocatorAddress = deallocatorAddress;
        }

        private long ownerAddress;
        private long deallocatorAddress;

        @Override public void deallocate() {
            if (ownerAddress != 0 && deallocatorAddress != 0) {
                deallocate(ownerAddress, deallocatorAddress);
                ownerAddress = deallocatorAddress = 0;
            }
        }

        private native void deallocate(long ownerAddress, long deallocatorAddress);

        @Override public String toString() {
            return getClass().getName() + "[ownerAddress=0x" + Long.toHexString(ownerAddress)
                    + ",deallocatorAddress=0x" + Long.toHexString(deallocatorAddress) + "]";
        }
    }

    /**
     * A subclass of {@link PhantomReference} that also acts as a linked
     * list to keep their references alive until they get garbage collected.
     * Also keeps track of total allocated memory in bytes, to have it
     * call {@link System#gc()} when that amount reaches {@link #maxBytes}.
     */
    static class DeallocatorReference extends PhantomReference<Pointer> {
        DeallocatorReference(Pointer p, Deallocator deallocator) {
            super(p, referenceQueue);
            this.deallocator = deallocator;
            this.bytes = p.capacity * p.sizeof();
        }

        static volatile DeallocatorReference head = null;
        volatile DeallocatorReference prev = null, next = null;
        Deallocator deallocator;

        static volatile long totalBytes = 0;
        long bytes;

        final void add() {
            synchronized (DeallocatorReference.class) {
                if (head == null) {
                    head = this;
                } else {
                    next = head;
                    next.prev = head = this;
                }
                totalBytes += bytes;
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
                totalBytes -= bytes;
            }
        }

        @Override public void clear() {
            super.clear();
            if (deallocator != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Collecting " + this);
                }
                deallocator.deallocate();
                deallocator = null;
            }
        }

        @Override public String toString() {
            return getClass().getName() + "[deallocator=" + deallocator + "]";
        }
    }

    static class DeallocatorThread extends Thread {
        DeallocatorThread() {
            super("JavaCPP Deallocator");
            setPriority(Thread.MAX_PRIORITY);
            setDaemon(true);
            start();
        }

        @Override public void run() {
            try {
                while (true) {
                    DeallocatorReference r = (DeallocatorReference)referenceQueue.remove();
                    r.clear();
                    r.remove();
                }
            } catch (InterruptedException ex) {
                // reset interrupt to be nice
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final Logger logger = Logger.create(Pointer.class);

    /** The {@link ReferenceQueue} used by {@link DeallocatorReference}.
     * Initialized to null if the "org.bytedeco.javacpp.nopointergc" system property is "true". */
    private static final ReferenceQueue<Pointer> referenceQueue;

    static final Thread deallocatorThread;

    /** Maximum amount of memory registered with live deallocators before forcing call to {@link System#gc()}.
     * Set via "org.bytedeco.javacpp.maxbytes" system property, defaults to {@link Runtime#maxMemory()}. */
    static final long maxBytes;

    /** Maximum amount of memory reported by {@link #physicalBytes()} before forcing call to {@link System#gc()}.
     * Set via "org.bytedeco.javacpp.maxphysicalbytes" system property, defaults to {@code 2 * Runtime.maxMemory()}. */
    static final long maxPhysicalBytes;

    /** Maximum number of times to call {@link System#gc()} before giving up with {@link OutOfMemoryError}.
     * Set via "org.bytedeco.javacpp.maxretries" system property, defaults to 10, where each retry is followed
     * by a call to {@code Thread.sleep(100)} and {@code Pointer.trimMemory()}. */
    static final int maxRetries;

    public static String formatBytes(long bytes) {
        if (bytes < 1024L * 100) {
            return bytes + "";
        } else if ((bytes /= 1024L) < 1024L * 100) {
            return bytes + "K";
        } else if ((bytes /= 1024L) < 1024L * 100) {
            return bytes + "M";
        } else if ((bytes /= 1024L) < 1024L * 100) {
            return bytes + "G";
        } else {
            return (bytes / 1024L) + "T";
        }
    }

    public static long parseBytes(String string) throws NumberFormatException {
        int i = 0;
        while (i < string.length()) {
            if (!Character.isDigit(string.charAt(i))) {
                break;
            }
            i++;
        }
        long size = Long.parseLong(string.substring(0, i));
        switch (string.substring(i).trim().toLowerCase()) {
            case "t": case "tb": size *= 1024L; /* no break */
            case "g": case "gb": size *= 1024L; /* no break */
            case "m": case "mb": size *= 1024L; /* no break */
            case "k": case "kb": size *= 1024L; /* no break */
            case "": break;
            default: throw new NumberFormatException("Cannot parse into bytes: " + string);
        }
        return size;
    }

    static {
        String s = System.getProperty("org.bytedeco.javacpp.nopointergc", "false").toLowerCase();
        s = System.getProperty("org.bytedeco.javacpp.noPointerGC", s).toLowerCase();
        if (s.equals("true") || s.equals("t") || s.equals("")) {
            referenceQueue = null;
            deallocatorThread = null;
        } else {
            referenceQueue =  new ReferenceQueue<Pointer>();
            deallocatorThread = new DeallocatorThread();
        }

        long m = Runtime.getRuntime().maxMemory();
        s = System.getProperty("org.bytedeco.javacpp.maxbytes");
        s = System.getProperty("org.bytedeco.javacpp.maxBytes", s);
        if (s != null && s.length() > 0) {
            try {
                m = parseBytes(s);
            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            }
        }
        maxBytes = m;

        m = 2 * Runtime.getRuntime().maxMemory();
        s = System.getProperty("org.bytedeco.javacpp.maxphysicalbytes");
        s = System.getProperty("org.bytedeco.javacpp.maxPhysicalBytes", s);
        if (s != null && s.length() > 0) {
            try {
                m = parseBytes(s);
            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            }
        }
        maxPhysicalBytes = m;

        int n = 10;
        s = System.getProperty("org.bytedeco.javacpp.maxretries");
        s = System.getProperty("org.bytedeco.javacpp.maxRetries", s);
        if (s != null && s.length() > 0) {
            try {
                n = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            }
        }
        maxRetries = n;
    }

    /** Clears, deallocates, and removes all garbage collected objects from the {@link #referenceQueue}. */
    public static void deallocateReferences() {
        DeallocatorReference r;
        while (referenceQueue != null && (r = (DeallocatorReference)referenceQueue.poll()) != null) {
            r.clear();
            r.remove();
        }
    }

    /** Returns {@link #maxBytes}, the maximum amount of memory allowed to be tracked. */
    public static long maxBytes() {
        return maxBytes;
    }

    /** Returns {@link DeallocatorReference#totalBytes}, current amount of memory tracked by deallocators. */
    public static long totalBytes() {
        return DeallocatorReference.totalBytes;
    }

    /** Returns {@link #maxPhysicalBytes}, the maximum amount of physical memory that should be used. */
    public static long maxPhysicalBytes() {
        return maxPhysicalBytes;
    }

    /** Makes sure to return freed memory to the system, as required by Linux, at least. */
    @Name("JavaCPP_trimMemory") private static native boolean trimMemory();

    /** Returns the amount of physical memory currently used by the whole process, or 0 if unknown.
     * Also known as "resident set size" (Linux, Mac OS X, etc) or "working set size" (Windows). */
    @Name("JavaCPP_physicalBytes") public static native long physicalBytes();

    /** Returns the amount of physical memory installed according to the operating system, or 0 if unknown.
     * It should not be possible for {@link #physicalBytes()} to go over this value. */
    @Name("JavaCPP_totalPhysicalBytes") public static native long totalPhysicalBytes();

    /** Returns the amount of physical memory that is free according to the operating system, or 0 if unknown. */
    @Name("JavaCPP_availablePhysicalBytes") public static native long availablePhysicalBytes();

    /** The native address of this Pointer, which can be an array. */
    protected long address = 0;
    /** The index of the element of a native array that should be accessed. */
    protected long position = 0;
    /** The index of the first element that should not be accessed, or 0 if unknown. */
    protected long limit = 0;
    /** The number of elements contained in this native array, or 0 if unknown. */
    protected long capacity = 0;
    /** The deallocator associated with this Pointer that should be called on garbage collection. */
    private Deallocator deallocator = null;

    /** Returns {@code address == 0}. */
    public boolean isNull() {
        return address == 0;
    }
    /** Sets {@link #address} to 0. */
    public void setNull() {
        address = 0;
    }

    /** Returns {@link #address}. */
    public long address() {
        return address;
    }

    /** Returns {@link #position}. */
    public long position() {
        return position;
    }
    /**
     * Sets the position and returns this. That makes the {@code array.position(i)}
     * statement sort of equivalent to the {@code array[i]} statement in C++.
     *
     * @param position the new position
     * @return this
     */
    public <P extends Pointer> P position(long position) {
        this.position = position;
        return (P)this;
    }

    /** Returns {@link #limit}. */
    public long limit() {
        return limit;
    }
    /**
     * Sets the limit and returns this.
     * Used to limit the size of an operation on this object.
     *
     * @param limit the new limit
     * @return this
     */
    public <P extends Pointer> P limit(long limit) {
        this.limit = limit;
        return (P)this;
    }

    /** Returns {@link #capacity}. */
    public long capacity() {
        return capacity;
    }
    /**
     * Sets the capacity and returns this.
     * Should not be called more than once after allocation.
     *
     * @param capacity the new capacity
     * @return this
     */
    public <P extends Pointer> P capacity(long capacity) {
        this.limit = capacity;
        this.capacity = capacity;
        return (P)this;
    }

    /** Returns {@link #deallocator}. */
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
            if (logger.isDebugEnabled()) {
                logger.debug("Predeallocating " + this);
            }
            this.deallocator.deallocate();
            this.deallocator = null;
        }
        if (deallocator != null && !deallocator.equals(null)) {
            this.deallocator = deallocator;
            DeallocatorReference r = deallocator instanceof DeallocatorReference ?
                    (DeallocatorReference)deallocator :
                    new DeallocatorReference(this, deallocator);
            int count = 0;
            long lastPhysicalBytes = maxPhysicalBytes > 0 ? physicalBytes() : 0;
            synchronized (DeallocatorThread.class) {
                try {
                    while (count++ < maxRetries && ((maxBytes > 0 && DeallocatorReference.totalBytes + r.bytes > maxBytes)
                                         || (maxPhysicalBytes > 0 && lastPhysicalBytes > maxPhysicalBytes))) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Calling System.gc() and Pointer.trimMemory() in " + this);
                        }
                        // try to get some more memory back
                        System.gc();
                        Thread.sleep(100);
                        trimMemory();
                        lastPhysicalBytes = maxPhysicalBytes > 0 ? physicalBytes() : 0;
                    }
                } catch (InterruptedException ex) {
                    // reset interrupt to be nice
                    Thread.currentThread().interrupt();
                } catch (UnsatisfiedLinkError e) {
                    // old binaries with physicalBytes() or trimMemory() missing -> ignore for backward compatibility
                    if (logger.isDebugEnabled()) {
                        logger.debug(e.getMessage());
                    }
                }
                if (maxBytes > 0 && DeallocatorReference.totalBytes + r.bytes > maxBytes) {
                    deallocate();
                    throw new OutOfMemoryError("Failed to allocate memory within limits: totalBytes ("
                            + formatBytes(DeallocatorReference.totalBytes) + " + " + formatBytes(r.bytes) + ") > maxBytes (" + formatBytes(maxBytes) + ")");
                } else if (maxPhysicalBytes > 0 && lastPhysicalBytes > maxPhysicalBytes) {
                    deallocate();
                    throw new OutOfMemoryError("Physical memory usage is too high: physicalBytes ("
                            + formatBytes(lastPhysicalBytes) + ") > maxPhysicalBytes (" + formatBytes(maxPhysicalBytes) + ")");
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Registering " + this);
                }
                r.add();

                PointerScope s = PointerScope.getInnerScope();
                if (s != null) {
                    s.attach(this);
                }
            }
        }
        return (P)this;
    }

    /** Calls {@code deallocate()}. */
    @Override public void close() {
        deallocate();
    }

    /** Calls {@code deallocate(true)}. */
    public void deallocate() {
        deallocate(true);
    }
    /**
     * Explicitly manages native memory without waiting after the garbage collector.
     * Has no effect if no deallocator was previously set with {@link #deallocator(Deallocator)}.
     * @param deallocate if true, deallocates, else does not, but disables garbage collection
     */
    public void deallocate(boolean deallocate) {
        if (deallocate && deallocator != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Deallocating " + this);
            }
            deallocator.deallocate();
            address = 0;
        }
        if (!deallocate || referenceQueue == null) {
            synchronized (DeallocatorReference.class) {
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
    }

    /** Returns {@code Loader.offsetof(getClass(), member)} or -1 on error. */
    public int offsetof(String member) {
        int offset = -1;
        try {
            Class<? extends Pointer> c = getClass();
            if (c != Pointer.class) {
                offset = Loader.offsetof(c, member);
            }
        } catch (ClassCastException | NullPointerException e) {
            return offset;
        }
        return offset;
    }

    /** Returns 1 for Pointer or BytePointer else {@code Loader.sizeof(getClass())} or -1 on error. */
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
        int size = sizeof();
        Pointer p = new Pointer();
        p.address = address;
        return p.position(size * position)
                .capacity(size * (limit <= 0 ? position + 1 : limit))
                .asDirectBuffer().order(ByteOrder.nativeOrder());
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

    public static native Pointer malloc(long size);
    public static native Pointer calloc(long n, long size);
    public static native Pointer realloc(Pointer p, long size);
    public static native void free(Pointer p);

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
        long length = psize * (p.limit <= 0 ? 1 : p.limit - p.position);
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
        long length = size * (limit <= 0 ? 1 : limit - position);
        position *= size;
        memset(this, b, length);
        position /= size;
        return (P)this;
    }
    /** Returns {@code fill(0)}. */
    public <P extends Pointer> P zero() { return (P)fill(0); }

    /**
     * Checks for equality with argument. Defines obj to be equal if {@code
     *     (obj == null && this.address == 0) ||
     *     (obj.address == this.address && obj.position == this.position)},
     * and the classes are the same, unless one of them in Pointer itself.
     *
     * @param obj the object to compare this Pointer to
     * @return true if obj is equal
     */
    @Override public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null) {
            return isNull();
        } else if (obj.getClass() != getClass()
                && obj.getClass() != Pointer.class
                && getClass() != Pointer.class) {
            return false;
        } else {
            Pointer other = (Pointer)obj;
            return address == other.address && position == other.position;
        }
    }

    /** Returns {@code (int)address}. */
    @Override public int hashCode() {
        return (int)address;
    }

    /** Returns a {@link String} representation of {@link #address},
     * {@link #position}, {@link #limit}, {@link #capacity}, and {@link #deallocator}. */
    @Override public String toString() {
        return getClass().getName() + "[address=0x" + Long.toHexString(address) +
                ",position=" + position + ",limit=" + limit + ",capacity=" + capacity + ",deallocator=" + deallocator + "]";
    }
}
