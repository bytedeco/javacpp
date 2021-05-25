/*
 * Copyright (C) 2011-2020 Samuel Audet
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.tools.Logger;

/**
 * Does the heavy lifting of collecting values off Properties annotations found
 * on enclosing classes. Operates for the desired "platform" value specified
 * in {@link java.util.Properties}. As a {@link HashMap}, it makes the result
 * easily accessible, and mutable.
 *
 * @see Loader#loadProperties(Class, java.util.Properties, boolean)
 */
public class ClassProperties extends HashMap<String,List<String>> {
    private static final Logger logger = Logger.create(ClassProperties.class);

    public ClassProperties() { }
    public ClassProperties(Properties properties) {
        platform      = properties.getProperty("platform");
        platformExtension = properties.getProperty("platform.extension");
        platformRoot  = properties.getProperty("platform.root");
        pathSeparator = properties.getProperty("platform.path.separator");
        if (platformRoot == null || platformRoot.length() == 0) {
            platformRoot = ".";
        }
        if (!platformRoot.endsWith(File.separator)) {
            platformRoot += File.separator;
        }
        for (Map.Entry e : properties.entrySet()) {
            String k = (String)e.getKey(), v = (String)e.getValue();
            if (v == null || v.length() == 0) {
                continue;
            }
            if (k.equals("platform.includepath") || k.equals("platform.includeresource") || k.equals("platform.include")
                || k.equals("platform.linkpath") || k.equals("platform.linkresource") || k.equals("platform.link")
                || k.equals("platform.preloadpath") || k.equals("platform.preloadresource") || k.equals("platform.preload")
                || k.equals("platform.resourcepath") || k.equals("platform.resource")
                || k.equals("platform.frameworkpath") || k.equals("platform.framework")
                || k.equals("platform.executablepath") || k.equals("platform.executable")
                || k.equals("platform.compiler.*") || k.equals("platform.library.suffix") || k.equals("platform.extension")) {
                addAll(k, v.split(pathSeparator));
            } else {
                setProperty(k, v);
            }
        }
    }

    String[] defaultNames = {};
    String platform, platformExtension, platformRoot, pathSeparator;
    List<Class> inheritedClasses = null;
    List<Class> effectiveClasses = null;
    boolean loaded = false;

    public List<String> get(String key) {
        List<String> list = super.get(key);
        if (list == null) {
            put((String)key, list = new ArrayList<String>());
        }
        return list;
    }

    public void addAll(String key, String ... values) {
        if (values != null) {
            addAll(key, Arrays.asList(values));
        }
    }
    public void addAll(String key, Collection<String> values) {
        if (values != null) {
            String root = null;
            if (key.equals("platform.compiler") || key.equals("platform.sysroot") || key.equals("platform.toolchain") ||
                    key.equals("platform.includepath") || key.equals("platform.linkpath")) {
                root = platformRoot;
            }

            List<String> values2 = get(key);
            for (String value : values) {
                if (value == null) {
                    continue;
                }
                if (root != null && !new File(value).isAbsolute() &&
                        new File(root + value).exists()) {
                    value = root + value;
                }
                if (!values2.contains(value)) {
                    values2.add(value);
                }
            }
        }
    }

    public String getProperty(String key) {
        return getProperty(key, null);
    }
    public String getProperty(String key, String defaultValue) {
        List<String> values = get(key);
        return values.isEmpty() ? defaultValue : values.get(0);
    }
    public String setProperty(String key, String value) {
        List<String> values = get(key);
        String oldValue = values.isEmpty() ? null : values.get(0);
        values.clear();
        addAll(key, value);
        return oldValue;
    }

    public void load(Class cls, boolean inherit) {
        Class<?> c = Loader.getEnclosingClass(cls);
        List<Class> classList = new ArrayList<Class>();
        classList.add(0, c);
        while (!c.isAnnotationPresent(org.bytedeco.javacpp.annotation.Properties.class)
                && !c.isAnnotationPresent(Platform.class) && c.getSuperclass() != null
                && c.getSuperclass() != Object.class && c.getSuperclass() != Pointer.class) {
            // accumulate superclasses to process native methods from those as well
            classList.add(0, c = c.getSuperclass());
        }
        if (effectiveClasses == null) {
            effectiveClasses = classList;
        }
        org.bytedeco.javacpp.annotation.Properties classProperties =
                c.getAnnotation(org.bytedeco.javacpp.annotation.Properties.class);
        Platform classPlatform = c.getAnnotation(Platform.class);
        Platform[] platforms = null;
        String ourTarget = null;
        if (classProperties != null) {
            Class[] classes = classProperties.inherit();
            if (inherit && classes != null) {
                if (inheritedClasses == null) {
                    inheritedClasses = new ArrayList<Class>();
                }
                for (Class c2 : classes) {
                    load(c2, inherit);
                    if (!inheritedClasses.contains(c2)) {
                        inheritedClasses.add(c2);
                    }
                }
            }
            String target = classProperties.target();
            String global = classProperties.global();
            if (global.length() == 0) {
                global = target;
            } else if (target.length() == 0) {
                target = global;
            } else if (target.length() > 0 && !global.contains(".")) {
                global = target + "." + global;
            }
            ourTarget = global;
            if (target.length() > 0) {
                addAll("target", target);
            }
            if (global.length() > 0) {
                addAll("global", global);
            }
            String helper = classProperties.helper();
            if (helper.length() > 0 && !helper.contains(".")) {
                helper = target + "." + helper;
            }
            if (helper.length() > 0) {
                addAll("helper", helper);
            }
            String[] names = classProperties.names();
            if (names.length > 0) {
                defaultNames = names;
            }
            platforms = classProperties.value();
        }
        if (classPlatform != null) {
            if (platforms == null) {
                platforms = new Platform[] { classPlatform };
            } else {
                platforms = Arrays.copyOf(platforms, platforms.length + 1);
                platforms[platforms.length - 1] = classPlatform;
            }
        }
        boolean hasPlatformProperties = platforms != null && platforms.length > (classProperties != null && classPlatform != null ? 1 : 0);

        String[] pragma = {}, define = {}, exclude = {}, include = {}, cinclude = {}, includepath = {}, includeresource = {}, compiler = {},
                 linkpath = {}, linkresource = {}, link = {}, frameworkpath = {}, framework = {}, preloadpath = {}, preloadresource = {}, preload = {},
                 resourcepath = {}, resource = {}, extension = {}, executablepath = {}, executable = {};
        String library = "jni" + c.getSimpleName();
        if (hasPlatformProperties) {
            if (ourTarget != null && ourTarget.length() > 0) {
                library = "jni" + ourTarget.substring(ourTarget.lastIndexOf('.') + 1);
            }
        } else {
            List<String> targets = get("global");
            if (targets != null && targets.size() > 0) {
                String target = targets.get(targets.size() - 1);
                library = "jni" + target.substring(target.lastIndexOf('.') + 1);
            }
        }
        for (Platform p : platforms != null ? platforms : new Platform[0]) {
            String[][] names = { p.value().length > 0 ? p.value() : defaultNames, p.not(), p.pattern() };
            boolean[] matches = { false, false, false };
            for (int i = 0; i < names.length; i++) {
                for (String s : names[i]) {
                    if ((i < 2 && platform.startsWith(s)) || (s.length() > 0 && platform.matches(s))) {
                        matches[i] = true;
                        break;
                    }
                }
            }
            if ((names[0].length == 0 || matches[0]) && (names[1].length == 0 || !matches[1]) && (names[2].length == 0 || matches[2])) {
                // when no extensions are given by user, but we are in library loading mode, try to load extensions anyway
                boolean match = p.extension().length == 0 || (Loader.isLoadLibraries() && platformExtension == null);
                for (String s : p.extension()) {
                    if (platformExtension != null && platformExtension.length() > 0 && platformExtension.endsWith(s)) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    continue;
                }
                if (p.pragma()     .length > 0) { pragma      = p.pragma();      }
                if (p.define()     .length > 0) { define      = p.define();      }
                if (p.exclude()    .length > 0) { exclude     = p.exclude();     }
                if (p.include()    .length > 0) { include     = p.include();     }
                if (p.cinclude()   .length > 0) { cinclude    = p.cinclude();    }
                if (p.includepath().length > 0) { includepath = p.includepath(); }
                if (p.includeresource().length > 0) { includeresource = p.includeresource(); }
                if (p.compiler()   .length > 0) { compiler    = p.compiler();    }
                if (p.linkpath()   .length > 0) { linkpath    = p.linkpath();    }
                if (p.linkresource()   .length > 0) { linkresource    = p.linkresource();    }
                if (p.link()       .length > 0) { link        = p.link();        }
                if (p.frameworkpath().length > 0) { frameworkpath = p.frameworkpath(); }
                if (p.framework()  .length > 0) { framework   = p.framework();   }
                if (p.preloadresource().length > 0) { preloadresource = p.preloadresource(); }
                if (p.preloadpath().length > 0) { preloadpath = p.preloadpath(); }
                if (p.preload()    .length > 0) { preload     = p.preload();     }
                if (p.resourcepath().length > 0) { resourcepath = p.resourcepath(); }
                if (p.resource()    .length > 0) { resource     = p.resource();     }
                if (p.extension()   .length > 0) { extension    = p.extension();    }
                if (p.executablepath().length > 0) { executablepath = p.executablepath(); }
                if (p.executable()  .length > 0) { executable   = p.executable();   }
                if (p.library().length() > 0)   { library     = p.library();     }
            }
        }
        for (int i = 0; i < includeresource.length; i++) {
            // turn resources into absolute names
            String name = includeresource[i];
            if (!name.startsWith("/")) {
                String s = cls.getName().replace('.', '/');
                int n = s.lastIndexOf('/');
                if (n >= 0) {
                    name = s.substring(0, n + 1) + name;
                }
                includeresource[i] = "/" + name;
            }
        }
        for (int i = 0; i < linkresource.length; i++) {
            // turn resources into absolute names
            String name = linkresource[i];
            if (!name.startsWith("/")) {
                String s = cls.getName().replace('.', '/');
                int n = s.lastIndexOf('/');
                if (n >= 0) {
                    name = s.substring(0, n + 1) + name;
                }
                linkresource[i] = "/" + name;
            }
        }
        addAll("platform.pragma", pragma);
        addAll("platform.define", define);
        addAll("platform.exclude", exclude);
        addAll("platform.include", include);
        addAll("platform.cinclude", cinclude);
        addAll("platform.includepath", includepath);
        addAll("platform.includeresource", includeresource);
        addAll("platform.compiler.*", compiler);
        addAll("platform.linkpath", linkpath);
        addAll("platform.linkresource", linkresource);
        addAll("platform.link", link);
        addAll("platform.frameworkpath", frameworkpath);
        addAll("platform.framework", framework);
        addAll("platform.preloadresource", preloadresource);
        addAll("platform.preloadpath", preloadpath);
        addAll("platform.preload", preload);
        addAll("platform.resourcepath", resourcepath);
        addAll("platform.resource", resource);
        if (platformExtension == null || platformExtension.length() == 0) {
            // don't override the platform extension when found outside the class
            addAll("platform.extension", extension);
        }
        addAll("platform.executablepath", executablepath);
        addAll("platform.executable", executable);
        setProperty("platform.library", library);

        if (LoadEnabled.class.isAssignableFrom(c)) {
            try {
                ((LoadEnabled)c.newInstance()).init(this);
            } catch (ClassCastException | InstantiationException | IllegalAccessException e) {
                logger.warn("Could not create an instance of " + c + ": " + e);
            }
        }

        // need platform information from both classProperties and classPlatform to be considered "loaded"
        loaded |= hasPlatformProperties;
    }

    public List<Class> getInheritedClasses() {
        return inheritedClasses;
    }

    public List<Class> getEffectiveClasses() {
        return effectiveClasses;
    }

    public boolean isLoaded() {
        return loaded;
    }
}
