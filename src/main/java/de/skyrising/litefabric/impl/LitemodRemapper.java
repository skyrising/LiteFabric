package de.skyrising.litefabric.impl;

import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.mapping.tree.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.spongepowered.asm.mixin.extensibility.IRemapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class LitemodRemapper extends Remapper implements IRemapper {
    private static final String SOURCE_NAMESPACE = "official";
    private final Map<String, ClassDef> classes = new HashMap<>();
    private final Map<String, String> classesReverse = new HashMap<>();
    private final Map<String, Set<String>> superClasses = new HashMap<>();
    private final Map<String, Map<String, FieldDef>> fields = new HashMap<>();
    private final Map<String, Map<String, MethodDef>> methods = new HashMap<>();
    private final Map<String, Set<String>> shadowFields = new HashMap<>();
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

    public Set<String> addClass(ClassNode node) {
        LinkedHashSet<String> superClasses = new LinkedHashSet<>();
        String superName = node.superName;
        if (node.superName != null) superClasses.add(classesReverse.getOrDefault(superName, superName));
        for (String itfName : node.interfaces) {
            superClasses.add(classesReverse.getOrDefault(itfName, itfName));
        }
        if (node.invisibleAnnotations != null) {
            for (AnnotationNode classAnnotation : node.invisibleAnnotations) {
                if (!"Lorg/spongepowered/asm/mixin/Mixin;".equals(classAnnotation.desc)) continue;
                Set<String> shadowFields = new HashSet<>();
                for (FieldNode field : node.fields) {
                    if (field.visibleAnnotations == null) continue;
                    for (AnnotationNode fieldAnnotation : field.visibleAnnotations) {
                        if (!"Lorg/spongepowered/asm/mixin/Shadow;".equals(fieldAnnotation.desc)) continue;
                        shadowFields.add(field.name);
                    }
                }
                this.shadowFields.put(node.name, shadowFields);
                break;
            }
        }
        return addSuperClassMapping(node.name, superClasses);
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
        if (isUnmappedField(owner)) return null;
        boolean knownClass = classes.containsKey(owner);
        if (!knownClass && classesReverse.containsKey(owner)) {
            knownClass = true;
            owner = unmap(owner);
            descriptor = unmapDesc(descriptor);
        }
        // don't traverse super classes for @Shadow fields
        if (!knownClass && shadowFields.containsKey(owner)) {
            if (shadowFields.get(owner).contains(name)) return null;
        }
        if (knownClass) {
            Map<String, FieldDef> fieldMap = fields.computeIfAbsent(owner, this::computeFields);
            if (fieldMap != null) {
                FieldDef fieldDef = fieldMap.get(name + descriptor);
                if (fieldDef != null) return fieldDef.getName(targetNamespace);
            }
        }
        return mapFieldNameFromSupers(owner, name, descriptor);
    }

    private boolean isUnmappedField(String owner) {
        return owner == null || owner.isEmpty() || owner.charAt(0) == '[';
    }

    private String mapFieldNameFromSupers(String owner, String name, String descriptor) {
        Set<String> superClassNames = getSuperClasses(owner);
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
        return name;
    }

    private Map<String, MethodDef> computeMethods(String clsName) {
        ClassDef clsDef = classes.get(clsName);
        if (clsDef == null) return null;
        return computeDescriptored(clsDef.getMethods());
    }

    private String mapMethodName0(String owner, String name, String descriptor) {
        if (isUnmappedMethod(owner, name)) return null;
        boolean knownClass = classes.containsKey(owner);
        if (!knownClass && classesReverse.containsKey(owner)) {
            knownClass = true;
            owner = unmap(owner);
            descriptor = unmapDesc(descriptor);
        }
        if (knownClass) {
            Map<String, MethodDef> methodMap = methods.computeIfAbsent(owner, this::computeMethods);
            if (methodMap != null) {
                MethodDef methodDef = methodMap.get(name + descriptor);
                if (methodDef != null) return methodDef.getName(targetNamespace);
            }
        }
        return mapMethodNameFromSupers(owner, name, descriptor);
    }

    private static boolean isUnmappedMethod(String owner, String name) {
        return name.isEmpty() || name.charAt(0) == '<' || owner == null || owner.isEmpty() || owner.charAt(0) == '[';
    }

    private String mapMethodNameFromSupers(String owner, String name, String descriptor) {
        Set<String> superClassNames = getSuperClasses(owner);
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
        return name;
    }

    private Set<String> getSuperClasses(String cls) {
        if (cls == null) return Collections.emptySet();
        if (superClasses.containsKey(cls)) return superClasses.get(cls);
        InputStream in = FabricLauncherBase.getLauncher().getResourceAsStream(map(cls) + ".class");
        if (in == null) {
            return addSuperClassMapping(cls, Collections.emptySet());
        }
        try {
            ClassReader reader = new ClassReader(in);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
            return addClass(node);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return addSuperClassMapping(cls, Collections.emptySet());
    }

    private Set<String> addSuperClassMapping(String cls, Set<String> supers) {
        this.superClasses.put(cls, supers);
        return supers;
    }

    @Override
    public String unmap(String typeName) {
        if (!classesReverse.containsKey(typeName)) return typeName;
        return classesReverse.get(typeName);
    }

    @Override
    public String unmapDesc(String old) {
        if (old == null) return null;
        int lastL = old.indexOf('L');
        int lastSemi = -1;
        if (lastL < 0) return old;
        int oldLength = old.length();
        StringBuilder builder = new StringBuilder(oldLength + oldLength / 4);
        while (lastL >= 0) {
            if (lastSemi + 1 < lastL) builder.append(old, lastSemi + 1, lastL);

            lastSemi = old.indexOf(';', lastL + 1);
            if (lastSemi == -1) return old;

            builder.append('L').append(unmap(old.substring(lastL + 1, lastSemi))).append(';');
            lastL = old.indexOf('L', lastSemi + 1);
        }

        if (lastSemi + 1 < old.length()) builder.append(old, lastSemi + 1, old.length());

        return builder.toString();
    }
}
