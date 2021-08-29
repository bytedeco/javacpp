/*
 * Copyright (C) 2021-2021 Hannes Wellmann
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

import java.net.URL;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTracker;

public class OSGiBundleResourceLoader {

    private OSGiBundleResourceLoader() { // static use only
    }

    public static boolean isOSGiRuntime() {
        return IS_OSGI_RUNTIME;
    }

    public static String getContainerBundleName(URL resourceURL) {
        requireOSGi();
        return OSGiEnvironmentLoader.getContainerBundleName(resourceURL);
    }

    public static Enumeration<URL> getBundleDirectoryContent(URL resourceURL) {
        requireOSGi();
        return OSGiEnvironmentLoader.getBundleDirectoryContent(resourceURL);
    }

    private static void requireOSGi() {
        if (!IS_OSGI_RUNTIME) {
            throw new IllegalStateException(OSGiBundleResourceLoader.class.getSimpleName() + " must only be used within a OSGi runtime");
        }
    }

    private static final boolean IS_OSGI_RUNTIME;

    static {
        boolean isOSGI;
        try {
            Bundle.class.getName();
            isOSGI = true;
        } catch (NoClassDefFoundError e) {
            isOSGI = false;
        }
        IS_OSGI_RUNTIME = isOSGI;
        if (IS_OSGI_RUNTIME) {
            OSGiEnvironmentLoader.initialize();
        }
    }

    private static class OSGiEnvironmentLoader {
        // Code using OSGi APIs has to be encapsulated into own class
        // to prevent NoClassDefFoundErrors in OSGi environments

        private static final Map<String, Long> HOST_2_BUNDLE_ID = new HashMap<>();
        private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();

        private static void initialize() {
            BundleContext context = getBundleContext();
            if (context != null) {
                final BundleTracker<Bundle> bundleTracker = new BundleTracker<Bundle>(context, Bundle.ACTIVE | Bundle.RESOLVED | Bundle.STARTING,
                        null) {
                    @Override
                    public Bundle addingBundle(Bundle bundle, BundleEvent event) {
                        Bundle javaCppBundle = context.getBundle();
                        if (requires(bundle, javaCppBundle)) {
                            String bundleURLHost = getBundleURLHost(bundle);
                            Long bundleId = bundle.getBundleId();
                            LOCK.writeLock().lock();
                            try {
                                HOST_2_BUNDLE_ID.put(bundleURLHost, bundleId);
                            } finally {
                                LOCK.writeLock().unlock();
                            }
                            return bundle;
                        }
                        return null;
                    }

                    @Override
                    public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
                        String bundleURLHost = getBundleURLHost(bundle);
                        LOCK.writeLock().lock();
                        try {
                            HOST_2_BUNDLE_ID.remove(bundleURLHost);
                        } finally {
                            LOCK.writeLock().unlock();
                        }
                    }

                };
                bundleTracker.open();
                context.addFrameworkListener(new FrameworkListener() {
                    @Override
                    public void frameworkEvent(FrameworkEvent event) {
                        if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
                            BundleContext bundleContext = getBundleContext(); // don't keep a reference on the BundleContext
                            if (bundleContext != null) {
                                LOCK.writeLock().lock();
                                try {
                                    List<Entry<String, Long>> toAdd = new ArrayList<>();
                                    for (Iterator<Entry<String, Long>> iterator = HOST_2_BUNDLE_ID.entrySet().iterator(); iterator.hasNext();) {
                                        Entry<String, Long> entry = iterator.next();
                                        Long bundleId = entry.getValue();
                                        Bundle bundle = bundleContext.getBundle(bundleId);
                                        String bundleURLHost = getBundleURLHost(bundle);
                                        if (bundleURLHost.equals(entry.getKey())) {
                                            iterator.remove();
                                            toAdd.add(new SimpleImmutableEntry<>(bundleURLHost, bundleId));
                                        }
                                    }
                                    for (Entry<String, Long> entry : toAdd) {
                                        HOST_2_BUNDLE_ID.put(entry.getKey(), entry.getValue());
                                    }
                                } finally {
                                    LOCK.writeLock().unlock();
                                }
                            }
                        } else if (event.getType() == FrameworkEvent.STOPPED) {
                            bundleTracker.close();
                        }
                    }
                });
            }
        }

        private static BundleContext getBundleContext() {
            Bundle bundle = FrameworkUtil.getBundle(OSGiEnvironmentLoader.class);
            if (bundle != null) {
                int state = bundle.getState();
                if (state != Bundle.ACTIVE && (state == Bundle.INSTALLED || state == Bundle.RESOLVED || state == Bundle.STARTING)) {
                    try {
                        bundle.start();
                    } catch (BundleException e) { // ignore
                    }
                }
                if (bundle.getState() == Bundle.ACTIVE) {
                    return bundle.getBundleContext();
                }
            }
            return null;
        }

        private static boolean requires(Bundle source, Bundle target) {
            BundleWiring sourceWiring = source.adapt(BundleWiring.class);
            Queue<BundleWiring> pending = new ArrayDeque<>(Collections.singleton(sourceWiring));
            Set<BundleWiring> visited = new HashSet<>(Collections.singleton(sourceWiring));

            while (!pending.isEmpty()) { // perform iterative bfs
                BundleWiring wiring = pending.remove();
                if (wiring.getBundle().equals(target)) {
                    return true;
                }
                List<BundleWire> requiredWires = wiring.getRequiredWires(null);
                for (BundleWire requiredWire : requiredWires) {
                    BundleWiring provider = requiredWire.getProviderWiring();
                    if (visited.add(provider)) {
                        pending.add(provider);
                    }
                }
            }
            return false;
        }

        private static String getBundleURLHost(Bundle bundle) {
            return bundle.getEntry("/").getHost();
        }

        private static Bundle getContainerBundle(URL url) {
            LOCK.readLock().lock();
            Long bundleId;
            try {
                bundleId = HOST_2_BUNDLE_ID.get(url.getHost());
            } finally {
                LOCK.readLock().unlock();
            }
            if (bundleId != null) {
                BundleContext context = getBundleContext();
                if (context != null) {
                    return context.getBundle(bundleId);
                }
            }
            return null;
        }

        public static String getContainerBundleName(URL resourceURL) {
            Bundle bundle = getContainerBundle(resourceURL);
            if (bundle != null) {
                Version v = bundle.getVersion();
                String version = v.getMajor() + "." + v.getMinor() + "." + v.getMicro(); // skip qualifier
                return bundle.getSymbolicName() + "_" + version;
            }
            return null;
        }

        public static Enumeration<URL> getBundleDirectoryContent(URL resourceURL) {
            Bundle bundle = getContainerBundle(resourceURL);
            if (bundle != null) {
                return bundle.findEntries(resourceURL.getPath(), null, true);
            }
            return null;
        }
    }
}
