package de.skyrising.litefabric.impl.util;

import de.skyrising.litefabric.liteloader.util.Input;
import de.skyrising.litefabric.mixin.KeyBindingAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class InputImpl extends Input {
    private static final Logger LOGGER = LogManager.getLogger("LiteFabric|Input");
    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("liteloader.keys.properties");
    private final Set<KeyBinding> keyBindings = new LinkedHashSet<>();
    private final Object2IntMap<String> storedKeys = new Object2IntOpenHashMap<>();
    private boolean loaded;

    @Override
    public void registerKeyBinding(KeyBinding binding) {
        GameOptions options = MinecraftClient.getInstance().options;
        List<KeyBinding> keyBindings = new ArrayList<>(Arrays.asList(options.keysAll));
        if (!keyBindings.contains(binding)) {
            String id = binding.getId();
            if (storedKeys.containsKey(id)) {
                binding.setKeyCode(storedKeys.getInt(id));
            }
            keyBindings.add(binding);
            this.keyBindings.add(binding);
            options.keysAll = keyBindings.toArray(new KeyBinding[0]);
            String category = binding.getCategory();
            Map<String, Integer> map = KeyBindingAccessor.getCategoryMap();
            if (!map.containsKey(category)) map.put(category, 1000 + map.size());
            KeyBinding.updateKeysByCode();
            save();
        }
    }

    @Override
    public void unRegisterKeyBinding(KeyBinding binding) {
        GameOptions options = MinecraftClient.getInstance().options;
        List<KeyBinding> keyBindings = new ArrayList<>(Arrays.asList(options.keysAll));
        if (this.keyBindings.remove(binding)) {
            keyBindings.remove(binding);
            options.keysAll = keyBindings.toArray(new KeyBinding[0]);
            KeyBinding.updateKeysByCode();
        }
    }

    public void onTick() {
        boolean changed = false;
        for (KeyBinding k : keyBindings) {
            if (k.getKeyCode() != storedKeys.getInt(k.getId())) {
                changed = true;
                break;
            }
        }
        if (changed) save();
    }

    public void load() {
        loaded = true;
        if (!Files.exists(file)) return;
        Properties props = new Properties();
        try {
            props.load(Files.newBufferedReader(file));
            for (String id : props.stringPropertyNames()) {
                storedKeys.put(id, Integer.parseInt(props.getProperty(id)));
            }
        } catch (IOException e) {
            LOGGER.warn("Could not load key bindings", e);
        }
    }

    public void save() {
        if (!loaded) return;
        Properties props = new Properties();
        for (KeyBinding k : keyBindings) {
            String id = k.getId();
            int key = k.getKeyCode();
            props.setProperty(id, String.valueOf(key));
            storedKeys.put(id, key);
        }
        try {
            props.store(
                Files.newBufferedWriter(file),
                "Mod key mappings for LiteLoader mods, stored here to avoid losing settings stored in options.txt"
            );
        } catch (IOException e) {
            LOGGER.warn("Could not store key bindings", e);
        }
    }
}
