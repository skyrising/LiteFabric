package de.skyrising.litefabric.impl.util;

import de.skyrising.litefabric.liteloader.util.Input;
import net.minecraft.client.options.KeyBinding;

public class InputImpl extends Input {
    @Override
    public void registerKeyBinding(KeyBinding binding) {
        System.out.println("registerKeyBinding(" + binding + ")");
    }

    @Override
    public void unRegisterKeyBinding(KeyBinding binding) {
        System.out.println("unRegisterKeyBinding(" + binding + ")");
    }
}
