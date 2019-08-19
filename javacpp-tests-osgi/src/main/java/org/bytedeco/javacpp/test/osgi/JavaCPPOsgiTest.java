package org.bytedeco.javacpp.test.osgi;

import static org.junit.Assert.assertEquals;

import org.bytedeco.javacpp.ClassProperties;
import org.bytedeco.javacpp.LoadEnabled;
import org.junit.Test;

public class JavaCPPOsgiTest implements LoadEnabled {
    
    @Test
    public void testJavaCPP() {
        assertEquals(3, Calc.add(1, 2));
    }

	@Override
	public void init(ClassProperties properties) {
		// TODO Auto-generated method stub
		
	}

}
