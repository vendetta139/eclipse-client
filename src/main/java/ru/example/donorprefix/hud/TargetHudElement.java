package ru.example.donorprefix.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;
import ru.example.donorprefix.gui.EclipseColors;
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

    private LivingEntity cachedNameEntity = null;
    private String cachedTargetName = "Цель (Превью)";
    private int lastDistTenths = Integer.MIN_VALUE;
    private String cachedDistStr = "4.2m";
    private int cachedDistW = -1;
    private int lastCurHpInt = Integer.MIN_VALUE;
    private int lastMaxHpInt = Integer.MIN_VALUE;
    private String cachedHpStr = "18/20";

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

        if (isPreview) {
            cachedNameEntity = null;
            cachedTargetName = "Цель (Превью)";
        } else if (cachedNameEntity != displayEntity) {
            cachedNameEntity = displayEntity;
            cachedTargetName = displayEntity.getName().getString();
        }
        float curHp = isPreview ? 18.0f : displayEntity.getHealth();
        float maxHp = isPreview ? 20.0f : Math.max(1.0f, displayEntity.getMaxHealth());
        float dist = isPreview ? 4.2f : (client.player != null && displayEntity != null ? client.player.distanceTo(displayEntity) : 0f);

        int distTenths = Math.max(0, Math.round(dist * 10.0f));
        if (distTenths != lastDistTenths || cachedDistW < 0) {
            lastDistTenths = distTenths;
            cachedDistStr = (distTenths / 10) + "." + (distTenths % 10) + "m";
            cachedDistW = client.textRenderer.getWidth(cachedDistStr);
        }

        int curHpInt = Math.round(curHp);
        int maxHpInt = Math.round(maxHp);
        if (curHpInt != lastCurHpInt || maxHpInt != lastMaxHpInt) {
            lastCurHpInt = curHpInt;
            lastMaxHpInt = maxHpInt;
            cachedHpStr = curHpInt + "/" + maxHpInt;
        }

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
            int bgCol = RenderUtils.multiplyAlpha(EclipseColors.BG_HUD, animAlpha);
            int outlineCol = RenderUtils.multiplyAlpha(EclipseColors.BORDER, animAlpha);
            int cardCol = RenderUtils.multiplyAlpha(EclipseColors.BG_CARD, animAlpha);
            int textCol = RenderUtils.multiplyAlpha(EclipseColors.TEXT_PRIMARY, animAlpha);
            int subtextCol = RenderUtils.multiplyAlpha(EclipseColors.TEXT_MUTED_HUD, animAlpha);

            // 1. Плоская монохромная подложка
            RenderUtils.drawFlatCard(context, 0, 0, this.width, this.height, bgCol, outlineCol);

            // 2. Аватарка / Рамка под модельку (7, 7, 30, 30)
            int avatarSize = 30;
            int avatarX = 7;
            int avatarY = 7;
            RenderUtils.drawFlatCard(context, avatarX, avatarY, avatarSize, avatarSize, cardCol, outlineCol);

            if (displayEntity != null && cfg.targetHud3DModel) {
                try {
                    float rotAngle = (float) ((now % 4500L) / 4500.0 * Math.PI * 2);
                    float lookX = (avatarX + avatarSize / 2f) + (float) Math.sin(rotAngle) * 35f;
                    float lookY = (avatarY + avatarSize / 2f) - 8f;
                    InventoryScreen.drawEntity(
                            context, avatarX, avatarY, avatarX + avatarSize, avatarY + avatarSize,
                            14, 0.0625f, lookX, lookY, displayEntity
                    );
                } catch (Throwable ignored) {}
            } else {
                RenderUtils.drawCenteredTextWithShadow(context, client.textRenderer, "E", avatarX + avatarSize / 2, avatarY + 11, textCol);
            }

            // 3. Тексты
            RenderUtils.drawTextWithShadow(context, client.textRenderer, cachedTargetName, 42, 9, textCol);
            RenderUtils.drawTextWithShadow(context, client.textRenderer, cachedDistStr, this.width - cachedDistW - 7, 9, subtextCol);

            // 4. Полоса здоровья (монохром)
            int barX = 42;
            int barY = 24;
            int barW = this.width - barX - 7; // 111 px
            int barH = 8;
            float hpRatio = MathHelper.clamp(animHp / maxHp, 0f, 1f);

            context.fill(barX, barY, barX + barW, barY + barH, RenderUtils.multiplyAlpha(EclipseColors.TRACK_BG, animAlpha));
            int fillW = (int) (barW * hpRatio);
            if (fillW > 0) {
                context.fill(barX, barY, barX + fillW, barY + barH, RenderUtils.multiplyAlpha(EclipseColors.ACCENT_WHITE, animAlpha));
            }

            RenderUtils.drawCenteredTextWithShadow(context, client.textRenderer, cachedHpStr, barX + barW / 2, barY - 1,
                    RenderUtils.multiplyAlpha(hpRatio > 0.55f ? EclipseColors.TEXT_ON_ACCENT : EclipseColors.TEXT_PRIMARY, animAlpha));
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
