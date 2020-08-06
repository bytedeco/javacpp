package org.bytedeco.javacpp.tools;

import org.bytedeco.javacpp.Pointer;

import javax.management.*;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;

public class PointerBufferPoolMXBean implements BufferPoolMXBean {

    private static final Logger LOGGER = Logger.create(PointerBufferPoolMXBean.class);
    private static final String JAVACPP_MXBEAN_NAME = "javacpp";
    private static final ObjectName OBJECT_NAME;

    static {
        ObjectName objectName = null;
        try {
            objectName = new ObjectName("java.nio:type=BufferPool,name=" + JAVACPP_MXBEAN_NAME);
        } catch (MalformedObjectNameException e) {
            LOGGER.warn("Could not create OBJECT_NAME for " + JAVACPP_MXBEAN_NAME);
        }
        OBJECT_NAME = objectName;
    }

    @Override
    public String getName() {
        return JAVACPP_MXBEAN_NAME;
    }

    @Override
    public ObjectName getObjectName() {
        return OBJECT_NAME;
    }

    @Override
    public long getCount() {
        return Pointer.totalCount();
    }

    @Override
    public long getTotalCapacity() {
        return Pointer.maxPhysicalBytes();
    }

    @Override
    public long getMemoryUsed() {
        return Pointer.totalBytes();
    }

    public static void register() {
        if (OBJECT_NAME != null) {
            try {
                ManagementFactory.getPlatformMBeanServer().registerMBean(new PointerBufferPoolMXBean(), OBJECT_NAME);
            } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
                LOGGER.warn("Could not register " + JAVACPP_MXBEAN_NAME + " BufferPoolMXBean");
            }
        }
    }
}
