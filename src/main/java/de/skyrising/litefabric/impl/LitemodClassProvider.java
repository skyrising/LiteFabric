package de.skyrising.litefabric.impl;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;

class LitemodClassProvider {
    private static final Logger LOGGER = LogManager.getFormatterLogger("LiteFabric|ClassProvider");
    private static final String SCREEN_CLASS = classNameOf("class_5641");
    private static final Set<String> CONFIG_GUI_SUPER_CLASSES = new HashSet<>(Arrays.asList(
        "fi/dy/masa/malilib/gui/GuiConfigsBase",
        SCREEN_CLASS
    ));
    private static final byte[] NOT_PRESENT_BYTES = new byte[0];
    private static final ClassNode NOT_PRESENT_NODE = new ClassNode();
    private static final Object2LongMap<String> TRANSFORM_TIME = new Object2LongOpenHashMap<>();
    private static final Object2LongMap<String> TASK_TIME = new Object2LongOpenHashMap<>();
    private static final Object2IntMap<String> TRANSFORM_COUNT = new Object2IntOpenHashMap<>();
    private static final WeakHashMap<LitemodClassProvider, Object> INSTANCES = new WeakHashMap<>();

    private final LitemodContainer mod;
    private final FileSystem fileSystem;
    private final LitemodRemapper remapper;
    private final Map<String, byte[]> classByteCache = new HashMap<>();
    private final Map<String, ClassNode> classNodeCache = new HashMap<>();
    private final ThreadLocal<LinkedList<String>> currentClasses = ThreadLocal.withInitial(LinkedList::new);
    private final Set<String> classes = new HashSet<>();

    public LitemodClassProvider(LitemodContainer mod, FileSystem fileSystem, LitemodRemapper remapper) {
        this.mod = mod;
        this.fileSystem = fileSystem;
        this.remapper = remapper;
        INSTANCES.put(this, null);
        try {
            cachePackages();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void cachePackages() throws IOException {
        for (Path root : fileSystem.getRootDirectories()) {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String p = file.toString();
                    if (attrs.isRegularFile() && p.endsWith(".class")) {
                        classes.add(p.substring(1, p.length() - 6));
                    }
                    return super.visitFile(file, attrs);
                }
            });
        }
    }

    byte[] getClassBytes(String name) {
        return computeIfAbsent(classByteCache, name.replace('.', '/'), NOT_PRESENT_BYTES, this::computeClassBytes);
    }

    private byte[] computeClassBytes(String name) {
        if (name.startsWith("java/")) return null;
        LinkedList<String> curCls = currentClasses.get();
        if (LiteFabric.PROFILE_STARTUP) curCls.push(name);
        long start = LiteFabric.PROFILE_STARTUP ? System.nanoTime() : 0;
        if (!classes.contains(name)) {
            countTime(name, start, curCls, false);
            return null;
        }
        String classFileName = name + ".class";
        Path classFilePath = fileSystem.getPath(classFileName);
        byte[] rawBytes;
        try {
            rawBytes = Files.readAllBytes(classFilePath);
        } catch (IOException e) {
            countTime(name, start, curCls, false);
            return null;
        }
        start = countSubTask(name, "readFile", start);
        ClassNode raw = readNode(rawBytes);
        start = countSubTask(name, "readNode", start);
        remapper.addClass(raw);
        ClassNode remapped = new ClassNode();
        ClassRemapper clsRemapper = new ClassRemapper(remapped, remapper);
        raw.accept(clsRemapper);
        countTime(name, start, curCls, true);
        long time = System.nanoTime();
        if (isConfigGuiCandidate(remapped)) mod.configGuiCandidates.add(remapped.name);
        time = countSubTask(name, "isConfigGuiCandidate", time);
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) makePublic(remapped);
        time = countSubTask(name, "makePublic", time);
        classNodeCache.put(name, remapped);
        time = countSubTask(name, "cache", time);
        ClassWriter writer = new ClassWriter(Opcodes.ASM9);
        remapped.accept(writer);
        byte[] bytes = writer.toByteArray();
        time = countSubTask(name, "toBytes", time);
        if (LiteFabric.DUMP_CLASSES) dump(classFileName, bytes);
        countSubTask(name, "dump", time);
        return bytes;
    }

    private static ClassNode readNode(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.EXPAND_FRAMES);
        return node;
    }

    private void dump(String classFileName, byte[] bytes) {
        Path out = Paths.get(".litefabric.out/class/" + classFileName);
        try {
            Files.createDirectories(out.getParent());
            Files.write(out, bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void makePublic(ClassNode remapped) {
        if (Annotations.getInvisible(remapped, Mixin.class) == null) return;
        for (MethodNode method : remapped.methods) {
            if ((method.access & Opcodes.ACC_STATIC) != 0) continue;
            if (method.visibleAnnotations == null || Annotations.getVisible(method, Overwrite.class) != null) {
                Bytecode.setVisibility(method, Opcodes.ACC_PUBLIC);
            }
        }
    }

    private static <K, V> V computeIfAbsent(Map<K, V> cache, K key, V negative, Function<K, V> compute) {
        V cacheValue = cache.get(key);
        if (cacheValue == negative) return null;
        if (cacheValue != null) return cacheValue;
        V value = cacheValue = compute.apply(key);
        if (cacheValue == null) cacheValue = negative;
        cache.put(key, cacheValue);
        return value;
    }

    private static void countTime(String name, long start, LinkedList<String> curCls, boolean didWork) {
        if (!LiteFabric.PROFILE_STARTUP) return;
        long thisTime = System.nanoTime() - start;
        String popped = curCls.pop();
        //noinspection StringEquality
        if (name != popped) {
            throw new IllegalStateException("currentClasses desync " + name + " vs. " + popped + " " + curCls);
        }
        if (!curCls.isEmpty()) {
            String parent = curCls.peek();
            TRANSFORM_TIME.put(parent, TRANSFORM_TIME.getLong(parent) - thisTime);
        }
        TRANSFORM_TIME.put(name, TRANSFORM_TIME.getLong(name) + thisTime);
        if (didWork) {
            if (TRANSFORM_COUNT.getInt(name) > 0) {
                LOGGER.warn("Retransforming %s, caused by %s", name, curCls.peek());
            }
            TRANSFORM_COUNT.put(name, TRANSFORM_COUNT.getInt(name) + 1);
        }
    }

    private static long countSubTask(String name, String task, long start) {
        if (!LiteFabric.PROFILE_STARTUP) return 0;
        long now = System.nanoTime();
        long time = now - start;
        String key = name + "." + task;
        TRANSFORM_TIME.put(key, TRANSFORM_TIME.getLong(key) + time);
        TASK_TIME.put(task, TASK_TIME.getLong(task) + time);
        return now;
    }

    private static boolean isConfigGuiCandidate(ClassNode node) {
        if (Annotations.getInvisible(node, Mixin.class) != null) return false;
        if ((node.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT)) != Opcodes.ACC_PUBLIC) return false;
        return CONFIG_GUI_SUPER_CLASSES.contains(node.superName);
    }

    private ClassNode computeClassNode(String name) {
        byte[] bytes = getClassBytes(name);
        if (bytes == null) return null;
        ClassNode cached = classNodeCache.get(name);
        if (cached != null && cached != NOT_PRESENT_NODE) return cached;
        long start = LiteFabric.PROFILE_STARTUP ? System.nanoTime() : 0;
        ClassReader reader = new ClassReader(bytes);
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        if (LiteFabric.PROFILE_STARTUP) {
            TRANSFORM_TIME.put(name, TRANSFORM_TIME.getLong(name) + (System.nanoTime() - start));
            TRANSFORM_COUNT.put(name, TRANSFORM_COUNT.getInt(name) + 1);
        }
        return node;
    }

    ClassNode getClassNode(String name) {
        return computeIfAbsent(classNodeCache, name.replace('.', '/'), NOT_PRESENT_NODE, this::computeClassNode);
    }

    URL findResource(String name) {
        if (name.startsWith("com/google/")) return null;
        try {
            if (name.endsWith(".class")) {
                Path tmpPath = LiteFabric.TMP_FILES.resolve(name);
                if (Files.exists(tmpPath)) return tmpPath.toUri().toURL();
                byte[] classBytes = getClassBytes(name.substring(0, name.length() - 6));
                if (classBytes != null) {
                    Files.createDirectories(tmpPath.getParent());
                    Files.write(tmpPath, classBytes);
                    return tmpPath.toUri().toURL();
                }
                return null;
            }
            Path path = fileSystem.getPath(name);
            if (Files.exists(path)) {
                if (LiteFabric.DUMP_RESOURCES) {
                    Path out = Paths.get(".litefabric.out/resource/", mod.meta.name, name);
                    Files.createDirectories(out.getParent());
                    Files.copy(path, out, StandardCopyOption.REPLACE_EXISTING);
                }
                URI uri = path.toUri();
                String uriStr = uri.toString();
                if (uriStr.contains("%25")) {
                    uriStr = uriStr.replaceAll("%25", "%");
                    try {
                        uri = new URI(uriStr);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
                return uri.toURL();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }

    private static String classNameOf(String intermediary) {
        return FabricLoader.getInstance().getMappingResolver().mapClassName("intermediary", "net.minecraft." + intermediary).replace('.', '/');
    }

    public static void logTransformPerformance() {
        List<String> sortedKeys = new ArrayList<>(TRANSFORM_TIME.keySet());
        sortedKeys.sort((a, b) -> Long.compare(TRANSFORM_TIME.getLong(b), TRANSFORM_TIME.getLong(a)));
        long totalTime = 0;
        for (Object2LongMap.Entry<String> e : TRANSFORM_TIME.object2LongEntrySet()) {
            totalTime += e.getLongValue();
            if (e.getLongValue() < 0) {
                LOGGER.warn("%s: %d", e.getKey(), e.getLongValue());
            }
        }
        LOGGER.info("Total transform time: %.3fms", totalTime / 1e6);
        for (int i = 0; i < 10 && i < sortedKeys.size(); i++) {
            String key = sortedKeys.get(i);
            LOGGER.info("  %s: %.3fms", key, TRANSFORM_TIME.getLong(key) / 1e6);
        }
        List<String> sortedTasks = new ArrayList<>(TASK_TIME.keySet());
        sortedTasks.sort((a, b) -> Long.compare(TASK_TIME.getLong(b), TASK_TIME.getLong(a)));
        LOGGER.info("Tasks:");
        for (String task : sortedTasks) {
            LOGGER.info("  %s: %.3fms", task, TASK_TIME.getLong(task) / 1e6);
        }
        int cachedPresentCount = 0;
        int cachedAbsentCount = 0;
        long cacheSize = 0;
        String biggestClass = null;
        int biggestSize = 0;
        for (LitemodClassProvider instance : INSTANCES.keySet()) {
            for (Map.Entry<String, byte[]> e : instance.classByteCache.entrySet()) {
                byte[] bytes = e.getValue();
                if (bytes.length > biggestSize) {
                    biggestClass = e.getKey();
                    biggestSize = bytes.length;
                }
                cacheSize += 4 + bytes.length;
                if (bytes == NOT_PRESENT_BYTES) {
                    cachedAbsentCount++;
                } else {
                    cachedPresentCount++;
                }
            }
        }
        LOGGER.info("%d cached present classes, %d absent, %.3fMiB", cachedPresentCount, cachedAbsentCount, cacheSize / (1024.0 * 1024.0));
        LOGGER.info("Biggest class: %s, %d bytes", biggestClass, biggestSize);
    }
}
