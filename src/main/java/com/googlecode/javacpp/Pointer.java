/*
 * Copyright (C) 2011,2012 Samuel Audet
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

package com.googlecode.javacpp;

import com.googlecode.javacpp.annotation.Opaque;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author Samuel Audet
 */
@Opaque public class Pointer {
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
    public Pointer(Pointer p) {
        if (p == null) {
            address = 0;
            position = 0;
            capacity = 0;
        } else {
            address = p.address;
            position = p.position;
            capacity = p.capacity;
        }
    }

    private static Field bufferAddressField = null;
    public Pointer(Buffer b) {
        if (b == null || b.hasArray()) {
            address = 0;
            position = 0;
            capacity = 0;
        } else {
            try {
                if (bufferAddressField == null) {
                    try {
                        bufferAddressField = Buffer.class.getDeclaredField("address");
                    } catch (NoSuchFieldException e) {
                        bufferAddressField = Buffer.class.getDeclaredField("effectiveDirectAddress");
                    }
                    bufferAddressField.setAccessible(true);
                }
                address = bufferAddressField.getType() == long.class ? 
                          bufferAddressField.getLong(b) : bufferAddressField.getInt(b);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            position = b.position();
            capacity = b.capacity();
        }
    }

    void init(long allocatedAddress, int allocatedCapacity, long deallocatorAddress) {
        address = allocatedAddress;
        capacity = allocatedCapacity;
        deallocator(new ReferenceDeallocator(this, allocatedAddress, deallocatorAddress));
    }

    protected interface Deallocator {
        void deallocate();
    }

    private static class ReferenceDeallocator extends DeallocatorReference implements Deallocator {
        ReferenceDeallocator(Pointer p, long allocatedAddress, long deallocatorAddress) {
            super(p, null);
            this.deallocator = this;
            this.allocatedAddress = allocatedAddress;
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
            deallocator.deallocate();
        }
    }

    private static final ReferenceQueue<Pointer> referenceQueue = new ReferenceQueue<Pointer>();

    public static void deallocateReferences() {
        DeallocatorReference r = null;
        while ((r = (DeallocatorReference)referenceQueue.poll()) != null) {
            r.clear();
            r.remove();
        }
    }

    protected long address;
    protected int position, capacity;
    private Deallocator deallocator;

    public boolean isNull() {
        return address == 0;
    }
    public void setNull() {
        address = 0;
    }

    public int position() {
        return position;
    }
    public Pointer position(int position) {
        this.position = position;
        return this;
    }

    public int capacity() {
        return capacity;
    }
    public Pointer capacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    protected Deallocator deallocator() {
        return deallocator;
    }
    protected Pointer deallocator(Deallocator deallocator) {
        if (this.deallocator() != null) {
            this.deallocator().deallocate();
        }
        this.deallocator = deallocator;
        deallocateReferences();

        DeallocatorReference r = deallocator instanceof DeallocatorReference ?
                (DeallocatorReference)deallocator :
                new DeallocatorReference(this, deallocator);
        r.add();
        return this;
    }

    public void deallocate() {
        deallocator.deallocate();
        address = 0;
    }

    private native ByteBuffer asDirectBuffer();
    public ByteBuffer asByteBuffer() {
        if (isNull()) {
            return null;
        }
        int valueSize = 1;
        try {
            Class<? extends Pointer> c = getClass();
            if (c != Pointer.class) {
                valueSize = Loader.sizeof(c);
            }
        } catch(NullPointerException e) { /* default to 1 byte */ }

        int arrayPosition = position;
        int arrayCapacity = capacity;
        position = valueSize*arrayPosition;
        capacity = valueSize*arrayCapacity;
        ByteBuffer b = asDirectBuffer().order(ByteOrder.nativeOrder());
        position = arrayPosition;
        capacity = arrayCapacity;
        return b;
    }
    public Buffer asBuffer() {
        return asByteBuffer();
    }

    @Override public boolean equals(Object obj) {
        if (obj == null && isNull()) {
            return true;
        } else if (obj.getClass() != getClass()) {
            return false;
        } else {
            Pointer other = (Pointer)obj;
            return address == other.address && position == other.position;
        }
    }

    @Override public int hashCode() {
        return (int)address;
    }

    @Override public String toString() {
        return getClass().getName() + "[address=0x" + Long.toHexString(address) +
                ",position=" + position + ",capacity=" + capacity + ",deallocator=" + deallocator + "]";
    }
}
