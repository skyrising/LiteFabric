package de.skyrising.litefabric.impl;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.security.CodeSource;
import java.security.SecureClassLoader;

public class LitemodClassLoader extends SecureClassLoader {
    private final FileSystem fileSystem;
    private final LitemodRemapper remapper;
    private final CodeSource codeSource;

    public LitemodClassLoader(ClassLoader parent, FileSystem fileSystem, LitemodRemapper remapper, CodeSource codeSource) {
        super(parent);
        this.fileSystem = fileSystem;
        this.remapper = remapper;
        this.codeSource = codeSource;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = selfLoad(name);
            if (c == null) {
                c = LiteFabric.getInstance().combinedClassLoader.loadFromSiblings(name, this);
            }

            if (c == null) {
                c = getParent().loadClass(name);
            }

            if (resolve) {
                resolveClass(c);
            }

            return c;
        }
    }

    Class<?> selfLoad(String name) {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                byte[] bytes = getClassBytes(name);
                if (bytes != null) {
                    int pkgDelimiterPos = name.lastIndexOf('.');
                    if (pkgDelimiterPos > 0) {
                        String pkgString = name.substring(0, pkgDelimiterPos);
                        if (getPackage(pkgString) == null) {
                            definePackage(pkgString, null, null, null, null, null, null, null);
                        }
                    }
                    return defineClass(name, bytes, 0, bytes.length, codeSource);
                }
            }
            return c;
        }
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
        ClassNode remapped = new ClassNode();
        ClassRemapper clsRemapper = new ClassRemapper(remapped, remapper);
        reader.accept(clsRemapper, ClassReader.EXPAND_FRAMES);
        //System.out.println(remapped.name);
        //for (MethodNode m : remapped.methods) {
        //    System.out.println(m.name + " " + m.desc);
        //}
        remapped.accept(writer);
        byte[] bytes = writer.toByteArray();
        Path out = Paths.get(".litefabric.out/class/" + classFileName);
        try {
            Files.createDirectories(out.getParent());
            Files.write(out, bytes);
        } catch (IOException e) {
            e.printStackTrace();
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

    URL findOwnResource(String name) {
        Path path = fileSystem.getPath(name);
        if (!Files.exists(path)) return null;
        try {
            Path out = Paths.get(".litefabric.out/class/" + name);
            Files.createDirectories(out.getParent());
            Files.copy(path, out, StandardCopyOption.REPLACE_EXISTING);
            return path.toUri().toURL();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected URL findResource(String name) {
        URL own = findOwnResource(name);
        if (own != null) return own;
        return LiteFabric.getInstance().combinedClassLoader.getResourceFromSiblings(name, this);
    }

    static {
        registerAsParallelCapable();
    }
}
