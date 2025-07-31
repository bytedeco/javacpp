/*
 * Copyright (C) 2025 Park Jeonghwan
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

import org.bytedeco.javacpp.Pointer;

import java.lang.ref.PhantomReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks native memory allocation sites and provides usage statistics for debugging memory lifecycle.
 * Captures the source code location where Pointer objects are created and monitors allocation,
 * deallocation, and garbage collection events for each site.
 */
public class NativeAllocationTracer {
    /**
     * Represents a specific location in source code identified by class, method, file, and line number.
     * Used to track where Pointer objects are created for memory allocation analysis.
     */
    public static class Location {
        /** Class name where the allocation occurred */
        private final String className;
        /** Method name where the allocation occurred */
        private final String methodName;
        /** File name where the allocation occurred */
        private final String fileName;
        /** Line number where the allocation occurred */
        private final int lineNumber;

        Location(StackTraceElement element) {
            this.className = element.getClassName();
            this.methodName = element.getMethodName();
            this.fileName = element.getFileName();
            this.lineNumber = element.getLineNumber();
        }

        /**
         * @return the name of the class where the allocation occurred
         */
        public String getClassName() {
            return className;
        }

        /**
         * @return the name of the method where the allocation occurred
         */
        public String getMethodName() {
            return methodName;
        }

        /**
         * @return the name of the file where the allocation occurred
         */
        public String getFileName() {
            return fileName;
        }

        /**
         * @return the number of the line where the allocation occurred
         */
        public int getLineNumber() {
            return lineNumber;
        }

        /**
         * @return true if the method is a native method, false otherwise
         */
        public boolean isNativeMethod() {
            return lineNumber == -2;
        }

        @Override
        public String toString() {
            String physicalLocation;

            if (fileName != null && lineNumber >= 0) {
                physicalLocation = fileName + ":" + lineNumber;
            } else if (isNativeMethod()) {
                physicalLocation = "Native Method";
            } else {
                physicalLocation = "Unknown Source";
            }

            return className + "." + methodName + "(" + physicalLocation + ")";
        }

        @Override
        public boolean equals(Object object) {
            if (object == null || getClass() != object.getClass()) return false;
            Location location = (Location) object;
            return lineNumber == location.lineNumber &&
                    Objects.equals(className, location.className) &&
                    Objects.equals(methodName, location.methodName) &&
                    Objects.equals(fileName, location.fileName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(className, methodName, fileName, lineNumber);
        }
    }

    /**
     * Contains allocation statistics for a specific source code location.
     * Tracks total allocations, currently live objects, and garbage collected objects
     * with both count and byte metrics using thread-safe atomic operations.
     */
    public static final class Site {
        /** Source code location where allocations occur */
        private final Location location;

        /** Total number of allocated at this location */
        private final AtomicLong totalCounts;
        /** Total bytes allocated at this location */
        private final AtomicLong totalBytes;
        /** Current number of live allocations */
        private final AtomicLong liveCounts;
        /** Current bytes of live memory */
        private final AtomicLong liveBytes;
        /** Number of allocations that have been garbage collected (non-manually deallocated) */
        private final AtomicLong collectedCounts;
        /** Bytes of memory that have been garbage collected (non-manually deallocated) */
        private final AtomicLong collectedBytes;

        Site(Location location) {
            this.location = location;

            this.totalCounts = new AtomicLong(0);
            this.totalBytes = new AtomicLong(0);
            this.liveCounts = new AtomicLong(0);
            this.liveBytes = new AtomicLong(0);
            this.collectedCounts = new AtomicLong(0);
            this.collectedBytes = new AtomicLong(0);
        }

        /**
         * @return the source code {@code Location} location where allocations occur
         */
        public Location getLocation() {
            return location;
        }

        /**
         * @return the total number of allocated at this location
         */
        public long getTotalCounts() {
            return totalCounts.get();
        }

        /**
         * @return the total bytes allocated at this location across all operations
         */
        public long getTotalBytes() {
            return totalBytes.get();
        }

        /**
         * @return the total bytes allocated at this location
         */
        public long getLiveCounts() {
            return liveCounts.get();
        }

        /**
         * @return the current number of live allocations
         */
        public long getLiveBytes() {
            return liveBytes.get();
        }

        /**
         * @return the number of allocations that have been garbage collected (non-manually deallocated)
         */
        public long getCollectedCounts() {
            return collectedCounts.get();
        }

        /**
         * @return the bytes of memory that have been garbage collected (non-manually deallocated)
         */
        public long getCollectedBytes() {
            return collectedBytes.get();
        }

        @Override
        public boolean equals(Object object) {
            if (object == null || getClass() != object.getClass()) return false;
            Site site = (Site) object;
            return location.equals(site.location);
        }

        @Override
        public int hashCode() {
            return location.hashCode();
        }

        @Override
        public String toString() {
            return "Site[location=" + location.toString() +
                    ",totalCounts=" + totalCounts.get() + ",totalBytes=" + totalBytes.get() +
                    ",liveCounts=" + liveCounts.get() + ",liveBytes=" + liveBytes.get() +
                    ",collectedCounts=" + collectedCounts.get() + ",collectedBytes=" + collectedBytes.get() + "]";
        }
    }

    private static final Logger logger = Logger.create(NativeAllocationTracer.class);

    /** Maps source code locations to their allocation statistics */
    private static final HashMap<Location, Site> sites = new HashMap<>();
    /** Maps for remember where each Pointer object was created */
    private static final WeakHashMap<Pointer, Location> pointerLocations = new WeakHashMap<>();
    /** Maps phantom references to allocation sites for GC-time tracking */
    private static final WeakHashMap<PhantomReference<Pointer>, Location> pointerReferenceLocations = new WeakHashMap<>();

    /**
     * Retrieves a collection of all currently tracked allocation sites.
     *
     * @return a collection of {@code Site} objects representing the allocation statistics
     */
    public static Collection<Site> getSites() {
        return sites.values();
    }

    /**
     * Associates a Pointer with its creation location for future tracking
     *
     * @param pointer the Pointer object to be marked and tracked
     */
    private static void markPointer(Pointer pointer) {
        Location location = captureCreationLocation(pointer.getClass());

        if (location == null) {
            if (logger.isWarnEnabled()) {
                logger.warn("Could not capture creation location for " + pointer);
            }

            return;
        }

        synchronized (NativeAllocationTracer.class) {
            if (!sites.containsKey(location)) {
                Site site = new Site(location);
                sites.put(location, site);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Mark location for " + pointer + ": " + location);
            }

            pointerLocations.put(pointer, location);
        }
    }

    /**
     * Associates a Pointer Reference with its allocation location for tracking during GC.
     * First attempts to retrieve the location from the associated Pointer, then falls back
     * to capturing the current stack frame location if not found.
     *
     * @param pointerReference the phantom reference to be marked and tracked
     * @param pointer the Pointer object associated with the phantom reference
     */
    private static void markReference(PhantomReference<Pointer> pointerReference, Pointer pointer) {
        Location location = pointerLocations.get(pointer);

        if (location == null) {
            location = captureCreationLocation(pointer.getClass());

            if (location == null) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Could not get creation location for " + pointerReference);
                }
            }
        }

        synchronized (NativeAllocationTracer.class) {
            if (!sites.containsKey(location)) {
                Site site = new Site(location);
                sites.put(location, site);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Mark location for " + pointerReference + ": " + location);
            }

            pointerReferenceLocations.put(pointerReference, location);
        }
    }

    /**
     * Records when native memory is allocated, updating total and live statistics.
     *
     * @param pointerReference the phantom reference associated with the allocation
     * @param size the number of bytes allocated
     */
    private static void recordAllocation(PhantomReference<Pointer> pointerReference, long size) {
        Location location = pointerReferenceLocations.get(pointerReference);
        Site site = sites.get(location);

        if (site == null) {
            if (logger.isWarnEnabled()) {
                logger.warn("Could not find allocation site for " + pointerReference + ": " + location);
            }
            return;
        }

        site.totalCounts.incrementAndGet();
        site.totalBytes.addAndGet(size);
        site.liveCounts.incrementAndGet();
        site.liveBytes.addAndGet(size);
    }

    /**
     * Records when native memory is manually deallocated, updating live statistics.
     *
     * @param pointerReference the phantom reference associated with the deallocation
     * @param size the number of bytes deallocated
     */
    private static void recordDeallocation(PhantomReference<Pointer> pointerReference, long size) {
        Location location = pointerReferenceLocations.get(pointerReference);
        Site site = sites.get(location);

        if (site == null) {
            if (logger.isWarnEnabled()) {
                logger.warn("Could not find allocation site for " + pointerReference + ": " + location);
            }
            return;
        }

        site.liveCounts.decrementAndGet();
        site.liveBytes.addAndGet(-size);
    }

    /**
     * Records when native memory is garbage collected, updating collection statistics.
     *
     * @param pointerReference the phantom reference associated with the garbage collected memory
     * @param size the number of bytes garbage collected
     */
    private static void recordCollection(PhantomReference<Pointer> pointerReference, long size) {
        Location location = pointerReferenceLocations.get(pointerReference);
        Site site = sites.get(location);

        if (site == null) {
            if (logger.isWarnEnabled()) {
                logger.warn("Could not find allocation site for " + pointerReference + ": " + location);
            }
            return;
        }

        site.collectedCounts.incrementAndGet();
        site.collectedBytes.addAndGet(size);
    }

    /**
     * Captures the location in the source code where an instance creation occurs.
     * The method extracts the stack trace information, skipping frames related to
     * Pointer class constructors and internal tracer calls to identify the actual
     * call site where the object instantiation was initiated.
     *
     * @param createdClass the class of the object being created
     * @return the Location object containing class, method, file and line information,
     *         or null if the location could not be determined
     */
    private static Location captureCreationLocation(Class<?> createdClass) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (int i = 2; i < stackTrace.length - 1; i++) {
            StackTraceElement element = stackTrace[i];
            String elementMethod = element.getMethodName();
            Class<?> elementClass = null;

            try {
                elementClass = Class.forName(element.getClassName());
            } catch (ClassNotFoundException ignored) {
                continue;
            }

            if (elementMethod.equals("<init>") || elementMethod.equals("init")) {
                if (elementClass == createdClass) {
                    return new Location(stackTrace[i + 1]);
                }
            }
        }

        return null;
    }
}
