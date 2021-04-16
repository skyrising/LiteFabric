package de.skyrising.litefabric.impl;

import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.mapping.tree.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IRemapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class LitemodRemapper extends Remapper implements IRemapper {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SOURCE_NAMESPACE = "official";
    private final Map<String, ClassDef> classes = new HashMap<>();
    private final Map<String, String> classesReverse = new HashMap<>();
    private final Map<String, Set<String>> superClasses = new HashMap<>();
    private final Map<String, Map<String, FieldDef>> fields = new HashMap<>();
    private final Map<String, Map<String, MethodDef>> methods = new HashMap<>();
    private final String targetNamespace;

    public LitemodRemapper(TinyTree mappings, String targetNamespace) {
        this.targetNamespace = targetNamespace;
        for (ClassDef cls : mappings.getClasses()) {
            String clsName = cls.getName(SOURCE_NAMESPACE);
            String targetName = cls.getName(targetNamespace);
            classes.put(clsName, cls);
            classesReverse.put(targetName, clsName);
        }
    }

    @Override
    public String map(String internalName) {
        if (internalName.startsWith("com/mumfrey/liteloader/")) {
            return "de/skyrising/litefabric/liteloader/" + internalName.substring(23);
        }
        ClassDef def = classes.get(internalName);
        if (def != null) return def.getName(targetNamespace);
        return internalName;
    }

    private <T extends Descriptored> Map<String, T> computeDescriptored(Collection<T> collection) {
        Map<String, T> map = new HashMap<>();
        for (T def : collection) {
            String key = def.getName(SOURCE_NAMESPACE) + def.getDescriptor(SOURCE_NAMESPACE);
            map.put(key, def);
        }
        return map;
    }

    private Map<String, FieldDef> computeFields(String clsName) {
        ClassDef clsDef = classes.get(clsName);
        if (clsDef == null) return null;
        return computeDescriptored(clsDef.getFields());
    }

    private String mapFieldName0(String owner, String name, String descriptor) {
        if (owner == null) throw new NullPointerException("Are you stupid, Mixin?");
        if (!classes.containsKey(owner) && classesReverse.containsKey(owner)) {
            owner = unmap(owner);
            descriptor = unmapDesc(descriptor);
        }
        Map<String, FieldDef> fieldMap = fields.computeIfAbsent(owner, this::computeFields);
        if (fieldMap != null) {
            FieldDef fieldDef = fieldMap.get(name + descriptor);
            if (fieldDef != null) return fieldDef.getName(targetNamespace);
        }
        Set<String> superClassNames = superClasses.computeIfAbsent(owner, this::computeSuperClasses);
        for (String superClass : superClassNames) {
            String superMap = mapFieldName0(superClass, name, descriptor);
            if (superMap != null) return superMap;
        }
        return null;
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        String mapped = mapFieldName0(owner, name, descriptor);
        if (mapped != null) return mapped;
        //LOGGER.warn("No mapping for {} ({}) {}{}", classes.get(owner).getName(targetNamespace), owner, name, descriptor);
        return name;
    }

    private Map<String, MethodDef> computeMethods(String clsName) {
        ClassDef clsDef = classes.get(clsName);
        if (clsDef == null) return null;
        return computeDescriptored(clsDef.getMethods());
    }

    private String mapMethodName0(String owner, String name, String descriptor) {
        if (!classes.containsKey(owner) && classesReverse.containsKey(owner)) {
            owner = unmap(owner);
            descriptor = unmapDesc(descriptor);
        }
        if (name.startsWith("<")) return name;
        Map<String, MethodDef> methodMap = methods.computeIfAbsent(owner, this::computeMethods);
        if (methodMap != null) {
            MethodDef methodDef = methodMap.get(name + descriptor);
            if (methodDef != null) return methodDef.getName(targetNamespace);
        }
        Set<String> superClassNames = superClasses.computeIfAbsent(owner, this::computeSuperClasses);
        for (String superClass : superClassNames) {
            String superMap = mapMethodName0(superClass, name, descriptor);
            if (superMap != null) return superMap;
        }
        return null;
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        String mapped = mapMethodName0(owner, name, descriptor);
        if (mapped != null) return mapped;
        //LOGGER.warn("No mapping for {} ({}) {}{}", classes.get(owner).getName(targetNamespace), owner, name, descriptor);
        return name;
    }

    private Set<String> computeSuperClasses(String cls) {
        LinkedHashSet<String> superClasses = new LinkedHashSet<>();
        InputStream in = FabricLauncherBase.getLauncher().getResourceAsStream(map(cls) + ".class");
        if (in == null) return superClasses;
        try {
            ClassReader reader = new ClassReader(in);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
            String superName = node.superName;
            if (node.superName != null) superClasses.add(classesReverse.getOrDefault(superName, superName));
            for (String itfName : node.interfaces) {
                superClasses.add(classesReverse.getOrDefault(itfName, itfName));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return superClasses;
    }

    @Override
    public String unmap(String typeName) {
        if (!classesReverse.containsKey(typeName)) return typeName;
        return classesReverse.get(typeName);
    }

    @Override
    public String unmapDesc(String old) {
        int lastL = old.indexOf(76);
        int lastSemi = -1;
        if (lastL < 0) {
            return old;
        } else {
            StringBuilder builder;
            for(builder = new StringBuilder((int)((double)old.length() * 1.2D)); lastL >= 0; lastL = old.indexOf(76, lastSemi + 1)) {
                if (lastSemi + 1 < lastL) {
                    builder.append(old, lastSemi + 1, lastL);
                }

                lastSemi = old.indexOf(59, lastL + 1);
                if (lastSemi == -1) {
                    return old;
                }

                builder.append('L').append(unmap(old.substring(lastL + 1, lastSemi))).append(';');
            }

            if (lastSemi + 1 < old.length()) {
                builder.append(old, lastSemi + 1, old.length());
            }

            return builder.toString();
        }
    }
}
