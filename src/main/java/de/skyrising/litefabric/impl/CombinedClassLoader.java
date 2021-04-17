package de.skyrising.litefabric.impl;

import de.skyrising.litefabric.impl.util.UnsafeUtils;

import java.lang.reflect.Field;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.List;

class CombinedClassLoader extends SecureClassLoader {
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
