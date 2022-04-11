package de.skyrising.litefabric.liteloader;

public interface OutboundChatFilter {
    boolean onSendChatMessage(String message);
}
