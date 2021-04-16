package de.skyrising.litefabric.impl;

import de.skyrising.litefabric.impl.util.UnsafeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class ClassInfoHax {
    private static final MethodHandle constructor;
    private static boolean applied;

    static {
        try {
            Constructor<ClassInfo> constr = ClassInfo.class.getDeclaredConstructor(ClassNode.class);
            constr.setAccessible(true);
            constructor = MethodHandles.lookup().unreflectConstructor(constr);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    static void apply() {
        if (applied) return;
        applied = true;
        try {
            Field cache = ClassInfo.class.getDeclaredField("cache");
            cache.setAccessible(true);
            @SuppressWarnings("unchecked")
            DelegatingMap map = new DelegatingMap((Map<String, ClassInfo>) cache.get(null));
            UnsafeUtils.setFinalField(cache, null, map);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    static class DelegatingMap implements Map<String, ClassInfo> {
        private final Map<String, ClassInfo> contents;
        private final Set<String> negatives = new HashSet<>();

        DelegatingMap(Map<String, ClassInfo> contents) {
            this.contents = contents;
        }

        private void tryGenerate(String key) {
            if (contents.containsKey(key) || negatives.contains(key)) return;
            for (LitemodContainer mod : LiteFabric.getInstance().mods.values()) {
                try {
                    ClassNode node = mod.classLoader.getClassNode(key);
                    if (node == null) continue;
                    ClassInfo info = (ClassInfo) constructor.invokeExact(node);
                    contents.put(key, info);
                    return;
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
            negatives.add(key);
        }

        @Override
        public int size() {
            return contents.size();
        }

        @Override
        public boolean isEmpty() {
            return contents.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            tryGenerate((String) key);
            return contents.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return contents.containsValue(value);
        }

        @Override
        public ClassInfo get(Object key) {
            tryGenerate((String) key);
            return contents.get(key);
        }

        @Nullable
        @Override
        public ClassInfo put(String key, ClassInfo value) {
            return contents.put(key, value);
        }

        @Override
        public ClassInfo remove(Object key) {
            return contents.remove(key);
        }

        @Override
        public void putAll(@NotNull Map<? extends String, ? extends ClassInfo> m) {
            contents.putAll(m);
        }

        @Override
        public void clear() {
            contents.clear();
            negatives.clear();
        }

        @NotNull
        @Override
        public Set<String> keySet() {
            return contents.keySet();
        }

        @NotNull
        @Override
        public Collection<ClassInfo> values() {
            return contents.values();
        }

        @NotNull
        @Override
        public Set<Entry<String, ClassInfo>> entrySet() {
            return contents.entrySet();
        }
    }
}
