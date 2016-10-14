package org.bytedeco.javacpp.tools;

import org.bytedeco.javacpp.ClassProperties;
import org.bytedeco.javacpp.Loader;
import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public class Runtime extends Builder {
    private final Class _preset;
    private final ClassProperties _cp;
    private final String _target;
    private static final String _classes = "target/classes";

    public Runtime(Class preset) {
        System.setProperty("org.bytedeco.javacpp.loadlibraries", "true");
        _preset = preset;
        _cp = new ClassProperties(properties);
        _cp.load(preset, true);
        List<String> targets = _cp.get("target");
        _target = targets.get(targets.size() - 1);
        List<Class> classes = _cp.getInheritedClasses();
        ConfigurableMavenResolverSystem maven = Maven.configureResolver();
        maven.workOffline(true);
        maven.withClassPathResolution(false);
        Repository repo = Loader.getRepository();

        for (Class cls : classes) {
            Class generated;
            try {
                generated = Class.forName(cls.getName().replace(".presets", ""));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            Coordinates c = Loader.coordinates(generated);
            if (c != null) {
                String canon = c.group + ":" + c.id + ":jar:include:" + c.version;
                Path jar = maven.resolve(canon).withoutTransitivity().asSingleFile().toPath();
                Path path = repo.getPath(new Coordinates(c, "include", jar));
                p("platform.includepath", path.toString());

                String platform = Loader.getPlatform();
                jar = maven.resolve(
                        c.group + ":" + c.id + ":jar:" + platform + ":" + c.version
                ).withoutTransitivity().asSingleFile().toPath();
                path = repo.getPath(new Coordinates(c, platform, jar));
                p("platform.linkpath", path.toString());
            }
        }
    }

    public void generateJava(String dir) {
        outputDirectory(dir);
        try {
            classesOrPackages(_preset.getName());
            build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void compileJava(String dir) {
        String cp = "";
        URLClassLoader cl = (URLClassLoader)
                Thread.currentThread().getContextClassLoader();
        for (URL url : cl.getURLs())
            cp += ':' + url.getPath();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        String targetPath = _target.replace('.', File.separatorChar);
        int exitValue = compiler.run(null, null, null,
                "-cp", cp,
                "-d", _classes,
                new File(dir, targetPath + ".java").getPath());
        if (exitValue != 0)
            throw new RuntimeException();
    }

    private String cppName(String dir) {
        return new File(dir, "jni" + _preset.getSimpleName() + ".cpp").getPath();
    }

    public void generateCpp(String dir) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            // loadClass does not initialize the class, so lib is not loaded
            Class[] classes = new Class[]{
                    cl.loadClass(_preset.getName()),
                    cl.loadClass(_target)
            };

            ClassProperties p = Loader.loadProperties(
                    classes, properties, true);

            try (Generator generator = new Generator(logger, p)) {
                String classPath = System.getProperty("java.class.path");
                for (String s : classScanner.getClassLoader().getPaths()) {
                    classPath += File.pathSeparator + s;
                }
                String path = cppName(dir);
                logger.info("Generating " + path);
                // new File(cpp).delete();
                try {
                    if (!generator.generate(path, null, classPath, classes))
                        throw new RuntimeException();
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void compileCpp(String dir, boolean debug) {
        File dst = new File("target/classes/org/bytedeco/javacpp/linux-x86_64");
        dst.mkdirs();
        File lib = new File(dst, "libjni" + _preset.getSimpleName() + ".so");
        logger.info("Compiling " + lib);
        ClassProperties p = Loader.loadProperties(
                new Class[]{_preset}, properties, true);

        if (debug) {
            properties.put("platform.compiler.output",
                    "-march=x86-64 -m64 -DDEBUG -g -O0 -fPIC -dynamiclib -o\u0020");
            properties.remove("platform.linkpath.prefix2");
        }

        int exitValue;
        try {
            String path = cppName(dir);
            exitValue = compile(path, lib.getPath(), p, new File("."));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (exitValue != 0)
            throw new RuntimeException();
    }

    void p(String k, String v) {
        String current = properties.getProperty(k);
        if (current != null) {
            if (current.length() > 0)
                current += ":";
            v = current + v;
        }
        properties.setProperty(k, v);
    }
}
