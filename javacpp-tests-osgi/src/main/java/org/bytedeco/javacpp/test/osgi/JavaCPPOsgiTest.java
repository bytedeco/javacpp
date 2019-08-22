package org.bytedeco.javacpp.test.osgi;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JavaCPPOsgiTest {
    
    @Test
    public void testJavaCPP() {
        assertEquals(3, Calc.add(1, 2));
    }

}
