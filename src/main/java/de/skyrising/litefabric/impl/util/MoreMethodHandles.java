package de.skyrising.litefabric.impl.util;

import sun.invoke.util.Wrapper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.List;

import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.methodType;

public final class MoreMethodHandles {
    private MoreMethodHandles() {}

    public static MethodHandle zero(Class<?> type) {
        return (type == void.class
            ? zero(int.class)
            : constant(type, Wrapper.forPrimitiveType(type).zero())
        ).asType(methodType(type));
    }

    public static MethodHandle empty(MethodType type) {
        return dropArguments(zero(type.returnType()), 0, type.parameterList());
    }

    public static MethodHandle dropReturn(MethodHandle handle) {
        return handle.asType(methodType(void.class, handle.type().parameterArray()));
    }

    public static MethodHandle chain(MethodHandle first, MethodHandle second) {
        return foldArguments(dropReturn(second), dropReturn(first));
    }

    public static MethodHandle chain(List<MethodHandle> handles) {
        int size = handles.size();
        if (size == 0) throw new IllegalArgumentException();
        if (size == 1) return dropReturn(handles.get(0));
        if (size == 2) return chain(handles.get(0), handles.get(1));
        return chain(chain(handles.subList(0, size / 2)), chain(handles.subList(size / 2, size)));
    }
}
