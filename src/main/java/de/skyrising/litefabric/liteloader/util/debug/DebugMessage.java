package de.skyrising.litefabric.liteloader.util.debug;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class DebugMessage {
    public enum Position {LEFT_TOP, LEFT_AFTER_INFO, LEFT_BOTTOM, RIGHT_TOP, RIGHT_BOTTOM}

    private static final Map<Position, List<DebugMessage>> MESSAGES = new EnumMap<>(Position.class);
    private final Position position;
    private String message;
    private boolean visible = true;

    private DebugMessage(Position pos, String message) {
        this.position = pos;
        this.message = message;
    }

    public DebugMessage setMessage(String message) {
        this.message = message;
        return this;
    }

    public DebugMessage setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public boolean isVisible() {
        return visible;
    }

    public void remove() {
        List<DebugMessage> messagesAtPos = MESSAGES.get(position);
        if (messagesAtPos == null) return;
        messagesAtPos.remove(this);
        if (messagesAtPos.isEmpty()) MESSAGES.put(position, null);
    }

    @Override
    public String toString() {
        return message == null ? "" : message;
    }

    public static DebugMessage create(Position pos, String message) {
        if (pos == null) throw new NullPointerException("pos");
        DebugMessage msg = new DebugMessage(pos, message);
        MESSAGES.computeIfAbsent(pos, k -> new ArrayList<>()).add(msg);
        return msg;
    }

    public static List<String> getMessages(Position pos) {
        List<DebugMessage> messages = MESSAGES.get(pos);
        if (messages == null) return null;
        ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
        for (DebugMessage message : messages) {
            if (message.isVisible()) builder.add(message.toString());
        }
        return builder.build();
    }
}
