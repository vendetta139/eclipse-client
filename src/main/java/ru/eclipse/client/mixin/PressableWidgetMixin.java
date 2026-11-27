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
import ru.example.donorprefix.gui.EclipseColors;
import ru.example.donorprefix.gui.RenderUtils;

/**
 * PressableWidgetMixin — Строгий монохромный стиль кнопок ECLIPSE 1.21.11.
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
                int bgCol = hovered ? EclipseColors.BG_HOVER : EclipseColors.BG_CARD;
                int borderCol = hovered ? EclipseColors.BORDER_ACTIVE : EclipseColors.BORDER;
                RenderUtils.drawFlatCard(context, x, y, w, h, bgCol, borderCol);

                int textColor = hovered ? EclipseColors.TEXT_PRIMARY : EclipseColors.TEXT_SECONDARY;
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
                bgCol = EclipseColors.BG_WINDOW;
                borderCol = EclipseColors.BORDER;
                textColor = EclipseColors.TEXT_DISABLED;
                RenderUtils.drawFlatCard(context, x, y, w, h, bgCol, borderCol);
            } else {
                bgCol = AnimationUtil.lerpColor(EclipseColors.BG_CARD, EclipseColors.BG_HOVER, eclipseHoverAnim);
                int defaultBorder = isEclipseBtn ? EclipseColors.TEXT_SECONDARY : EclipseColors.BORDER;
                borderCol = AnimationUtil.lerpColor(defaultBorder, EclipseColors.BORDER_ACTIVE, eclipseHoverAnim);
                RenderUtils.drawFlatCard(context, x, y, w, h, bgCol, borderCol);

                // Чёткая белая волна при клике (без цветных акцентов)
                long rippleAge = now - eclipseRippleStartTime;
                if (rippleAge < 300) {
                    float ripProg = (float) rippleAge / 300.0f;
                    float maxR = Math.max(w, h) * 0.85f;
                    float curR = ripProg * maxR;
                    int ripAlpha = (int) ((1.0f - ripProg) * 38.0f);
                    if (ripAlpha > 2) {
                        int ripColor = (ripAlpha << 24) | (EclipseColors.ACCENT_WHITE & 0x00FFFFFF);
                        int rx1 = (int) Math.max(x + 1, x + eclipseRippleX - curR);
                        int ry1 = (int) Math.max(y + 1, y + eclipseRippleY - curR);
                        int rx2 = (int) Math.min(x + w - 1, x + eclipseRippleX + curR);
                        int ry2 = (int) Math.min(y + h - 1, y + eclipseRippleY + curR);
                        if (rx2 > rx1 && ry2 > ry1) {
                            context.fill(rx1, ry1, rx2, ry2, ripColor);
                        }
                    }
                }

                textColor = EclipseColors.TEXT_PRIMARY;
            }

            int yShift = this.active ? (int) (-1.0f * eclipseHoverAnim + 1.0f * eclipsePressAnim) : 0;
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
