package de.skyrising.litefabric.liteloader;

import de.skyrising.litefabric.liteloader.core.LiteLoaderEventBroker;
import net.minecraft.text.Text;

public interface ChatFilter {
    boolean onChat(Text chat, String message, LiteLoaderEventBroker.ReturnValue<Text> newMessage);
}
