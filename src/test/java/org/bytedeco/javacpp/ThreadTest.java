package org.bytedeco.javacpp;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.annotation.Virtual;
import org.bytedeco.javacpp.tools.Builder;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test case verifies that threads attached from the native layer are
 * configured as daemons and can remain attached when the appropriate property
 * is specified.
 * <p>
 * You would want this behavior when you have a callback that is called with a
 * high frequency.
 *
 * @author Dan Avila
 *
 */
@Platform(compiler = "cpp11", include = "ThreadTest.h")
public class ThreadTest {
    public static class Callback extends Pointer {
        /** Default native constructor. */
        public Callback() { super((Pointer) null); allocate(); }
        /** Native array allocator. Access with {@link Pointer#position(long)}. */
        public Callback(long size){ super((Pointer) null); allocateArray(size); }
        /** Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}. */
        public Callback(Pointer p) { super(p); }

        private native void allocate();
        private native void allocateArray(long size);

        @Override public Callback position(long position) {
            return (Callback)super.position(position);
        }

        @Override public Callback getPointer(long i) {
            return new Callback((Pointer)this).offsetAddress(i);
        }

        @Virtual(true) public native void callback(int value);
    }

    static native void run(Callback callback, int count);

    @BeforeClass public static void setUpClass() throws Exception {
        System.out.println("Builder");
        Class c = ThreadTest.class;
        Builder builder = new Builder().classesOrPackages(c.getName());
        File[] outputFiles = builder.build();

        System.out.println("Loader");
        Loader.load(c);
    }

    @Test public void testJNIThreadAttachFromNativeCallbacks() {
        final int count = 10;
        final List<Integer> callbackValueRefs = new ArrayList<>();
        final List<Thread> threadRefs = new ArrayList<>();

        Callback callback = new Callback() {
            @Override public void callback(int value) {
                System.out.println("Callback from " + Thread.currentThread());
                callbackValueRefs.add(Integer.valueOf(value));
                threadRefs.add(Thread.currentThread());
            }
        };

        run(callback, count);

        assertEquals(callbackValueRefs.size(), count);
        assertEquals(threadRefs.size(), count);

        for (int i = 0; i < count; i++) {
            int value = callbackValueRefs.get(i);
            Thread callbackThread = threadRefs.get(i);

            assertEquals("Callback was not called", i + 1, value);

            assertNotEquals("Callback thread is not main", Thread.currentThread(), callbackThread);
            assertTrue("Callback thread is daemon", callbackThread.isDaemon());
        }

        for (int i = 1; i < count; i++) {
            Thread cbThread1 = threadRefs.get(i - 1);
            Thread cbThread2 = threadRefs.get(i);

            assertEquals(cbThread1, cbThread2);
        }
        
        // thread should be automatically detached upon completion
        assertFalse(threadRefs.get(0).isAlive());
    }
}
