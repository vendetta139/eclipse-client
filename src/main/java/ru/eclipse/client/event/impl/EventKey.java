package ru.eclipse.client.event.impl;

import ru.eclipse.client.event.Event;

public class EventKey extends Event {
    private final int key;
    private final int action;
    private final int modifiers;

    public EventKey(int key, int action, int modifiers) {
        this.key = key;
        this.action = action;
        this.modifiers = modifiers;
    }

    public int getKey() {
        return key;
    }

    public int getAction() {
        return action;
    }

    public int getModifiers() {
        return modifiers;
    }
}
