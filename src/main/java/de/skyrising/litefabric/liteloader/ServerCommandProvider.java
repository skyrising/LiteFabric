package de.skyrising.litefabric.liteloader;

import net.minecraft.server.command.CommandManager;

public interface ServerCommandProvider extends LiteMod {
    void provideCommands(CommandManager manager);
}
