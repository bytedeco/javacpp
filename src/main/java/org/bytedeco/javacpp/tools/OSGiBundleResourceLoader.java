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
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

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

    private static final Map<String, Long> HOST_2_BUNDLE = new ConcurrentHashMap<String, Long>();
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

        private static void initialize() {
            BundleContext context = getBundleContext();
            if (context != null) {
                indexAllBundles(context);
                context.addBundleListener(new BundleListener() {
                    @Override
                    public void bundleChanged(BundleEvent event) {
                        Bundle bundle = event.getBundle();
                        switch (event.getType()) {
                        case BundleEvent.RESOLVED:
                            HOST_2_BUNDLE.put(getBundleURLHost(bundle), bundle.getBundleId());
                            break;
                        case BundleEvent.UNRESOLVED:
                            HOST_2_BUNDLE.remove(getBundleURLHost(bundle));
                            break;
                        default:
                            break;
                        }
                    }
                });
                context.addFrameworkListener(new FrameworkListener() {
                    @Override
                    public void frameworkEvent(FrameworkEvent event) {
                        if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
                            HOST_2_BUNDLE.clear();
                            indexAllBundles(getBundleContext());
                            // don't keep a reference on the BundleContext
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

        private static void indexAllBundles(BundleContext context) {
            if (context != null) {
                for (Bundle bundle : context.getBundles()) {
                    HOST_2_BUNDLE.put(getBundleURLHost(bundle), bundle.getBundleId());
                }
            }
        }

        private static String getBundleURLHost(Bundle bundle) {
            return bundle.getEntry("/").getHost();
        }

        private static Bundle getContainerBundle(URL url) {
            Long bundleId = HOST_2_BUNDLE.get(url.getHost());
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
