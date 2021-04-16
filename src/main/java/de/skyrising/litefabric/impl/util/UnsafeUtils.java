package de.skyrising.litefabric.impl.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeUtils {
    private final Object firstField = null;
    private static final Unsafe UNSAFE;
    private static final long FIRST_FIELD_OFFSET;
    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
            FIRST_FIELD_OFFSET = UNSAFE.objectFieldOffset(UnsafeUtils.class.getDeclaredField("firstField"));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setFinalField(Field f, Object base, Object value) {
        if (base == null) {
            UNSAFE.putObject(UNSAFE.staticFieldBase(f), UNSAFE.staticFieldOffset(f), value);
        } else {
            UNSAFE.putObject(base, UNSAFE.objectFieldOffset(f), value);
        }
    }

    public static void setParent(ClassLoader child, ClassLoader parent) {
        UNSAFE.putObject(child, FIRST_FIELD_OFFSET, parent);
    }
/*
    public static ClassLoader createDynamicURLClassLoader(URLClassLoader previous) {
        Class<?> dynamicUrlClassLoaderCls = previous.getClass();
        try {
            Object newLoader = UNSAFE.allocateInstance(dynamicUrlClassLoaderCls);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
 */
}
