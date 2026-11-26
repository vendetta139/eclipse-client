package ru.example.donorprefix.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import ru.example.donorprefix.gui.RenderUtils;
import ru.example.donorprefix.visual.VisualConfig;

/**
 * HUD-виджет FPS:
 * Отображает текущую частоту кадров (FPS).
 */
public class FpsHudElement extends HudElement {

    private int lastFps = Integer.MIN_VALUE;
    private String cachedText = "0 FPS";
    private int cachedWidth = 60;

    public FpsHudElement() {
        super("fps", "FPS", 60, 16);
    }

    @Override
    public void renderContent(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        VisualConfig cfg = VisualConfig.getInstance();

        int fps = client.getCurrentFps();
        if (fps != lastFps) {
            lastFps = fps;
            cachedText = fps + " FPS";
            cachedWidth = client.textRenderer.getWidth(cachedText) + 16;
        }
        this.width = cachedWidth;
        this.height = 16;

        // Отрисовка СТРОГО от (0, 0)
        RenderUtils.drawPill(context, 0, 0, this.width, this.height, cfg.themeBgColor, cfg.themeOutlineColor);
        RenderUtils.drawTextWithShadow(context, client.textRenderer, cachedText, 8, 4, cfg.themeTextColor);
    }

    @Override
    public void loadConfig() {
        VisualConfig cfg = VisualConfig.getInstance();
        this.x = cfg.fpsHudX >= 0 ? cfg.fpsHudX : 8.0f;
        this.y = cfg.fpsHudY >= 0 ? cfg.fpsHudY : 28.0f;
        this.scale = cfg.fpsHudScale > 0.2f ? cfg.fpsHudScale : 1.0f;
        this.enabled = cfg.fpsHudEnabled;
    }

    @Override
    public void saveConfig() {
        VisualConfig cfg = VisualConfig.getInstance();
        cfg.fpsHudX = this.x;
        cfg.fpsHudY = this.y;
        cfg.fpsHudScale = this.scale;
        cfg.fpsHudEnabled = this.enabled;
    }

    @Override
    public void resetDefaultPosition(int screenWidth, int screenHeight) {
        this.x = 8.0f;
        this.y = 28.0f;
        this.scale = 1.0f;
    }
}
