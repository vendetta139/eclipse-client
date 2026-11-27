package ru.example.donorprefix.gui;

/**
 * Единая монохромная графитовая палитра ECLIPSE CLIENT.
 */
public final class EclipseColors {

    // Основные подложки окон и оверлеев (#09090B)
    public static final int BG_WINDOW = 0xFF09090B;
    public static final int BG_WINDOW_TRANSLUCENT = 0xF009090B;
    public static final int BG_HUD = 0xB809090B;

    // Карточки, панели и сайдбар
    public static final int BG_CARD = 0xFF121215;
    public static final int BG_SIDEBAR_ACTIVE = 0xFF18181D;
    public static final int BG_HOVER = 0xFF1C1C21;
    public static final int TRACK_BG = 0xFF1E1E24;

    // Тонкие 1px рамки
    public static final int BORDER = 0xFF24242A;
    public static final int BORDER_ACTIVE = 0xFFE4E4E7;

    // Текст
    public static final int TEXT_PRIMARY = 0xFFF4F4F5;
    public static final int TEXT_SECONDARY = 0xFF71717A;
    public static final int TEXT_MUTED_HUD = 0xFF8E8E93;
    public static final int TEXT_VERSION = 0xFFD4D4D8;
    public static final int TEXT_DISABLED = 0xFF52525B;

    // Инвертированный активный акцент
    public static final int ACCENT_WHITE = 0xFFF4F4F5;
    public static final int TEXT_ON_ACCENT = 0xFF09090B;

    // Чистый белый и серый для чат-уведомлений
    public static final int CHAT_WHITE = 0xFFFFFF;
    public static final int CHAT_VERSION = 0xD4D4D8;
    public static final int CHAT_CHANGELOG = 0xA1A1AA;

    // Направляющие в редакторе HUD
    public static final int EDITOR_GUIDE = 0x44E4E4E7;
    public static final int EDITOR_GRID = 0x16FFFFFF;

    private EclipseColors() {}
}
