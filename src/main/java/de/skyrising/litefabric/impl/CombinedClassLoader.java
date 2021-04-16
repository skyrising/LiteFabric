package de.skyrising.litefabric.impl;

import de.skyrising.litefabric.impl.util.UnsafeUtils;

import java.lang.reflect.Field;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.List;

class CombinedClassLoader extends SecureClassLoader {
    static final ClassLoader knotClassLoader;
    private static final Field originalLoaderField;
    private final List<LitemodClassLoader> classLoaders = new ArrayList<>();
    private boolean loading;

    static {
        try {
            knotClassLoader = CombinedClassLoader.class.getClassLoader();
            originalLoaderField = knotClassLoader.getClass().getDeclaredField("originalLoader");
            originalLoaderField.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    CombinedClassLoader() {
        super(findParent());
        System.out.println(getParent());
        UnsafeUtils.setFinalField(originalLoaderField, knotClassLoader, this);
        try {
            System.out.println(originalLoaderField.get(knotClassLoader));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static ClassLoader findParent() {
        try {
            return (ClassLoader) originalLoaderField.get(knotClassLoader);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void add(LitemodClassLoader classLoader) {
        this.classLoaders.add(classLoader);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = loadFromSiblings(name, null);
            if (c == null) c = getParent().loadClass(name);
            if (resolve) resolveClass(c);
            return c;
        }
    }

    public Class<?> loadFromSiblings(String name, LitemodClassLoader except) {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c != null) return c;
            if (loading) return null;
            loading = true;
            try {
                for (LitemodClassLoader loader : classLoaders) {
                    if (loader == except) continue;
                    c = loader.selfLoad(name);
                    if (c != null) break;
                }
                if (c == null) {
                    c = knotClassLoader.loadClass(name);
                }
            } catch (ClassNotFoundException ignored) {
            } finally {
                loading = false;
            }
            return c;
        }
    }

    public URL getResourceFromSiblings(String name, LitemodClassLoader except) {
        for (LitemodClassLoader loader : classLoaders) {
            if (loader == except) continue;
            URL resource = loader.findOwnResource(name);
            if (resource != null) return resource;
        }
        return null;
    }

    static {
        registerAsParallelCapable();
    }
}
