package de.skyrising.litefabric.impl.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.extensibility.IRemapper;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.IClassReferenceMapper;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;

import java.util.HashMap;
import java.util.Map;

public final class RemappingReferenceMapper implements IClassReferenceMapper, IReferenceMapper {
    private static final Logger LOGGER = LogManager.getLogger();
    final static ThreadLocal<String> targetClass = new ThreadLocal<>();

    private final IReferenceMapper refMap;

    private final IRemapper remapper;

    private final Map<String, String> mappedReferenceCache = new HashMap<String, String>();

    public RemappingReferenceMapper(IReferenceMapper refMap, IRemapper remapper) {
        this.refMap = refMap;
        this.remapper = remapper;
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
        System.out.println("remapClassName(" + className + ", " + remapped + ")");
        String origInfoString;
        if (this.refMap instanceof IClassReferenceMapper) {
            origInfoString = ((IClassReferenceMapper) this.refMap).remapClassNameWithContext(context, className, remapped);
        } else {
            origInfoString = this.refMap.remapWithContext(context, className, remapped);
        }
        return remapper.map(origInfoString.replace('.', '/'));
    }
}
