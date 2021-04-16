package de.skyrising.litefabric.impl;

import net.fabricmc.api.ClientModInitializer;

public class ClientInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LiteFabric.getInstance().onClientInit();
    }
}
