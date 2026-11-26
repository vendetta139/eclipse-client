package ru.eclipse.client.module;

import net.minecraft.client.MinecraftClient;
import ru.eclipse.client.event.EventBus;
import ru.eclipse.client.module.setting.Setting;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private int keybind;
    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, String description, Category category, int defaultKeybind, boolean defaultEnabled) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.keybind = defaultKeybind;
        this.enabled = defaultEnabled;
    }

    public Module(String name, String description, Category category) {
        this(name, description, category, 0, false);
    }

    protected void addSetting(Setting<?> setting) {
        this.settings.add(setting);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (this.enabled) {
                EventBus.register(this);
                onEnable();
            } else {
                EventBus.unregister(this);
                onDisable();
            }
        }
    }

    public void toggle() {
        setEnabled(!this.enabled);
    }

    public void onEnable() {}

    public void onDisable() {}

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getKeybind() {
        return keybind;
    }

    public void setKeybind(int keybind) {
        this.keybind = keybind;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }
}
