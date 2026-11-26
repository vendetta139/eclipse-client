package ru.eclipse.client.module.setting;

import java.util.function.Supplier;

public abstract class Setting<T> {
    private final String name;
    private final String description;
    protected T value;
    private Supplier<Boolean> visible = () -> true;

    public Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public boolean isVisible() {
        return visible.get();
    }

    public Setting<T> setVisible(Supplier<Boolean> visible) {
        this.visible = visible;
        return this;
    }
}
