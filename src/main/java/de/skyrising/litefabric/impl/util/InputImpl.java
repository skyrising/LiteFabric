package de.skyrising.litefabric.impl.util;

import de.skyrising.litefabric.liteloader.util.Input;
import de.skyrising.litefabric.mixin.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class InputImpl extends Input {
    @Override
    public void registerKeyBinding(KeyBinding binding) {
        GameOptions options = MinecraftClient.getInstance().options;
        List<KeyBinding> keyBindings = new ArrayList<>(Arrays.asList(options.keysAll));
        if (!keyBindings.contains(binding)) {
            keyBindings.add(binding);
            options.keysAll = keyBindings.toArray(new KeyBinding[0]);
            String category = binding.getCategory();
            Map<String, Integer> map = KeyBindingAccessor.getCategoryMap();
            if (!map.containsKey(category)) map.put(category, 1000 + map.size());
            KeyBinding.updateKeysByCode();
        }
    }

    @Override
    public void unRegisterKeyBinding(KeyBinding binding) {
        System.out.println("unRegisterKeyBinding(" + binding + ")");
    }
}
