package de.skyrising.litefabric.impl.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.ArrayList;
import java.util.List;

public class ListenerHandle<T> {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    public final MutableCallSite callSite;
    private final MethodHandle unbound;
    private final List<MethodHandle> listeners = new ArrayList<>();
    private boolean initialized;

    public ListenerHandle(Class<T> type, String name, MethodType methodType) throws NoSuchMethodException, IllegalAccessException {
        unbound = LOOKUP.findVirtual(type, name, methodType);
        callSite = new MutableCallSite(MoreMethodHandles.empty(methodType));
    }

    public void addListener(T listener) {
        listeners.add(unbound.bindTo(listener));
        if (initialized) updateCallSite();
    }

    private void updateCallSite() {
        if (listeners.isEmpty()) return;
        callSite.setTarget(MoreMethodHandles.chain(listeners));
    }

    public void init() {
        updateCallSite();
        initialized = true;
    }
}
