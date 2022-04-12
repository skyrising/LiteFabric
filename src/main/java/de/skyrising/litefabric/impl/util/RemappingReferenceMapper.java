package de.skyrising.litefabric.impl.util;

import de.skyrising.litefabric.impl.LiteFabric;
import de.skyrising.litefabric.impl.LitemodMixinService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IRemapper;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.IClassReferenceMapper;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public abstract class RemappingReferenceMapper implements IClassReferenceMapper, IReferenceMapper {
    private static final Logger LOGGER = LogManager.getLogger();
    final static ThreadLocal<String> targetClass = new ThreadLocal<>();

    private final IReferenceMapper refMap;

    private final IRemapper remapper;

    private final Map<String, String> mappedReferenceCache = new HashMap<>();

    public static Class<?> createClassFor(String name, LitemodMixinService service) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(V1_8, ACC_PUBLIC, name.replace('.', '/'), null, Type.getInternalName(RemappingReferenceMapper.class), null);
        writer.visitField(ACC_PUBLIC | ACC_STATIC, "SERVICE", Type.getDescriptor(LitemodMixinService.class), null, null).visitEnd();
        MethodVisitor init = writer.visitMethod(ACC_PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(MixinEnvironment.class), Type.getType(IReferenceMapper.class)), null, null);
        init.visitCode();
        init.visitVarInsn(ALOAD, 0);
        init.visitFieldInsn(GETSTATIC, name.replace('.', '/'), "SERVICE", Type.getDescriptor(LitemodMixinService.class));
        init.visitVarInsn(ALOAD, 2);
        init.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(RemappingReferenceMapper.class), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(LitemodMixinService.class), Type.getType(IReferenceMapper.class)), false);
        init.visitInsn(RETURN);
        init.visitMaxs(3, 3);
        init.visitEnd();
        byte[] bytes = writer.toByteArray();
        Class<?> cls = LiteFabric.getInstance().combinedClassLoader.defineClass(name, bytes);
        try {
            cls.getField("SERVICE").set(null, service);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return cls;
    }

    protected RemappingReferenceMapper(LitemodMixinService service, IReferenceMapper refMap) {
        this.refMap = service.getReferenceMapper(refMap.getResourceName());
        this.remapper = LiteFabric.getInstance().getRemapper();
    }

    @Override
    public boolean isDefault() {
        return this.refMap.isDefault();
    }

    @Override
    public String getResourceName() {
        return this.refMap.getResourceName();
    }

    @Override
    public String getStatus() {
        return this.refMap.getStatus();
    }

    @Override
    public String getContext() {
        return this.refMap.getContext();
    }

    @Override
    public void setContext(String context) {
        this.refMap.setContext(context);
    }

    @Override
    public String remap(String className, String reference) {
        return this.remapWithContext(getContext(), className, reference);
    }

    private static String remapMethodDescriptor(IRemapper remapper, String desc) {
        StringBuilder newDesc = new StringBuilder();
        newDesc.append('(');
        for (Type arg : Type.getArgumentTypes(desc)) {
            newDesc.append(remapper.mapDesc(arg.getDescriptor()));
        }
        return newDesc.append(')').append(remapper.mapDesc(Type.getReturnType(desc).getDescriptor())).toString();
    }

    @Override
    public String remapWithContext(String context, String className, String reference) {
        if (reference.isEmpty()) {
            return reference;
        }

        boolean cache = true;
        String origInfoString = this.refMap.remapWithContext(context, className, reference);
        String remappedCached = mappedReferenceCache.get(origInfoString);
        if (remappedCached != null) {
            return remappedCached;
        } else {
            String remapped = origInfoString;

            // To handle propagation, find super/itf-class (for IRemapper)
            // but pass the requested class in the MemberInfo
            MemberInfo info = MemberInfo.parse(remapped, null);
            if (info.getOwner() == null) {
                String target = targetClass.get();
                LOGGER.debug("No owner: {}, guessing {}", info, target);
                if (target != null) {
                    info = (MemberInfo) info.move(target);
                    cache = false;
                }
            }
            if (info.getName() == null && info.getDesc() == null) {
                return info.getOwner() != null ? new MemberInfo(remapper.map(info.getOwner()), null).toString() : info.toString();
            } else if (info.isField()) {
                remapped = new MemberInfo(
                        remapper.mapFieldName(info.getOwner(), info.getName(), info.getDesc()),
                        info.getOwner() == null ? null : remapper.map(info.getOwner()),
                        info.getDesc() == null ? null : remapper.mapDesc(info.getDesc())
                ).toString();
            } else {
                remapped = new MemberInfo(
                        remapper.mapMethodName(info.getOwner(), info.getName(), info.getDesc()),
                        info.getOwner() == null ? null : remapper.map(info.getOwner()),
                        info.getDesc() == null ? null : remapMethodDescriptor(remapper, info.getDesc())
                ).toString();
            }

            if (cache) mappedReferenceCache.put(origInfoString, remapped);
            return remapped;
        }
    }

    @Override
    public String remapClassName(String className, String inputClassName) {
        return remapClassNameWithContext(getContext(), className, inputClassName);
    }

    @Override
    public String remapClassNameWithContext(String context, String className, String remapped) {
        String origInfoString;
        if (this.refMap instanceof IClassReferenceMapper) {
            origInfoString = ((IClassReferenceMapper) this.refMap).remapClassNameWithContext(context, className, remapped);
        } else {
            origInfoString = this.refMap.remapWithContext(context, className, remapped);
        }
        return remapper.map(origInfoString.replace('.', '/'));
    }
}
