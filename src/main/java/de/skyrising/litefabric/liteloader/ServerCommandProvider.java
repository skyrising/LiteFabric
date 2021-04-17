package de.skyrising.litefabric.liteloader;

import net.minecraft.server.command.ServerCommandManager;

public interface ServerCommandProvider extends LiteMod {
    void provideCommands(ServerCommandManager manager);
}
