package ru.eclipse.client.module.setting;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String description, String defaultMode, String... modes) {
        super(name, description, defaultMode);
        this.modes = Arrays.asList(modes);
    }

    public List<String> getModes() {
        return modes;
    }

    public boolean is(String mode) {
        return value != null && value.equalsIgnoreCase(mode);
    }

    public void cycle() {
        if (modes.isEmpty()) return;
        int index = modes.indexOf(value);
        int next = (index + 1) % modes.size();
        setValue(modes.get(next));
    }
}
