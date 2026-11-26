package ru.eclipse.client.module.setting;

public class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    public boolean isEnabled() {
        return value != null && value;
    }

    public void toggle() {
        setValue(!isEnabled());
    }
}
