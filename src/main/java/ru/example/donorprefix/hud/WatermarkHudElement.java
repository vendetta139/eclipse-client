package ru.example.donorprefix.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import ru.example.donorprefix.gui.EclipseColors;
import ru.example.donorprefix.gui.RenderUtils;
import ru.example.donorprefix.visual.VisualConfig;

/**
 * HUD-виджет Watermark в строгом монохромном стиле.
 */
public class WatermarkHudElement extends HudElement {

    private static final String TITLE = "ECLIPSE";

    private int lastFps = Integer.MIN_VALUE;
    private int lastPing = Integer.MIN_VALUE;
    private int cachedTitleW = -1;
    private String cachedMeta = "  |  0 FPS  |  0ms";
    private int cachedTotalW = 150;

    public WatermarkHudElement() {
        super("watermark", "Watermark", 150, 18);
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

        if (cachedTitleW < 0) {
            cachedTitleW = client.textRenderer.getWidth(TITLE);
        }
        if (fps != lastFps || ping != lastPing) {
            lastFps = fps;
            lastPing = ping;
            cachedMeta = "  |  " + fps + " FPS  |  " + ping + "ms";
            cachedTotalW = cachedTitleW + client.textRenderer.getWidth(cachedMeta) + 16;
        }
        this.width = cachedTotalW;
        this.height = 18;

        int bg = cfg.themePreset == 2 ? cfg.themeBgColor : EclipseColors.BG_HUD;
        int border = cfg.themePreset == 2 ? cfg.themeOutlineColor : EclipseColors.BORDER;
        int titleCol = cfg.themePreset == 2 ? cfg.themeTextColor : EclipseColors.TEXT_PRIMARY;
        int metaCol = cfg.themePreset == 2 ? cfg.themeSubtextColor : EclipseColors.TEXT_MUTED_HUD;

        RenderUtils.drawFlatCard(context, 0, 0, this.width, this.height, bg, border);
        RenderUtils.drawTextWithShadow(context, client.textRenderer, TITLE, 8, 5, titleCol);
        RenderUtils.drawTextWithShadow(context, client.textRenderer, cachedMeta, 8 + cachedTitleW, 5, metaCol);
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
