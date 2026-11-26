package ru.example.donorprefix.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.example.donorprefix.gui.RenderUtils;
import ru.example.donorprefix.visual.CPSCounter;
import ru.example.donorprefix.visual.VisualConfig;

/**
 * HUD-виджет Keystrokes:
 * Отображает клавиши W, A, S, D, кнопки мыши (LMB/RMB) со счетчиком CPS и полоску Space.
 * Размеры элемента строго охватывают все кнопки (72 x 86).
 */
public class KeystrokesHudElement extends HudElement {

    public static final int KEY_SIZE = 22;
    public static final int GAP = 3;
    public static final int TOTAL_WIDTH = KEY_SIZE * 3 + GAP * 2; // 72
    public static final int TOTAL_HEIGHT = 86; // 22 + 3 + 22 + 3 + 22 + 3 + 11 = 86

    private float wAnim = 0f;
    private float aAnim = 0f;
    private float sAnim = 0f;
    private float dAnim = 0f;
    private float lmbAnim = 0f;
    private float rmbAnim = 0f;
    private float spaceAnim = 0f;

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

        wAnim += ((wDown ? 1f : 0f) - wAnim) * 0.35f;
        aAnim += ((aDown ? 1f : 0f) - aAnim) * 0.35f;
        sAnim += ((sDown ? 1f : 0f) - sAnim) * 0.35f;
        dAnim += ((dDown ? 1f : 0f) - dAnim) * 0.35f;
        lmbAnim += ((lmbDown ? 1f : 0f) - lmbAnim) * 0.35f;
        rmbAnim += ((rmbDown ? 1f : 0f) - rmbAnim) * 0.35f;
        spaceAnim += ((spaceDown ? 1f : 0f) - spaceAnim) * 0.35f;

        int lmbCps = CPSCounter.getLeftCps();
        int rmbCps = CPSCounter.getRightCps();

        // Отрисовка СТРОГО от (0, 0)
        // Ряд 1: [   ] [ W ] [   ]
        drawKey(context, client, KEY_SIZE + GAP, 0, KEY_SIZE, KEY_SIZE, "W", null, wAnim);

        // Ряд 2: [ A ] [ S ] [ D ]
        int r2Y = KEY_SIZE + GAP; // 25
        drawKey(context, client, 0, r2Y, KEY_SIZE, KEY_SIZE, "A", null, aAnim);
        drawKey(context, client, KEY_SIZE + GAP, r2Y, KEY_SIZE, KEY_SIZE, "S", null, sAnim);
        drawKey(context, client, (KEY_SIZE + GAP) * 2, r2Y, KEY_SIZE, KEY_SIZE, "D", null, dAnim);

        // Ряд 3: [ LMB + CPS ] [ RMB + CPS ]
        int r3Y = r2Y + KEY_SIZE + GAP; // 50
        int mouseW = (TOTAL_WIDTH - GAP) / 2; // 34
        int mouseW2 = TOTAL_WIDTH - GAP - mouseW; // 35
        drawKey(context, client, 0, r3Y, mouseW, KEY_SIZE, "LMB", lmbCps + " CPS", lmbAnim);
        drawKey(context, client, mouseW + GAP, r3Y, mouseW2, KEY_SIZE, "RMB", rmbCps + " CPS", rmbAnim);

        // Ряд 4: [ _______ SPACE _______ ]
        int r4Y = r3Y + KEY_SIZE + GAP; // 75
        drawKey(context, client, 0, r4Y, TOTAL_WIDTH, 11, "—", null, spaceAnim);
    }

    private void drawKey(DrawContext context, MinecraftClient client, int x, int y, int w, int h, String label, String sub, float pressAnim) {
        VisualConfig cfg = VisualConfig.getInstance();
        int baseBg = cfg.themeBgColor;
        int activeBg = RenderUtils.lerpColor(baseBg, cfg.themePrimaryColor, pressAnim * 0.8f);
        int border = RenderUtils.lerpColor(cfg.themeOutlineColor, cfg.themePrimaryColor, pressAnim);

        RenderUtils.drawRoundedRect(context, x, y, w, h, 4, activeBg);
        RenderUtils.drawRoundedOutline(context, x, y, w, h, 4, border);

        int textCol = RenderUtils.lerpColor(cfg.themeTextColor, 0xFFFFFFFF, pressAnim);

        if (sub != null) {
            RenderUtils.drawCenteredTextWithShadow(context, client.textRenderer, Text.literal(label).formatted(Formatting.BOLD), x + w / 2, y + 2, textCol);
            RenderUtils.drawCenteredTextWithShadow(context, client.textRenderer, Text.literal(sub).formatted(Formatting.GRAY), x + w / 2, y + 11, cfg.themeSubtextColor);
        } else {
            int textY = y + (h - 8) / 2;
            RenderUtils.drawCenteredTextWithShadow(context, client.textRenderer, Text.literal(label).formatted(Formatting.BOLD), x + w / 2, textY, textCol);
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
