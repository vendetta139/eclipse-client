package ru.eclipse.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.example.donorprefix.gui.AnimationUtil;
import ru.example.donorprefix.gui.EclipseTheme;
import ru.example.donorprefix.gui.RenderUtils;

/**
 * ✦ PressableWidgetMixin — Флагманский дизайн кнопок ECLIPSE CLIENT 1.21.11:
 * - Глубокий темный матовый обсидиан (#0B0B10 / #12131C)
 * - Плавная градиентная обводка (золото #F5A65B -> индиго #6D28D9)
 * - Тактильное упругое сжатие при клике (LabyMod-style press squish) через Matrix3x2fStack
 * - Интерактивная неоновая расходящаяся волна (Click Ripple)
 * - Микро-сдвиг текста вверх при наведении и мягкое неоновое свечение
 */
@Mixin(PressableWidget.class)
public abstract class PressableWidgetMixin extends ClickableWidget {

    public PressableWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Unique
    private float eclipseHoverAnim = 0.0f;
    @Unique
    private float eclipsePressAnim = 0.0f;
    @Unique
    private boolean eclipseWasPressed = false;
    @Unique
    private long eclipseRippleStartTime = 0;
    @Unique
    private float eclipseRippleX = 0f;
    @Unique
    private float eclipseRippleY = 0f;
    @Unique
    private long eclipseLastTime = 0;

    @Inject(method = "onClick(Lnet/minecraft/client/gui/Click;Z)V", at = @At("HEAD"))
    private void onEclipseWidgetClick(Click click, boolean bl, CallbackInfo ci) {
        if (!this.active) return;
        this.eclipseRippleStartTime = System.currentTimeMillis();
        if (click != null) {
            this.eclipseRippleX = (float) (click.x() - this.getX());
            this.eclipseRippleY = (float) (click.y() - this.getY());
        } else {
            this.eclipseRippleX = this.getWidth() / 2.0f;
            this.eclipseRippleY = this.getHeight() / 2.0f;
        }
    }

    @Inject(method = "renderWidget(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), cancellable = true)
    private void onRenderEclipseWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null || context == null) return;

        ci.cancel();

        long now = System.currentTimeMillis();
        float deltaSec = eclipseLastTime > 0 ? Math.min(0.1f, (now - eclipseLastTime) / 1000.0f) : 0.016f;
        eclipseLastTime = now;

        boolean hovered = this.isHovered();
        eclipseHoverAnim = AnimationUtil.deltaLerp(eclipseHoverAnim, hovered ? 1.0f : 0.0f, 14.0f, deltaSec);

        // Определение физического нажатия ЛКМ на кнопку
        long windowHandle = mc.getWindow().getHandle();
        boolean isMouseDown = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean isPressed = hovered && isMouseDown && this.active;

        if (isPressed && !eclipseWasPressed) {
            eclipseRippleStartTime = now;
            eclipseRippleX = mouseX - this.getX();
            eclipseRippleY = mouseY - this.getY();
        }
        eclipseWasPressed = isPressed;

        eclipsePressAnim = AnimationUtil.deltaLerp(eclipsePressAnim, isPressed ? 1.0f : 0.0f, 22.0f, deltaSec);

        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        boolean isEclipseBtn = this.getMessage().getString().contains("ECLIPSE");
        boolean isFooterBtn = mc.currentScreen != null && y >= mc.currentScreen.height - 35;

        // 1. Применяем тактильное упругое сжатие при клике через нативный Matrix3x2fStack 1.21.11
        boolean applyPressScale = eclipsePressAnim > 0.002f;
        Matrix3x2fStack matrices = context.getMatrices();
        if (applyPressScale && matrices != null) {
            float pressScale = 1.0f - 0.045f * eclipsePressAnim;
            matrices.pushMatrix();
            matrices.translate(x + w / 2.0f, y + h / 2.0f);
            matrices.scale(pressScale, pressScale);
            matrices.translate(-(x + w / 2.0f), -(y + h / 2.0f));
        }

        try {
            if (isFooterBtn) {
                // Компактные нижние кнопки Главного меню
                int bgCol = hovered ? 0xE6191B28 : 0xCC12131C;
                RenderUtils.drawRoundedRect(context, x, y, w, h, 4, bgCol);
                int borderCol = hovered ? EclipseTheme.ACCENT_GOLD : 0x88232533;
                RenderUtils.drawRoundedOutline(context, x, y, w, h, 4, borderCol);

                int textColor = hovered ? 0xFFFFFFFF : 0xFFB0B4C8;
                TextRenderer tr = mc.textRenderer;
                if (tr != null) {
                    RenderUtils.drawCenteredTextWithShadow(context, tr, this.getMessage(), x + w / 2, y + (h - 8) / 2, textColor);
                }
                return;
            }

            int bgCol;
            int borderCol;
            int textColor;

            if (!this.active) {
                // Приглушенный стиль неактивной кнопки: фон #0D0E14, рамка #1A1B24, текст #4A4E60
                bgCol = 0x990D0E14;
                borderCol = 0x881A1B24;
                textColor = 0xFF4A4E60;
                RenderUtils.drawRoundedRect(context, x, y, w, h, 5, bgCol);
                RenderUtils.drawRoundedOutline(context, x, y, w, h, 5, borderCol);
            } else {
                // 1. Плавный цвет фона (от глубокого #12131C к более светлому #191B28)
                bgCol = AnimationUtil.lerpColor(0xE612131C, 0xF5191B28, eclipseHoverAnim);
                RenderUtils.drawRoundedRect(context, x, y, w, h, 5, bgCol);

                // 2. Плавная обводка (от приглушенного #232533 к акцентному золотисто-фиолетовому)
                int defaultBorder = isEclipseBtn
                        ? EclipseTheme.lerpColor(EclipseTheme.ACCENT_GOLD, EclipseTheme.ACCENT_INDIGO, 0.5f)
                        : EclipseTheme.PANEL_BORDER;
                int hoverBorder = isEclipseBtn ? EclipseTheme.ACCENT_GOLD : EclipseTheme.ACCENT_INDIGO;
                borderCol = AnimationUtil.lerpColor(defaultBorder, hoverBorder, eclipseHoverAnim);
                RenderUtils.drawRoundedOutline(context, x, y, w, h, 5, borderCol);

                // 3. Мягкое неоновое свечение при наведении или нажатии
                if (eclipseHoverAnim > 0.05f) {
                    int glowCol = EclipseTheme.multiplyAlpha(EclipseTheme.ACCENT_INDIGO, (eclipseHoverAnim * 0.40f) + (eclipsePressAnim * 0.25f));
                    RenderUtils.drawSoftGlow(context, x, y, w, h, 5, 3, glowCol);
                } else if (isEclipseBtn) {
                    int glowCol = EclipseTheme.multiplyAlpha(EclipseTheme.ACCENT_GOLD, 0.15f);
                    RenderUtils.drawSoftGlow(context, x, y, w, h, 5, 2, glowCol);
                }

                // 4. Интерактивная расходящаяся неоновая волна при клике (Click Ripple)
                long rippleAge = now - eclipseRippleStartTime;
                if (rippleAge < 360) {
                    float ripProg = (float) rippleAge / 360.0f;
                    float maxR = Math.max(w, h) * 0.85f;
                    float curR = ripProg * maxR;
                    int ripAlpha = (int) ((1.0f - ripProg) * 110.0f);
                    if (ripAlpha > 2) {
                        int ripColor = (ripAlpha << 24) | (EclipseTheme.ACCENT_GOLD & 0x00FFFFFF);
                        int rx1 = (int) Math.max(x + 1, x + eclipseRippleX - curR);
                        int ry1 = (int) Math.max(y + 1, y + eclipseRippleY - curR);
                        int rx2 = (int) Math.min(x + w - 1, x + eclipseRippleX + curR);
                        int ry2 = (int) Math.min(y + h - 1, y + eclipseRippleY + curR);
                        if (rx2 > rx1 && ry2 > ry1) {
                            context.fill(rx1, ry1, rx2, ry2, ripColor);
                        }
                    }
                }

                textColor = AnimationUtil.lerpColor(isEclipseBtn ? EclipseTheme.ACCENT_GOLD : EclipseTheme.TEXT_PRIMARY, 0xFFFFFFFF, eclipseHoverAnim);
            }

            // 5. Легкий тактильный сдвиг текста на 1.5px вверх при наведении и возврат при нажатии
            int yShift = this.active ? (int) (-1.5f * eclipseHoverAnim + 1.0f * eclipsePressAnim) : 0;

            TextRenderer tr = mc.textRenderer;
            if (tr != null) {
                RenderUtils.drawCenteredTextWithShadow(context, tr, this.getMessage(), x + w / 2, y + (h - 8) / 2 + yShift, textColor);
            }
        } finally {
            if (applyPressScale && matrices != null) {
                matrices.popMatrix();
            }
        }
    }
}
