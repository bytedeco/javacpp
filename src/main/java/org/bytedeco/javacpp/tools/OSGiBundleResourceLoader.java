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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

public class OSGiBundleResourceLoader {

    private OSGiBundleResourceLoader() { // static use only
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
    }

    public static boolean isOSGiRuntime() {
        return IS_OSGI_RUNTIME;
    }

    public static String getOSGiContainerBundleName(URL resourceURL) {
        requireOSGi();
        return OSGiEnvironmentLoader.getContainerBundleName(resourceURL);
    }

    public static Enumeration<URL> getOSGiBundleDirectoryContent(URL resourceURL) {
        requireOSGi();
        return OSGiEnvironmentLoader.getBundleDirectoryContent(resourceURL);
    }

    public static URL getOSGiClassResource(Class<?> c, String name) {
        requireOSGi();
        return OSGiEnvironmentLoader.getClassResource(c, name);
    }

    public static Enumeration<URL> getOSGiClassLoaderResources(ClassLoader cl, String name) throws IOException {
        requireOSGi();
        return OSGiEnvironmentLoader.getClassLoaderResources(cl, name);
    }

    private static void requireOSGi() {
        if (!IS_OSGI_RUNTIME) {
            throw new IllegalStateException(OSGiBundleResourceLoader.class.getSimpleName() + " must only be used within a OSGi runtime");
        }
    }

    private static class OSGiEnvironmentLoader {
        // Code using OSGi APIs has to be encapsulated into own class
        // to prevent NoClassDefFoundErrors in OSGi environments

        private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();
        private static final WeakHashMap<URL, WeakReference<Bundle>> URL_TO_BUNDLE = new WeakHashMap<>();

        private static void associateURLtoBundle(URL resource, Bundle bundle) {
            URL_TO_BUNDLE.put(resource, new WeakReference<>(bundle));
        }

        private static Bundle getContainerBundle(URL url) {
            WeakReference<Bundle> bundle;
            LOCK.readLock().lock();
            try {
                bundle = URL_TO_BUNDLE.get(url);
            } finally {
                LOCK.readLock().unlock();
            }
            return bundle != null ? bundle.get() : null;
        }

        private static URL getClassResource(Class<?> c, String name) {
            URL resource = c.getResource(name);
            if (resource != null) {
                Bundle bundle = FrameworkUtil.getBundle(c);
                LOCK.writeLock().lock();
                try {
                    associateURLtoBundle(resource, bundle);
                } finally {
                    LOCK.writeLock().unlock();
                }
            }
            return resource;
        }

        private static Enumeration<URL> getClassLoaderResources(ClassLoader cl, String name) throws IOException {
            Enumeration<URL> resources = cl.getResources(name);
            if (resources != null && resources.hasMoreElements()) {
                Optional<Bundle> bundleOpt = FrameworkUtil.getBundle(cl);
                if (bundleOpt.isPresent()) {
                    Bundle bundle = bundleOpt.get();
                    List<URL> resourcesList = Collections.list(resources);
                    LOCK.writeLock().lock();
                    try {
                        for (URL url : resourcesList) {
                            associateURLtoBundle(url, bundle);
                        }
                    } finally {
                        LOCK.writeLock().unlock();
                    }
                    return Collections.enumeration(resourcesList);
                }
            }
            return resources;
        }

        private static String getContainerBundleName(URL resourceURL) {
            Bundle bundle = getContainerBundle(resourceURL);
            if (bundle != null) {
                Version v = bundle.getVersion();
                String version = v.getMajor() + "." + v.getMinor() + "." + v.getMicro(); // skip qualifier
                return bundle.getSymbolicName() + "_" + version;
            }
            return null;
        }

        private static Enumeration<URL> getBundleDirectoryContent(URL resourceURL) {
            Bundle bundle = getContainerBundle(resourceURL);
            if (bundle != null) {
                return bundle.findEntries(resourceURL.getPath(), null, true);
            }
            return null;
        }
    }
}
