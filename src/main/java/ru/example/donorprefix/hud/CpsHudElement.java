package ru.example.donorprefix.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import ru.example.donorprefix.gui.RenderUtils;
import ru.example.donorprefix.visual.CPSCounter;
import ru.example.donorprefix.visual.VisualConfig;

/**
 * HUD-виджет CPS:
 * Отображает клики в секунду (LMB & RMB).
 */
public class CpsHudElement extends HudElement {

    public CpsHudElement() {
        super("cps", "CPS", 80, 16);
    }

    @Override
    public void renderContent(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        VisualConfig cfg = VisualConfig.getInstance();

        int lmb = CPSCounter.getLeftCps();
        int rmb = CPSCounter.getRightCps();
        String text = lmb + " │ " + rmb + " CPS";
        int textW = client.textRenderer.getWidth(text);
        this.width = textW + 16;
        this.height = 16;

        // Отрисовка СТРОГО от (0, 0)
        RenderUtils.drawPill(context, 0, 0, this.width, this.height, cfg.themeBgColor, cfg.themeOutlineColor);
        RenderUtils.drawTextWithShadow(context, client.textRenderer, text, 8, 4, cfg.themeTextColor);
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
