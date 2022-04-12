package de.skyrising.litefabric.impl;

import de.skyrising.litefabric.impl.util.UnsafeUtils;

import java.lang.reflect.Field;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.List;

public class CombinedClassLoader extends SecureClassLoader {
    static final ClassLoader knotClassLoader;
    private static final Field urlLoaderField;
    private final List<LitemodClassProvider> classLoaders = new ArrayList<>();

    static {
        try {
            knotClassLoader = CombinedClassLoader.class.getClassLoader();
            urlLoaderField = knotClassLoader.getClass().getDeclaredField("urlLoader");
            urlLoaderField.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    CombinedClassLoader() {
        super(findParent());
        try {
            ClassLoader urlLoader = ((ClassLoader) urlLoaderField.get(knotClassLoader));
            UnsafeUtils.setParent(urlLoader, this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static ClassLoader findParent() {
        try {
            return ((ClassLoader) urlLoaderField.get(knotClassLoader)).getParent();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void add(LitemodClassProvider classLoader) {
        this.classLoaders.add(classLoader);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c != null) return c;
            for (LitemodClassProvider loader : classLoaders) {
                byte[] bytes = loader.getClassBytes(name);
                if (bytes == null) continue;
                c = defineClass(name, bytes);
                break;
            }
            if (c == null) c = knotClassLoader.loadClass(name);
            if (resolve) resolveClass(c);
            return c;
        }
    }

    public Class<?> defineClass(String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }

    @Override
    protected URL findResource(String name) {
        for (LitemodClassProvider loader : classLoaders) {
            URL resource = loader.findResource(name);
            if (resource != null) return resource;
        }
        return null;
    }

    static {
        registerAsParallelCapable();
    }
}
