package de.skyrising.litefabric.impl.modconfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import de.skyrising.litefabric.liteloader.modconfig.Exposable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

class ConfigHandler implements InstanceCreator<Exposable> {
    private static final int DEBOUNCE_DELAY = 1000;
    private final Exposable exposable;
    private final Path path;
    private final boolean aggressive;
    private final Gson gson;
    private volatile boolean dirty = false;
    private volatile long lastWrite;

    public ConfigHandler(Exposable exposable, Path path, boolean aggressive) {
        this.exposable = exposable;
        this.path = path;
        this.aggressive = aggressive;
        GsonBuilder builder = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapter(exposable.getClass(), this);
        this.gson = builder.create();
    }

    @Override
    public Exposable createInstance(Type type) {
        return exposable;
    }

    public synchronized void init() {
        read();
        if (Files.notExists(path)) {
            write();
        }
    }

    private synchronized void read() {
        if (!Files.exists(path)) return;
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            gson.fromJson(reader, exposable.getClass());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void write() {
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                gson.toJson(exposable, writer);
                dirty = false;
                lastWrite = System.currentTimeMillis();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void invalidate() {
        if (canWrite()) {
            write();
        } else {
            dirty = true;
        }
    }

    public void tick() {
        if (dirty && canWrite()) {
            write();
        }
    }

    private boolean canWrite() {
        return aggressive || (System.currentTimeMillis() - lastWrite) > DEBOUNCE_DELAY;
    }
}
