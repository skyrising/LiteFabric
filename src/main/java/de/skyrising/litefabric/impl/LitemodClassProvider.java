package de.skyrising.litefabric.impl;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.*;

class LitemodClassProvider {
    private static final boolean DUMP = true;

    private final FileSystem fileSystem;
    private final LitemodRemapper remapper;

    public LitemodClassProvider(FileSystem fileSystem, LitemodRemapper remapper) {
        this.fileSystem = fileSystem;
        this.remapper = remapper;
    }

    byte[] getClassBytes(String name) {
        String classFileName = name.replace('.', '/') + ".class";
        Path classFilePath = fileSystem.getPath(classFileName);
        if (!Files.exists(classFilePath)) return null;
        byte[] rawBytes;
        try {
            rawBytes = Files.readAllBytes(classFilePath);
        } catch (IOException e) {
            return null;
        }
        ClassReader reader = new ClassReader(rawBytes);
        ClassWriter writer = new ClassWriter(reader, Opcodes.ASM9);
        ClassNode raw = new ClassNode();
        reader.accept(raw, ClassReader.EXPAND_FRAMES);
        remapper.addClass(raw);
        ClassNode remapped = new ClassNode();
        ClassRemapper clsRemapper = new ClassRemapper(remapped, remapper);
        raw.accept(clsRemapper);
        remapped.accept(writer);
        byte[] bytes = writer.toByteArray();
        if (DUMP) {
            Path out = Paths.get(".litefabric.out/class/" + classFileName);
            try {
                Files.createDirectories(out.getParent());
                Files.write(out, bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bytes;
    }

    ClassNode getClassNode(String name) {
        byte[] bytes = getClassBytes(name);
        if (bytes == null) return null;
        ClassReader reader = new ClassReader(bytes);
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        return node;
    }

    URL findResource(String name) {
        try {
            if (name.endsWith(".class")) {
                Path tmpPath = LiteFabric.TMP_FILES.getPath(name);
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
                if (DUMP) {
                    Path out = Paths.get(".litefabric.out/resource/" + name);
                    Files.createDirectories(out.getParent());
                    Files.copy(path, out, StandardCopyOption.REPLACE_EXISTING);
                }
                return path.toUri().toURL();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }
}
