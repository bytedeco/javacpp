package org.bytedeco.javacpp.test.osgi;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.annotation.Platform;

@Platform(include = "calc.h")
public class Calc {
    
    static {
        
        // This line should be sufficient, but it
        // isn't because the library gets loaded by
        // the wrong classloader
        //
        // Loader.load();
        
        // This is what we need to happen in the scope
        // of the bundle containing the native code, 
        // not the JavaCPP bundle. Note that the call
        // to the Loader is just to force a wiring to
        // the JavaCPP package needed by the JNI code
        
        Loader.getPlatform();
        
        System.loadLibrary("jniCalc");
    }
    
    private Calc() {}
    

    public static native int add(int a, int b);

}
