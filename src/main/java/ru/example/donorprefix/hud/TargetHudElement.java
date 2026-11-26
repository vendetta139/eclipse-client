package ru.example.donorprefix.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;
import ru.example.donorprefix.gui.RenderUtils;
import ru.example.donorprefix.visual.TargetHUDManager;
import ru.example.donorprefix.visual.VisualConfig;

/**
 * HUD-виджет Target HUD:
 * Отображает противника в бою (3D-модель, имя, дистанцию и полосу здоровья).
 */
public class TargetHudElement extends HudElement {

    public static final int HUD_WIDTH = 160;
    public static final int HUD_HEIGHT = 44;

    private float animHp = 20.0f;
    private float animAlpha = 0.0f;
    private long lastAnimTime = 0;
    private LivingEntity lastTarget = null;

    public TargetHudElement() {
        super("targethud", "Target HUD", HUD_WIDTH, HUD_HEIGHT);
    }

    @Override
    public void renderContent(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        VisualConfig cfg = VisualConfig.getInstance();

        long now = System.currentTimeMillis();
        float deltaSec = lastAnimTime > 0 ? Math.min(0.1f, (now - lastAnimTime) / 1000.0f) : 0.016f;
        lastAnimTime = now;

        boolean isEditor = (client.currentScreen instanceof ru.example.donorprefix.gui.HudEditorScreen);
        LivingEntity target = TargetHUDManager.getCurrentTarget();

        if (isEditor) {
            animAlpha = 1.0f;
        } else if (target != null) {
            lastTarget = target;
            animAlpha = ru.example.donorprefix.gui.AnimationUtil.deltaLerp(animAlpha, 1.0f, 16.0f, deltaSec);
        } else {
            animAlpha = ru.example.donorprefix.gui.AnimationUtil.deltaLerp(animAlpha, 0.0f, 14.0f, deltaSec);
        }

        // Вне боя и вне редактора — HUD полностью скрыт с экрана
        if (animAlpha <= 0.01f && !isEditor) return;

        LivingEntity displayEntity = target != null ? target : (isEditor ? null : lastTarget);
        boolean isPreview = (displayEntity == null);

        String targetName = isPreview ? "Цель (Превью)" : displayEntity.getName().getString();
        float curHp = isPreview ? 18.0f : displayEntity.getHealth();
        float maxHp = isPreview ? 20.0f : Math.max(1.0f, displayEntity.getMaxHealth());
        float dist = isPreview ? 4.2f : (client.player != null && displayEntity != null ? client.player.distanceTo(displayEntity) : 0f);

        animHp += (curHp - animHp) * 0.25f;

        // Плавная микро-анимация масштаба при появлении/исчезновении
        boolean applyScale = animAlpha < 0.999f;
        Matrix3x2fStack matrices = applyScale ? context.getMatrices() : null;
        if (matrices != null) {
            matrices.pushMatrix();
            float scale = 0.92f + 0.08f * animAlpha;
            matrices.translate(this.width / 2.0f, this.height / 2.0f);
            matrices.scale(scale, scale);
            matrices.translate(-this.width / 2.0f, -this.height / 2.0f);
        }

        try {
            int bgCol = RenderUtils.multiplyAlpha(cfg.themeBgColor, animAlpha);
            int glowCol = RenderUtils.multiplyAlpha(cfg.themeGlowColor, animAlpha);
            int outlineCol = RenderUtils.multiplyAlpha(cfg.themeOutlineColor, animAlpha);
            int cardCol = RenderUtils.multiplyAlpha(cfg.themeCardColor, animAlpha);
            int textCol = RenderUtils.multiplyAlpha(cfg.themeTextColor, animAlpha);
            int subtextCol = RenderUtils.multiplyAlpha(cfg.themeSubtextColor, animAlpha);

            // 1. Отрисовка подложки СТРОГО от (0, 0)
            RenderUtils.drawSoftGlow(context, 0, 0, this.width, this.height, 8, 2, glowCol);
            RenderUtils.drawRoundedRect(context, 0, 0, this.width, this.height, 8, bgCol);
            RenderUtils.drawRoundedOutline(context, 0, 0, this.width, this.height, 8, outlineCol);

            // 2. Аватарка / Рамка под модельку (7, 7, 30, 30)
            int avatarSize = 30;
            int avatarX = 7;
            int avatarY = 7;
            RenderUtils.drawRoundedRect(context, avatarX, avatarY, avatarSize, avatarSize, 6, cardCol);
            RenderUtils.drawRoundedOutline(context, avatarX, avatarY, avatarSize, avatarSize, 6, outlineCol);

            if (displayEntity != null && cfg.targetHud3DModel) {
                try {
                    float rotAngle = (float) ((System.currentTimeMillis() % 4500L) / 4500.0 * Math.PI * 2);
                    float lookX = (avatarX + avatarSize / 2f) + (float) Math.sin(rotAngle) * 35f;
                    float lookY = (avatarY + avatarSize / 2f) - 8f;
                    InventoryScreen.drawEntity(
                            context, avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize,
                            14, 0.0625f, lookX, lookY, displayEntity
                    );
                } catch (Throwable ignored) {}
            } else {
                RenderUtils.drawCenteredTextWithShadow(context, client.textRenderer, "✦", avatarX + avatarSize / 2, avatarY + 10, RenderUtils.multiplyAlpha(cfg.themePrimaryColor, animAlpha));
            }

            // 3. Тексты
            RenderUtils.drawTextWithShadow(context, client.textRenderer, targetName, 42, 9, textCol);
            String distStr = String.format("%.1fm", dist);
            int distW = client.textRenderer.getWidth(distStr);
            RenderUtils.drawTextWithShadow(context, client.textRenderer, distStr, this.width - distW - 7, 9, subtextCol);

            // 4. Полоса здоровья
            int barX = 42;
            int barY = 24;
            int barW = this.width - barX - 7; // 111 px
            int barH = 8;
            float hpRatio = MathHelper.clamp(animHp / maxHp, 0f, 1f);

            RenderUtils.drawRoundedRect(context, barX, barY, barW, barH, 3, RenderUtils.multiplyAlpha(0x66000000, animAlpha));
            int fillW = (int) (barW * hpRatio);
            if (fillW > 0) {
                int hpColor = RenderUtils.lerpColor(0xFFEF4444, 0xFF10B981, hpRatio);
                RenderUtils.drawRoundedRect(context, barX, barY, fillW, barH, 3, RenderUtils.multiplyAlpha(hpColor, animAlpha));
            }

            String hpText = String.format("%.0f/%.0f", curHp, maxHp);
            RenderUtils.drawCenteredTextWithShadow(context, client.textRenderer, hpText, barX + barW / 2, barY - 1, RenderUtils.multiplyAlpha(0xFFFFFFFF, animAlpha));
        } finally {
            if (matrices != null) {
                matrices.popMatrix();
            }
        }
    }

    @Override
    public void loadConfig() {
        VisualConfig cfg = VisualConfig.getInstance();
        this.x = cfg.targetHudX;
        this.y = cfg.targetHudY;
        this.scale = cfg.targetHudScale > 0.2f ? cfg.targetHudScale : 1.0f;
        this.enabled = cfg.targetHudEnabled;
    }

    @Override
    public void saveConfig() {
        VisualConfig cfg = VisualConfig.getInstance();
        cfg.targetHudX = this.x;
        cfg.targetHudY = this.y;
        cfg.targetHudScale = this.scale;
        cfg.targetHudEnabled = this.enabled;
    }

    @Override
    public void resetDefaultPosition(int screenWidth, int screenHeight) {
        this.x = (screenWidth - this.width) / 2.0f;
        this.y = screenHeight / 2.0f + 36.0f;
        this.scale = 1.0f;
    }
}
