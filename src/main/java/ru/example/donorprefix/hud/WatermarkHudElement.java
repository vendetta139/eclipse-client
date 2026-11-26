package ru.example.donorprefix.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import ru.example.donorprefix.gui.RenderUtils;
import ru.example.donorprefix.visual.VisualConfig;

/**
 * HUD-виджет Watermark:
 * Отображает название клиента, текущий FPS и пинг соединения.
 */
public class WatermarkHudElement extends HudElement {

    public WatermarkHudElement() {
        super("watermark", "Watermark", 160, 18);
    }

    @Override
    public void renderContent(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        VisualConfig cfg = VisualConfig.getInstance();

        int fps = client.getCurrentFps();
        int ping = 0;
        if (client.getNetworkHandler() != null && client.player != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            if (entry != null) {
                ping = entry.getLatency();
            }
        }

        String text = "✦ ECLIPSE  │  " + fps + " FPS  │  " + ping + "ms";
        int textW = client.textRenderer.getWidth(text);
        this.width = textW + 16;
        this.height = 18;

        // Отрисовка СТРОГО от (0, 0)
        RenderUtils.drawSoftGlow(context, 0, 0, this.width, this.height, 8, 2, cfg.themeGlowColor);
        RenderUtils.drawPill(context, 0, 0, this.width, this.height, cfg.themeBgColor, cfg.themeOutlineColor);
        RenderUtils.drawTextWithShadow(context, client.textRenderer, text, 8, 5, cfg.themeTextColor);
    }

    @Override
    public void loadConfig() {
        VisualConfig cfg = VisualConfig.getInstance();
        this.x = cfg.watermarkX >= 0 ? cfg.watermarkX : 8.0f;
        this.y = cfg.watermarkY >= 0 ? cfg.watermarkY : 8.0f;
        this.scale = cfg.watermarkScale > 0.2f ? cfg.watermarkScale : 1.0f;
        this.enabled = cfg.watermarkEnabled;
    }

    @Override
    public void saveConfig() {
        VisualConfig cfg = VisualConfig.getInstance();
        cfg.watermarkX = this.x;
        cfg.watermarkY = this.y;
        cfg.watermarkScale = this.scale;
        cfg.watermarkEnabled = this.enabled;
    }

    @Override
    public void resetDefaultPosition(int screenWidth, int screenHeight) {
        this.x = 8.0f;
        this.y = 8.0f;
        this.scale = 1.0f;
    }
}
