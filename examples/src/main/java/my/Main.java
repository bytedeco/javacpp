package my;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.JavaCPP;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        Class preset = my.presets.test.class;

        String java = "src/main/java";
        JavaCPP.generateJava(preset, java);
        JavaCPP.compileJava(preset, java);

        String cpp = "src";
        JavaCPP.generateCpp(preset, cpp);
        JavaCPP.compileCpp(preset, cpp, false);

        // In another method to delay loading the generated class
        test();
    }

    public static void test() throws Exception {
        BytePointer s = new my.test.Test().test();
        System.out.println(s.getString());
    }
}

