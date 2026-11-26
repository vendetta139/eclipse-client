package ru.eclipse.client.module;

public enum Category {
    PREFIXES("Префиксы", "✦"),
    COMBAT_PVP("PvP & Бой", "⚔"),
    ANIMATIONS("Анимации", "⚡"),
    WORLD_FOG("Мир & Туман", "☁"),
    HUD_OVERLAYS("HUD & Оверлеи", "❖");

    private final String displayName;
    private final String icon;

    Category(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }
}
