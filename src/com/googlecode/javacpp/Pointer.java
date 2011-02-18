/*
 * Copyright (C) 2011 Samuel Audet
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

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import com.googlecode.javacpp.annotation.Opaque;

/**
 *
 * @author Samuel Audet
 */
@Opaque public class Pointer {
    public Pointer() {
        // Make sure out top enclosing class is initialized and the
        // associated native loaded. This code is actually quite efficient.
//        Class cls = getClass();
//        String className = cls.getName();
//        int topIndex = className.indexOf('$');
//        if (topIndex > 0 && Tools.loadedLibraries.
//                get(className.substring(0, topIndex)) == null) {
//            Tools.load(cls);
//        }
    }
    public Pointer(Pointer p) {
        if (p == null) {
            address = 0;
            position = 0;
        } else {
            address = p.address;
            position = p.position;
        }
    }

    void init(long allocatedAddress, long deallocatorAddress) {
        address = allocatedAddress;
        deallocator(new ReferenceDeallocator(this, allocatedAddress, deallocatorAddress));
    }

    protected interface Deallocator {
        void deallocate();
    }

    static class ReferenceDeallocator extends DeallocatorReference implements Deallocator {
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

    static class DeallocatorReference extends PhantomReference<Pointer> {
        DeallocatorReference(Pointer p, Deallocator deallocator) {
            super(p, referenceQueue);
            this.deallocator = deallocator;
        }

        static DeallocatorReference head = null;
        DeallocatorReference prev = null, next = null;
        Deallocator deallocator;

        synchronized final void add() {
            if (head == null) {
                head = this;
            } else {
                next = head;
                next.prev = head = this;
            }
        }

        synchronized final void remove() {
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

        @Override public void clear() {
            super.clear();
            deallocator.deallocate();
        }
    }

    static final ReferenceQueue<Pointer> referenceQueue = new ReferenceQueue<Pointer>();

    protected long address;
    protected int position;
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

    protected Deallocator deallocator() {
        return deallocator;
    }
    protected Pointer deallocator(Deallocator deallocator) {
        this.deallocator = deallocator;

        DeallocatorReference r = null;
        while ((r = (DeallocatorReference)referenceQueue.poll()) != null) {
            r.clear();
            r.remove();
        }

        if (deallocator instanceof DeallocatorReference) {
            r = (DeallocatorReference)deallocator;
        } else {
            r = new DeallocatorReference(this, deallocator);
        }
        r.add();
        return this;
    }

    public void deallocate() {
        deallocator.deallocate();
        address = 0;
    }

    private native ByteBuffer asDirectBuffer(int capacity);
    public ByteBuffer asByteBuffer(int capacity) {
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
        position = valueSize*arrayPosition;
        ByteBuffer b = asDirectBuffer(valueSize*capacity).order(ByteOrder.nativeOrder());
        position = arrayPosition;
        return b;
    }
    public Buffer asBuffer(int capacity) {
        return asByteBuffer(capacity);
    }

    @Override public boolean equals(Object obj) {
        if (obj == null && isNull()) {
            return true;
        } else if (!(obj instanceof Pointer)) {
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
                ",position=" + position + ",deallocator=" + deallocator + "]";
    }
}
