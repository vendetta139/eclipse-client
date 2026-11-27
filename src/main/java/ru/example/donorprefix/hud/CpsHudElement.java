package ru.example.donorprefix.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import ru.example.donorprefix.gui.EclipseColors;
import ru.example.donorprefix.gui.RenderUtils;
import ru.example.donorprefix.visual.CPSCounter;
import ru.example.donorprefix.visual.VisualConfig;

/**
 * HUD-виджет CPS в строгом монохромном стиле.
 */
public class CpsHudElement extends HudElement {

    private static final String UNIT = " CPS";

    private int lastLmb = Integer.MIN_VALUE;
    private int lastRmb = Integer.MIN_VALUE;
    private String cachedVal = "0 | 0";
    private int cachedValW = 24;
    private int cachedUnitW = -1;
    private int cachedTotalW = 72;

    public CpsHudElement() {
        super("cps", "CPS", 72, 16);
    }

    @Override
    public void renderContent(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        VisualConfig cfg = VisualConfig.getInstance();

        if (cachedUnitW < 0) {
            cachedUnitW = client.textRenderer.getWidth(UNIT);
        }
        int lmb = CPSCounter.getLeftCps();
        int rmb = CPSCounter.getRightCps();
        if (lmb != lastLmb || rmb != lastRmb) {
            lastLmb = lmb;
            lastRmb = rmb;
            cachedVal = lmb + " | " + rmb;
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
        this.x = cfg.cpsCounterX >= 0 ? cfg.cpsCounterX : 8.0f;
        this.y = cfg.cpsCounterY >= 0 ? cfg.cpsCounterY : 48.0f;
        this.scale = cfg.cpsCounterScale > 0.2f ? cfg.cpsCounterScale : 1.0f;
        this.enabled = cfg.cpsCounterEnabled;
    }

    @Override
    public void saveConfig() {
        VisualConfig cfg = VisualConfig.getInstance();
        cfg.cpsCounterX = this.x;
        cfg.cpsCounterY = this.y;
        cfg.cpsCounterScale = this.scale;
        cfg.cpsCounterEnabled = this.enabled;
    }

    @Override
    public void resetDefaultPosition(int screenWidth, int screenHeight) {
        this.x = 8.0f;
        this.y = 48.0f;
        this.scale = 1.0f;
    }
}
