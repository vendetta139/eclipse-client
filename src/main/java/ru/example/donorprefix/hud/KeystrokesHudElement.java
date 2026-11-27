package ru.example.donorprefix.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.example.donorprefix.gui.EclipseColors;
import ru.example.donorprefix.gui.RenderUtils;
import ru.example.donorprefix.visual.CPSCounter;
import ru.example.donorprefix.visual.VisualConfig;

/**
 * HUD-виджет Keystrokes в строгом монохромном стиле с резкой инверсией при нажатии.
 */
public class KeystrokesHudElement extends HudElement {

    public static final int KEY_SIZE = 22;
    public static final int GAP = 3;
    public static final int TOTAL_WIDTH = KEY_SIZE * 3 + GAP * 2; // 72
    public static final int TOTAL_HEIGHT = 86;

    private static final Text LABEL_W = Text.literal("W").formatted(Formatting.BOLD);
    private static final Text LABEL_A = Text.literal("A").formatted(Formatting.BOLD);
    private static final Text LABEL_S = Text.literal("S").formatted(Formatting.BOLD);
    private static final Text LABEL_D = Text.literal("D").formatted(Formatting.BOLD);
    private static final Text LABEL_LMB = Text.literal("LMB").formatted(Formatting.BOLD);
    private static final Text LABEL_RMB = Text.literal("RMB").formatted(Formatting.BOLD);
    private static final Text LABEL_SPACE = Text.literal("—").formatted(Formatting.BOLD);

    private int lastLmbCps = Integer.MIN_VALUE;
    private int lastRmbCps = Integer.MIN_VALUE;
    private Text cachedLmbSub = Text.literal("0 CPS");
    private Text cachedRmbSub = Text.literal("0 CPS");

    public KeystrokesHudElement() {
        super("keystrokes", "Keystrokes", TOTAL_WIDTH, TOTAL_HEIGHT);
    }

    @Override
    public void renderContent(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();

        boolean wDown = client.options != null && client.options.forwardKey.isPressed();
        boolean aDown = client.options != null && client.options.leftKey.isPressed();
        boolean sDown = client.options != null && client.options.backKey.isPressed();
        boolean dDown = client.options != null && client.options.rightKey.isPressed();
        boolean lmbDown = client.options != null && client.options.attackKey.isPressed();
        boolean rmbDown = client.options != null && client.options.useKey.isPressed();
        boolean spaceDown = client.options != null && client.options.jumpKey.isPressed();

        int lmbCps = CPSCounter.getLeftCps();
        int rmbCps = CPSCounter.getRightCps();
        if (lmbCps != lastLmbCps) {
            lastLmbCps = lmbCps;
            cachedLmbSub = Text.literal(lmbCps + " CPS");
        }
        if (rmbCps != lastRmbCps) {
            lastRmbCps = rmbCps;
            cachedRmbSub = Text.literal(rmbCps + " CPS");
        }

        drawKey(context, client, KEY_SIZE + GAP, 0, KEY_SIZE, KEY_SIZE, LABEL_W, null, wDown);

        int r2Y = KEY_SIZE + GAP;
        drawKey(context, client, 0, r2Y, KEY_SIZE, KEY_SIZE, LABEL_A, null, aDown);
        drawKey(context, client, KEY_SIZE + GAP, r2Y, KEY_SIZE, KEY_SIZE, LABEL_S, null, sDown);
        drawKey(context, client, (KEY_SIZE + GAP) * 2, r2Y, KEY_SIZE, KEY_SIZE, LABEL_D, null, dDown);

        int r3Y = r2Y + KEY_SIZE + GAP;
        int mouseW = (TOTAL_WIDTH - GAP) / 2;
        int mouseW2 = TOTAL_WIDTH - GAP - mouseW;
        drawKey(context, client, 0, r3Y, mouseW, KEY_SIZE, LABEL_LMB, cachedLmbSub, lmbDown);
        drawKey(context, client, mouseW + GAP, r3Y, mouseW2, KEY_SIZE, LABEL_RMB, cachedRmbSub, rmbDown);

        int r4Y = r3Y + KEY_SIZE + GAP;
        drawKey(context, client, 0, r4Y, TOTAL_WIDTH, 11, LABEL_SPACE, null, spaceDown);
    }

    private void drawKey(DrawContext context, MinecraftClient client, int x, int y, int w, int h, Text label, Text sub, boolean down) {
        int bg = down ? EclipseColors.ACCENT_WHITE : EclipseColors.BG_HUD;
        int border = down ? EclipseColors.BORDER_ACTIVE : EclipseColors.BORDER;
        int textCol = down ? EclipseColors.TEXT_ON_ACCENT : EclipseColors.TEXT_PRIMARY;
        int subCol = down ? EclipseColors.TEXT_DISABLED : EclipseColors.TEXT_MUTED_HUD;

        RenderUtils.drawFlatCard(context, x, y, w, h, bg, border);

        if (sub != null) {
            RenderUtils.drawCenteredTextWithShadow(context, client.textRenderer, label, x + w / 2, y + 2, textCol);
            RenderUtils.drawCenteredTextWithShadow(context, client.textRenderer, sub, x + w / 2, y + 11, subCol);
        } else {
            int textY = y + (h - 8) / 2;
            RenderUtils.drawCenteredTextWithShadow(context, client.textRenderer, label, x + w / 2, textY, textCol);
        }
    }

    @Override
    public void loadConfig() {
        VisualConfig cfg = VisualConfig.getInstance();
        this.x = cfg.keystrokesX;
        this.y = cfg.keystrokesY;
        this.scale = cfg.keystrokesScale > 0.2f ? cfg.keystrokesScale : 1.0f;
        this.enabled = cfg.keystrokesEnabled;
    }

    @Override
    public void saveConfig() {
        VisualConfig cfg = VisualConfig.getInstance();
        cfg.keystrokesX = this.x;
        cfg.keystrokesY = this.y;
        cfg.keystrokesScale = this.scale;
        cfg.keystrokesEnabled = this.enabled;
    }

    @Override
    public void resetDefaultPosition(int screenWidth, int screenHeight) {
        this.x = Math.max(2, screenWidth - this.width - 12);
        this.y = 12.0f;
        this.scale = 1.0f;
    }
}
