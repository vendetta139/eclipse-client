package ru.eclipse.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.example.donorprefix.gui.AnimationUtil;
import ru.example.donorprefix.gui.CosmicBackgroundRenderer;
import ru.example.donorprefix.gui.EclipseTheme;
import ru.example.donorprefix.gui.RenderUtils;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow public int width;
    @Shadow public int height;
    @Shadow protected MinecraftClient client;

    @Unique
    private float eclipseOpenProgress = 0.0f;
    @Unique
    private long eclipseLastFrameTime = 0;
    @Unique
    private boolean eclipseMatrixPushed = false;

    @Inject(method = "init(II)V", at = @At("TAIL"))
    private void onScreenInit(int width, int height, CallbackInfo ci) {
        this.eclipseOpenProgress = 0.0f;
        this.eclipseLastFrameTime = System.currentTimeMillis();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onScreenRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (context == null || !shouldApplyTransition()) return;

        long now = System.currentTimeMillis();
        float deltaSec = eclipseLastFrameTime > 0 ? (now - eclipseLastFrameTime) / 1000.0f : 0.016f;
        deltaSec = Math.max(0.0f, Math.min(0.1f, deltaSec));
        eclipseLastFrameTime = now;

        float animSpeed = isFastTransition() ? 26.0f : 18.0f;
        eclipseOpenProgress = AnimationUtil.deltaLerp(eclipseOpenProgress, 1.0f, animSpeed, deltaSec);
        float t = AnimationUtil.easeOutCubic(eclipseOpenProgress);

        if (t < 0.999f) {
            float scale = 0.975f + 0.025f * t;
            float cx = this.width / 2.0f;
            float cy = this.height / 2.0f;

            Matrix3x2fStack matrices = context.getMatrices();
            if (matrices != null) {
                matrices.pushMatrix();
                matrices.translate(cx, cy);
                matrices.scale(scale, scale);
                matrices.translate(-cx, -cy);
                eclipseMatrixPushed = true;
            }
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onScreenRenderReturn(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (eclipseMatrixPushed) {
            eclipseMatrixPushed = false;
            if (context != null && context.getMatrices() != null) {
                context.getMatrices().popMatrix();
            }
        }

        Object self = this;
        if (self instanceof GameMenuScreen && context != null && this.client != null) {
            drawGameMenuBadge(context);
        }
    }

    @Inject(method = "renderWithTooltip", at = @At("RETURN"))
    private void onScreenRenderWithTooltipReturn(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (context != null && shouldApplyTransition() && eclipseOpenProgress < 0.99f) {
            float t = AnimationUtil.easeOutCubic(eclipseOpenProgress);
            int fadeAlpha = (int) ((1.0f - t) * 60.0f);
            if (fadeAlpha > 2) {
                context.fill(0, 0, this.width, this.height, (fadeAlpha << 24) | 0x0B0B10);
            }
        }
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void onRenderGlobalBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Object self = this;
        if (self instanceof net.minecraft.client.gui.screen.TitleScreen) return;

        ci.cancel();
        if (context == null || this.client == null) return;

        if (this.client.world == null) {
            CosmicBackgroundRenderer.render(context, this.width, this.height, 1.0f);
        } else {
            context.fillGradient(0, 0, this.width, this.height, 0xB00B0B10, 0xD012131C);
        }
    }

    @Inject(method = "renderInGameBackground", at = @At("HEAD"), cancellable = true)
    private void onRenderInGameBg(DrawContext context, CallbackInfo ci) {
        ci.cancel();
        if (context != null) {
            context.fillGradient(0, 0, this.width, this.height, 0xB00B0B10, 0xD012131C);
        }
    }

    @Inject(method = "renderDarkening(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderDarkening(DrawContext context, CallbackInfo ci) {
        ci.cancel();
        if (context != null) {
            context.fillGradient(0, 0, this.width, this.height, 0xB00B0B10, 0xD012131C);
        }
    }

    @Unique
    private boolean shouldApplyTransition() {
        Object self = this;
        if (self instanceof net.minecraft.client.gui.screen.ChatScreen) return false;
        return true;
    }

    @Unique
    private boolean isFastTransition() {
        return (Object) this instanceof net.minecraft.client.gui.screen.ingame.HandledScreen;
    }

    @Unique
    private void drawGameMenuBadge(DrawContext context) {
        try {
            TextRenderer tr = this.client.textRenderer;
            if (tr == null) return;

            int badgeW = 92;
            int badgeH = 20;
            int badgeX = (this.width - badgeW) / 2;
            int badgeY = 16;

            RenderUtils.drawPill(context, badgeX, badgeY, badgeW, badgeH, 0xE612131C, 0xFF232533);
            RenderUtils.drawSoftGlow(context, badgeX, badgeY, badgeW, badgeH, 5, 2, 0x336A67CE);

            EclipseTheme.drawEclipseBadge(context, badgeX + 6, badgeY + 2, 16, 1.0f);
            RenderUtils.drawTextWithShadow(context, tr, Text.literal("✦ ECLIPSE"), badgeX + 26, badgeY + 6, 0xFFEAECEF);
        } catch (Throwable ignored) {}
    }
}
