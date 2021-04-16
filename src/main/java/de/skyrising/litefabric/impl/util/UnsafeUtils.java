package de.skyrising.litefabric.impl.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeUtils {
    private static final Unsafe UNSAFE;
    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
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
}
