package de.skyrising.litefabric.impl.modconfig;

import de.skyrising.litefabric.liteloader.LiteMod;
import de.skyrising.litefabric.liteloader.modconfig.ConfigStrategy;
import de.skyrising.litefabric.liteloader.modconfig.Exposable;
import de.skyrising.litefabric.liteloader.modconfig.ExposableOptions;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class ConfigManager {
    private final Map<Exposable, ConfigHandler> handlers = new HashMap<>();

    public void registerMod(LiteMod mod) {
        this.registerExposable(mod, null, false);
    }

    public void registerExposable(Exposable exposable, String fileName, boolean ignoreMissingOptions) {
        ExposableOptions options = exposable.getClass().getAnnotation(ExposableOptions.class);
        if (options != null) {
            if (fileName == null) fileName = options.filename();
            initHandler(exposable, fileName, options.strategy(), options.aggressive());
        } else if (ignoreMissingOptions) {
            initHandler(exposable, fileName, ConfigStrategy.Versioned, false);
        }
    }

    private void initHandler(Exposable exposable, String fileName, ConfigStrategy strategy, boolean aggressive) {
        if (handlers.containsKey(exposable)) return;
        if (fileName == null || fileName.isEmpty()) {
            fileName = exposable.getClass().getSimpleName().toLowerCase(Locale.ROOT);
            if (fileName.startsWith("litemod")) {
                fileName = fileName.substring(7);
            }
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".json")) {
            fileName += ".json";
        }
        Path path = strategy.getPathForStrategy(fileName);
        handlers.put(exposable, new ConfigHandler(exposable, path, aggressive));
    }

    private void ifConfig(Exposable exposable, Consumer<ConfigHandler> onPresent) {
        ConfigHandler handler = handlers.get(exposable);
        if (handler != null) onPresent.accept(handler);
    }

    public void initConfig(Exposable exposable) {
        ifConfig(exposable, ConfigHandler::init);
    }

    public void invalidateConfig(Exposable exposable) {
        ifConfig(exposable, ConfigHandler::invalidate);
    }

    public void tick() {
        for (ConfigHandler handler : handlers.values()) {
            handler.tick();
        }
    }
}
