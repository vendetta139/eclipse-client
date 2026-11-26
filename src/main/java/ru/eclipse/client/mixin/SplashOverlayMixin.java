package ru.eclipse.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.example.donorprefix.gui.EclipseTheme;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private ResourceReload reload;
    @Shadow @Final private Consumer<Optional<Throwable>> exceptionHandler;
    @Shadow @Final private boolean reloading;
    @Shadow private float progress;
    @Shadow private long reloadCompleteTime;
    @Shadow private long reloadStartTime;

    @Unique
    private static float eclipseSmoothProgress = 0.0f;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderCustomSplash(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();

        if (context == null || this.client == null) return;

        try {
            int screenWidth = context.getScaledWindowWidth();
            int screenHeight = context.getScaledWindowHeight();
            long now = Util.getMeasuringTimeMs();

            if (this.reloading && this.reloadStartTime == -1L) {
                this.reloadStartTime = now;
            }

            float fadeOutDelta = this.reloadCompleteTime > -1L ? (float)(now - this.reloadCompleteTime) / 1000.0f : -1.0f;
            float fadeInDelta = this.reloadStartTime > -1L ? (float)(now - this.reloadStartTime) / 500.0f : -1.0f;

            float overlayAlpha;
            if (fadeOutDelta >= 1.0f) {
                if (this.client.currentScreen != null) {
                    try {
                        this.client.currentScreen.render(context, 0, 0, delta);
                    } catch (Throwable ignored) {}
                }
                overlayAlpha = 1.0f - MathHelper.clamp(fadeOutDelta - 1.0f, 0.0f, 1.0f);
            } else if (this.reloading) {
                if (this.client.currentScreen != null && fadeInDelta < 1.0f) {
                    try {
                        this.client.currentScreen.render(context, mouseX, mouseY, delta);
                    } catch (Throwable ignored) {}
                }
                overlayAlpha = MathHelper.clamp(fadeInDelta, 0.0f, 1.0f);
            } else {
                overlayAlpha = 1.0f;
            }

            if (this.reload != null) {
                float rawProgress = this.reload.getProgress();
                this.progress = MathHelper.clamp(this.progress * 0.95f + rawProgress * 0.05f, 0.0f, 1.0f);
            }
            eclipseSmoothProgress = MathHelper.lerp(0.14f, eclipseSmoothProgress, this.progress);

            if (overlayAlpha > 0.001f) {
                int bgAlpha = MathHelper.clamp(Math.round(overlayAlpha * 255.0f), 0, 255);
                int bgColor = (bgAlpha << 24) | (EclipseTheme.BG_DEEP & 0x00FFFFFF);
                context.fill(0, 0, screenWidth, screenHeight, bgColor);

                float timeSec = (now % 10000000L) / 1000.0f;
                float centerX = screenWidth / 2.0f;
                float centerY = (screenHeight / 2.0f) - 22.0f;
                float ringRadius = 38.0f;

                EclipseTheme.drawProceduralEclipseRing(context, centerX, centerY, ringRadius, overlayAlpha, timeSec);

                TextRenderer tr = this.client.textRenderer;
                if (tr != null) {
                    EclipseTheme.drawProceduralSplashTypography(context, tr, screenWidth, screenHeight, overlayAlpha, timeSec);
                }

                if (fadeOutDelta < 1.0f) {
                    EclipseTheme.drawSplashProgressBar(context, tr, screenWidth, screenHeight, eclipseSmoothProgress, overlayAlpha);
                }
            }

            if (fadeOutDelta >= 2.0f) {
                this.client.setOverlay(null);
                eclipseSmoothProgress = 0.0f;
            }

            if (this.reloadCompleteTime == -1L && this.reload != null && this.reload.isComplete() && (!this.reloading || fadeInDelta >= 2.0f)) {
                try {
                    this.reload.throwException();
                    if (this.exceptionHandler != null) {
                        this.exceptionHandler.accept(Optional.empty());
                    }
                } catch (Throwable t) {
                    if (this.exceptionHandler != null) {
                        this.exceptionHandler.accept(Optional.of(t));
                    }
                }
                this.reloadCompleteTime = Util.getMeasuringTimeMs();
                if (this.client.currentScreen != null) {
                    try {
                        this.client.currentScreen.init(screenWidth, screenHeight);
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
            System.err.println("[EclipseClient] Error during SplashOverlay render: " + t.getMessage());
        }
    }
}
