package ru.eclipse.client.module.setting;

public class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, String description, int defaultColor) {
        super(name, description, defaultColor);
    }

    public int getColor() {
        return value != null ? value : 0xFFFFFFFF;
    }

    public int getRed() {
        return (getColor() >> 16) & 0xFF;
    }

    public int getGreen() {
        return (getColor() >> 8) & 0xFF;
    }

    public int getBlue() {
        return getColor() & 0xFF;
    }

    public int getAlpha() {
        return (getColor() >> 24) & 0xFF;
    }
}
