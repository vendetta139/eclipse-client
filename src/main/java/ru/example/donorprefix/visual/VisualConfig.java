package ru.example.donorprefix.visual;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import ru.eclipse.client.core.EclipseStateSync;
import ru.example.donorprefix.DonorPrefixClientMod;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Конфигурация визуальных модулей, темы и HUD (сохраняется в config/eclipse-client.json).
 */
public class VisualConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path LEGACY_PATH = FabricLoader.getInstance().getConfigDir().resolve("donor-prefix-visuals.json");
    private static VisualConfig INSTANCE;

    // --- 0. Общие настройки и цветовая тема UI ---
    public boolean checkUpdates = true;
    public boolean betaChannel = false;
    public int themePreset = 0; // 0: Space Eclipse, 1: Evo Midnight, 2: Cyan, 3: Emerald, 4: Crimson
    public int themePrimaryColor = 0xFFF5A65B;
    public int themeSecondaryColor = 0xFF6A67CE;
    public int themeBgColor = 0xF20B0B10;
    public int themeCardColor = 0xFF101118;
    public int themeOutlineColor = 0xFF232533;
    public int themeGlowColor = 0x2A6A67CE;
    public int themeTextColor = 0xFFEAECEF;
    public int themeSubtextColor = 0xFF7D8296;

    // --- 1. HUD и оверлеи ---
    public boolean hudSnapping = true;

    public boolean watermarkEnabled = true;
    public float watermarkX = 8.0f;
    public float watermarkY = 8.0f;
    public float watermarkScale = 1.0f;

    public boolean fpsHudEnabled = true;
    public float fpsHudX = 8.0f;
    public float fpsHudY = 28.0f;
    public float fpsHudScale = 1.0f;

    public boolean cpsCounterEnabled = true;
    public float cpsCounterX = 8.0f;
    public float cpsCounterY = 48.0f;
    public float cpsCounterScale = 1.0f;

    public boolean targetHudEnabled = true;
    public boolean targetHud3DModel = true;
    public float targetHudX = -1.0f;
    public float targetHudY = -1.0f;
    public float targetHudScale = 1.0f;

    public boolean potionHudEnabled = true;
    public float potionHudX = -1.0f;
    public float potionHudY = -1.0f;
    public float potionHudScale = 1.0f;

    public boolean armorHudEnabled = true;
    public float armorHudX = -1.0f;
    public float armorHudY = -1.0f;
    public float armorHudScale = 1.0f;

    public boolean keystrokesEnabled = false;
    public float keystrokesX = -1.0f;
    public float keystrokesY = -1.0f;
    public float keystrokesScale = 1.0f;

    // --- 2. PvP & Бой ---
    public boolean noHurtCam = true;
    public boolean hitMarkerEnabled = true;
    public boolean hitSoundEnabled = true;
    public int hitSoundType = 0; // 0: Ding, 1: Skeet, 2: Chime, 3: Arrow, 4: Bonk

    public boolean customCrosshair = false;
    public int crosshairType = 0; // 0: Dot, 1: Cross, 2: Circle, 3: T-Shape
    public int crosshairColor = 0xFFFFFFFF;
    public boolean crosshairRainbow = false;
    public int crosshairSize = 4;
    public int crosshairGap = 2;
    public int crosshairThickness = 1;
    public boolean crosshairCenterDot = false;

    // --- 3. Анимации и руки ---
    public boolean blockHit = false;
    public boolean smallShield = false;
    public float handScale = 1.0f;

    // --- 4. Мир & Оверлеи ---
    public boolean fullbright = true;
    public boolean noFog = true;
    public boolean lowFireEnabled = true;
    public float lowFireOffset = -0.30f;
    public boolean timeChangerEnabled = false;
    public long customTime = 12500L;
    public boolean clearWeather = false;

    public void resetHudPositions() {
        watermarkX = 8.0f;
        watermarkY = 8.0f;
        watermarkScale = 1.0f;

        fpsHudX = 8.0f;
        fpsHudY = 28.0f;
        fpsHudScale = 1.0f;

        cpsCounterX = 8.0f;
        cpsCounterY = 48.0f;
        cpsCounterScale = 1.0f;

        targetHudX = -1.0f;
        targetHudY = -1.0f;
        targetHudScale = 1.0f;

        potionHudX = -1.0f;
        potionHudY = -1.0f;
        potionHudScale = 1.0f;

        armorHudX = -1.0f;
        armorHudY = -1.0f;
        armorHudScale = 1.0f;

        keystrokesX = -1.0f;
        keystrokesY = -1.0f;
        keystrokesScale = 1.0f;

        save();
    }

    public void applyThemePreset(int preset) {
        this.themePreset = preset;
        switch (preset) {
            case 0 -> {
                themePrimaryColor = 0xFFF5A65B;
                themeSecondaryColor = 0xFF6A67CE;
                themeBgColor = 0xF20B0B10;
                themeCardColor = 0xFF101118;
                themeOutlineColor = 0xFF232533;
                themeGlowColor = 0x2A6A67CE;
                themeTextColor = 0xFFEAECEF;
                themeSubtextColor = 0xFF7D8296;
            }
            case 1 -> {
                themePrimaryColor = 0xFF8B5CF6;
                themeSecondaryColor = 0xFF06B6D4;
                themeBgColor = 0xF2090D16;
                themeCardColor = 0x2A151F33;
                themeOutlineColor = 0x4D8B5CF6;
                themeGlowColor = 0x338B5CF6;
                themeTextColor = 0xFFF8FAFC;
                themeSubtextColor = 0xFF94A3B8;
            }
            case 2 -> {
                themePrimaryColor = 0xFF00E5FF;
                themeSecondaryColor = 0xFF38BDF8;
                themeBgColor = 0xF2030E17;
                themeCardColor = 0x2A061C2C;
                themeOutlineColor = 0x5500E5FF;
                themeGlowColor = 0x4400E5FF;
                themeTextColor = 0xFFF8FAFC;
                themeSubtextColor = 0xFF94A3B8;
            }
            case 3 -> {
                themePrimaryColor = 0xFF10B981;
                themeSecondaryColor = 0xFF34D399;
                themeBgColor = 0xF204140E;
                themeCardColor = 0x2A08261B;
                themeOutlineColor = 0x5510B981;
                themeGlowColor = 0x4410B981;
                themeTextColor = 0xFFF8FAFC;
                themeSubtextColor = 0xFF94A3B8;
            }
            case 4 -> {
                themePrimaryColor = 0xFFFF1744;
                themeSecondaryColor = 0xFFFF5252;
                themeBgColor = 0xF20B0205;
                themeCardColor = 0x2A28050B;
                themeOutlineColor = 0x66FF1744;
                themeGlowColor = 0x44FF1744;
                themeTextColor = 0xFFF8FAFC;
                themeSubtextColor = 0xFF94A3B8;
            }
            default -> {}
        }
        save();
    }

    public int getEffectiveCrosshairColor() {
        if (crosshairRainbow) {
            float hue = (float) ((System.currentTimeMillis() % 3000L) / 3000.0 * 360.0);
            return ru.example.donorprefix.gui.RenderUtils.hsvToRgb(hue, 1.0f, 1.0f);
        }
        return crosshairColor;
    }

    public void validateScales() {
        if (watermarkScale <= 0.1f || Float.isNaN(watermarkScale)) watermarkScale = 1.0f;
        if (fpsHudScale <= 0.1f || Float.isNaN(fpsHudScale)) fpsHudScale = 1.0f;
        if (cpsCounterScale <= 0.1f || Float.isNaN(cpsCounterScale)) cpsCounterScale = 1.0f;
        if (targetHudScale <= 0.1f || Float.isNaN(targetHudScale)) targetHudScale = 1.0f;
        if (potionHudScale <= 0.1f || Float.isNaN(potionHudScale)) potionHudScale = 1.0f;
        if (armorHudScale <= 0.1f || Float.isNaN(armorHudScale)) armorHudScale = 1.0f;
        if (keystrokesScale <= 0.1f || Float.isNaN(keystrokesScale)) keystrokesScale = 1.0f;
    }

    public static VisualConfig getCurrentOrNull() {
        return INSTANCE;
    }

    public static VisualConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = loadOrCreate();
        }
        return INSTANCE;
    }

    public static VisualConfig loadOrCreate() {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        JsonObject root = EclipseStateSync.getInstance().readRootIfExists();
        if (root != null && root.has("visuals") && root.get("visuals").isJsonObject()) {
            VisualConfig cfg = GSON.fromJson(root.getAsJsonObject("visuals"), VisualConfig.class);
            if (cfg != null) {
                cfg.validateScales();
                INSTANCE = cfg;
                return cfg;
            }
        }

        if (Files.exists(LEGACY_PATH)) {
            try (Reader reader = Files.newBufferedReader(LEGACY_PATH, StandardCharsets.UTF_8)) {
                VisualConfig config = GSON.fromJson(reader, VisualConfig.class);
                if (config != null) {
                    config.validateScales();
                    INSTANCE = config;
                    return config;
                }
            } catch (IOException e) {
                DonorPrefixClientMod.LOGGER.error("Не удалось прочитать legacy donor-prefix-visuals.json", e);
            }
        }

        VisualConfig defaultConfig = new VisualConfig();
        INSTANCE = defaultConfig;
        defaultConfig.save();
        return defaultConfig;
    }

    public static void applyFromJson(JsonObject visualsObj) {
        if (visualsObj == null) return;
        try {
            VisualConfig cfg = GSON.fromJson(visualsObj, VisualConfig.class);
            if (cfg != null) {
                cfg.validateScales();
                INSTANCE = cfg;
            }
        } catch (Throwable t) {
            DonorPrefixClientMod.LOGGER.error("Ошибка при применении секции visuals из eclipse-client.json", t);
        }
    }

    public void save() {
        INSTANCE = this;
        EclipseStateSync.getInstance().saveState();
    }
}
