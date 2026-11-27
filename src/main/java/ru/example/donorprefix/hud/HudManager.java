package ru.example.donorprefix.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import ru.example.donorprefix.gui.UpdateNoticeOverlay;
import ru.example.donorprefix.visual.CustomCrosshairRenderer;
import ru.example.donorprefix.visual.HitMarkerManager;
import ru.example.donorprefix.visual.VisualConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Менеджер управления и отрисовки всех HUD-элементов и оверлеев мода.
 */
public class HudManager {
    private static final HudManager INSTANCE = new HudManager();

    private final List<HudElement> elements = new ArrayList<>();
    private boolean initialized = false;
    private long lastRenderTime = 0;

    private HudManager() {
        elements.add(new WatermarkHudElement());
        elements.add(new FpsHudElement());
        elements.add(new CpsHudElement());
        elements.add(new KeystrokesHudElement());
        elements.add(new ArmorHudElement());
        elements.add(new TargetHudElement());
        elements.add(new PotionHudElement());
    }

    public static HudManager getInstance() {
        return INSTANCE;
    }

    public List<HudElement> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public void loadConfigs() {
        for (HudElement el : elements) {
            el.loadConfig();
        }
        initialized = true;
    }

    public void saveConfigs() {
        for (HudElement el : elements) {
            el.saveConfig();
        }
        VisualConfig.getInstance().save();
    }

    public void renderInGame(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null || client.options.hudHidden) return;

        if (context == null) return;

        if (!initialized) {
            loadConfigs();
            if (client.getWindow() != null) {
                int screenW = client.getWindow().getScaledWidth();
                int screenH = client.getWindow().getScaledHeight();
                for (HudElement el : elements) {
                    el.clampOrReset(screenW, screenH);
                }
            }
        }

        // 1. Хитмаркеры попадания по врагам
        try {
            HitMarkerManager.render(context, client);
        } catch (Throwable ignored) {}

        // 2. Кастомный прицел
        try {
            if (VisualConfig.getInstance().customCrosshair) {
                CustomCrosshairRenderer.render(context, client);
            }
        } catch (Throwable ignored) {}

        // 3. Все HUD-элементы
        for (HudElement el : elements) {
            try {
                el.render(context, false, false, false);
            } catch (Throwable ignored) {}
        }

        // 4. Всплывающее уведомление об обновлении
        try {
            UpdateNoticeOverlay.renderInGameToast(context, client);
        } catch (Throwable ignored) {}
    }
}
