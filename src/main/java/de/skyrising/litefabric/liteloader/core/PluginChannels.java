package de.skyrising.litefabric.liteloader.core;

import de.skyrising.litefabric.liteloader.CommonPluginChannelListener;
import net.minecraft.util.PacketByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class PluginChannels<T extends CommonPluginChannelListener> {
    private static final Logger LOGGER = LogManager.getLogger("LiteFabric|PluginChannels");
    protected final HashMap<String, List<T>> pluginChannels = new HashMap<>();
    protected final Set<String> remotePluginChannels = new HashSet<>();
    protected final List<T> listeners = new ArrayList<>();

    protected void addPluginChannelsFor(T listener) {
        List<String> channels = listener.getChannels();
        if (channels == null) return;
        for (String channel : channels) {
            if (!isValidChannelName(channel)) continue;
            pluginChannels.computeIfAbsent(channel, k -> new ArrayList<>()).add(listener);
        }
    }

    public Set<String> getLocalChannels() {
        return Collections.unmodifiableSet(pluginChannels.keySet());
    }

    public Set<String> getRemoteChannels() {
        return Collections.unmodifiableSet(remotePluginChannels);
    }

    public boolean isRemoteChannelRegistered(String channel) {
        return remotePluginChannels.contains(channel);
    }

    protected void addPluginChannelListener(T listener) {
        listeners.add(listener);
    }

    protected void onRegisterPacketReceived(PacketByteBuf data) {
        try {
            byte[] bytes = new byte[data.readableBytes()];
            data.readBytes(bytes);
            String channels = new String(bytes, StandardCharsets.UTF_8);
            List<String> newChannels = Arrays.asList(channels.split("\u0000"));
            LOGGER.info("Received REGISTER for {}", newChannels);
            remotePluginChannels.addAll(newChannels);
        } catch (Exception e) {
            LOGGER.warn("Error decoding register packet", e);
        }
    }

    protected static boolean isValidChannelName(String channel) {
        if (channel == null) return false;
        int len = channel.length();
        if (len == 0 || len > 20) return false;
        if (channel.equalsIgnoreCase("REGISTER")) return false;
        return !channel.equalsIgnoreCase("UNREGISTER");
    }

    public enum ChannelPolicy {
        DISPATCH,
        DISPATCH_IF_REGISTERED,
        DISPATCH_ALWAYS;

        public boolean check(PluginChannels<?> channels, String channel) {
            if (this == DISPATCH_ALWAYS) return true;
            if (channels.isRemoteChannelRegistered(channel)) return true;
            if (this == DISPATCH) throw new IllegalStateException("Channel '" + channel + "' is not registered");
            return false;
        }
    }

}
