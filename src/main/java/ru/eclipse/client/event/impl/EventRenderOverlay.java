package ru.eclipse.client.event.impl;

import ru.eclipse.client.event.Event;

public class EventRenderOverlay extends Event {
    public enum OverlayType {
        FIRE,
        IN_WALL,
        UNDERWATER,
        PUMPKIN
    }

    private final OverlayType type;

    public EventRenderOverlay(OverlayType type) {
        this.type = type;
    }

    public OverlayType getType() {
        return type;
    }
}
