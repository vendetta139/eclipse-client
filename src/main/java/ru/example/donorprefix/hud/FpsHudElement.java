package ru.example.donorprefix.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import ru.example.donorprefix.gui.EclipseColors;
import ru.example.donorprefix.gui.RenderUtils;
import ru.example.donorprefix.visual.VisualConfig;

/**
 * HUD-виджет FPS в строгом монохромном стиле.
 */
public class FpsHudElement extends HudElement {

    private static final String UNIT = " FPS";

    private int lastFps = Integer.MIN_VALUE;
    private String cachedVal = "0";
    private int cachedValW = 6;
    private int cachedUnitW = -1;
    private int cachedTotalW = 54;

    public FpsHudElement() {
        super("fps", "FPS", 54, 16);
    }

    @Override
    public void renderContent(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        VisualConfig cfg = VisualConfig.getInstance();

        if (cachedUnitW < 0) {
            cachedUnitW = client.textRenderer.getWidth(UNIT);
        }
        int fps = client.getCurrentFps();
        if (fps != lastFps) {
            lastFps = fps;
            cachedVal = Integer.toString(fps);
            cachedValW = client.textRenderer.getWidth(cachedVal);
            cachedTotalW = cachedValW + cachedUnitW + 16;
        }
        this.width = cachedTotalW;
        this.height = 16;

        int bg = cfg.themePreset == 2 ? cfg.themeBgColor : EclipseColors.BG_HUD;
        int border = cfg.themePreset == 2 ? cfg.themeOutlineColor : EclipseColors.BORDER;
        int valCol = cfg.themePreset == 2 ? cfg.themeTextColor : EclipseColors.TEXT_PRIMARY;
        int unitCol = cfg.themePreset == 2 ? cfg.themeSubtextColor : EclipseColors.TEXT_MUTED_HUD;

        RenderUtils.drawFlatCard(context, 0, 0, this.width, this.height, bg, border);
        RenderUtils.drawTextWithShadow(context, client.textRenderer, cachedVal, 8, 4, valCol);
        RenderUtils.drawTextWithShadow(context, client.textRenderer, UNIT, 8 + cachedValW, 4, unitCol);
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
