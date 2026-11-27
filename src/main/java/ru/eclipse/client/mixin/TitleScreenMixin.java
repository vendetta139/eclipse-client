package ru.eclipse.client.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.realms.gui.screen.RealmsNotificationsScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.eclipse.client.ui.clickgui.EclipseClickGui;
import ru.example.donorprefix.gui.AnimationUtil;
import ru.example.donorprefix.gui.CosmicBackgroundRenderer;
import ru.example.donorprefix.gui.EclipseColors;
import ru.example.donorprefix.gui.EclipseTheme;
import ru.example.donorprefix.gui.RenderUtils;

import java.util.ArrayList;
import java.util.List;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Shadow private SplashTextRenderer splashText;
    @Shadow @Final private LogoDrawer logoDrawer;

    @Unique
    private float eclipseScreenOpen = 0.0f;
    @Unique
    private long eclipseLastRenderTime = 0;

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (context != null) {
            context.fill(0, 0, this.width, this.height, EclipseColors.BG_WINDOW);
            CosmicBackgroundRenderer.render(context, this.width, this.height, 1.0f);
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/TitleScreen;renderPanoramaBackground(Lnet/minecraft/client/gui/DrawContext;F)V"))
    private void onRedirectPanorama(TitleScreen instance, DrawContext context, float delta) {
        // Vanilla panorama is suppressed; Cosmic background is drawn at HEAD
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/LogoDrawer;draw(Lnet/minecraft/client/gui/DrawContext;IF)V"))
    private void onHideVanillaLogo(LogoDrawer instance, DrawContext context, int width, float alpha) {
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/realms/gui/screen/RealmsNotificationsScreen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    private void onHideRealmsNotifications(RealmsNotificationsScreen instance, DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"))
    private void onRedirectVersionText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color) {
        if (textRenderer != null && context != null) {
            String customVer = "ECLIPSE CLIENT 1.21.11 | by alone.vendetta";
            RenderUtils.drawTextWithShadow(context, textRenderer, customVer, 12, this.height - 12, EclipseColors.TEXT_SECONDARY);
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInitCustomLayout(CallbackInfo ci) {
        this.splashText = null;
        this.eclipseScreenOpen = 0.0f;
        this.eclipseLastRenderTime = System.currentTimeMillis();

        int colW = 180;
        int colH = 22;
        int gap = 6;
        int colX = (this.width - colW) / 2;
        int startY = (int) (this.height * 0.38f);

        ClickableWidget singleBtn = null;
        ClickableWidget multiBtn = null;
        ClickableWidget realmsBtn = null;
        ClickableWidget optionsBtn = null;
        ClickableWidget quitBtn = null;
        ClickableWidget langBtn = null;
        ClickableWidget accessBtn = null;
        ClickableWidget existingEclipseBtn = null;

        for (Element child : this.children()) {
            if (child instanceof ClickableWidget widget) {
                String str = widget.getMessage().getString().toLowerCase();

                boolean isCopyright = str.contains("mojang") || str.contains("copyright") || str.contains("credits") || widget.getClass().getSimpleName().contains("PressableTextWidget");
                if (isCopyright) {
                    widget.visible = false;
                    widget.active = false;
                    widget.setX(-1000);
                    widget.setY(-1000);
                    continue;
                }

                if (str.contains("eclipse")) {
                    existingEclipseBtn = widget;
                } else if (str.contains("одиноч") || str.contains("single") || str.contains("solo")) {
                    singleBtn = widget;
                } else if (str.contains("сетев") || str.contains("multi")) {
                    multiBtn = widget;
                } else if (str.contains("realms")) {
                    realmsBtn = widget;
                } else if (str.contains("настройк") || str.contains("option")) {
                    optionsBtn = widget;
                } else if (str.contains("выйти") || str.contains("выход") || str.contains("quit")) {
                    quitBtn = widget;
                } else if (str.contains("язык") || str.contains("lang")) {
                    langBtn = widget;
                } else if (str.contains("доступ") || str.contains("access")) {
                    accessBtn = widget;
                } else if (widget.getWidth() <= 24 && widget.getHeight() <= 24) {
                    if (langBtn == null) {
                        langBtn = widget;
                    } else if (accessBtn == null) {
                        accessBtn = widget;
                    }
                }
            }
        }

        ClickableWidget eclipseBtn = existingEclipseBtn;
        if (eclipseBtn == null) {
            eclipseBtn = ButtonWidget.builder(
                    Text.literal("ECLIPSE CLIENT"),
                    b -> {
                        if (this.client != null) {
                            this.client.setScreen(new EclipseClickGui());
                        }
                    }
            ).dimensions(colX, startY, colW, colH).build();
            this.addDrawableChild(eclipseBtn);
        }

        List<ClickableWidget> column = new ArrayList<>();
        if (singleBtn != null) column.add(singleBtn);
        if (multiBtn != null) column.add(multiBtn);
        column.add(eclipseBtn);
        if (realmsBtn != null) column.add(realmsBtn);
        if (optionsBtn != null) column.add(optionsBtn);
        if (quitBtn != null) column.add(quitBtn);

        int curY = startY;
        for (ClickableWidget widget : column) {
            widget.setX(colX);
            widget.setY(curY);
            widget.setWidth(colW);
            widget.setHeight(colH);
            curY += colH + gap;
        }

        int bottomY = this.height - 28;
        int smallH = 18;
        TextRenderer tr = this.client != null ? this.client.textRenderer : null;

        if (langBtn != null && accessBtn != null) {
            int langW = tr != null ? Math.max(76, tr.getWidth(langBtn.getMessage()) + 16) : 84;
            int accessW = tr != null ? Math.max(76, tr.getWidth(accessBtn.getMessage()) + 16) : 130;
            int totalW = langW + 8 + accessW;
            int startX = (this.width - totalW) / 2;

            langBtn.setX(startX);
            langBtn.setY(bottomY);
            langBtn.setWidth(langW);
            langBtn.setHeight(smallH);

            accessBtn.setX(startX + langW + 8);
            accessBtn.setY(bottomY);
            accessBtn.setWidth(accessW);
            accessBtn.setHeight(smallH);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderCustomTitleScreen(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (context == null || this.client == null) return;

        try {
            long now = Util.getMeasuringTimeMs();
            float deltaSec = eclipseLastRenderTime > 0 ? (now - eclipseLastRenderTime) / 1000.0f : 0.016f;
            eclipseLastRenderTime = now;

            eclipseScreenOpen = AnimationUtil.deltaLerp(eclipseScreenOpen, 1.0f, 8.0f, deltaSec);
            float alpha = AnimationUtil.easeOutCubic(eclipseScreenOpen);

            int centerX = this.width / 2;
            int slideY = (int) ((1.0f - alpha) * 8.0f);
            int headerY = (int) (this.height * 0.15f) + slideY;

            int iconSize = 28;
            EclipseTheme.drawEclipseBadge(context, centerX - iconSize / 2, headerY, iconSize, alpha);

            int titleY = headerY + iconSize + 8;
            float timeSec = (now % 10000000L) / 1000.0f;
            float pulse = 0.90f + 0.10f * (float) Math.sin(timeSec * 2.2f);
            int titleCol = EclipseTheme.withAlpha(EclipseTheme.TEXT_PRIMARY, alpha * pulse);

            TextRenderer tr = this.client.textRenderer;
            if (tr != null) {
                RenderUtils.drawCenteredTextWithShadow(context, tr, Text.literal("E  C  L  I  P  S  E"), centerX, titleY, titleCol);
                int subY = titleY + 13;
                int subCol = EclipseTheme.withAlpha(EclipseTheme.TEXT_MUTED, alpha * 0.85f);
                RenderUtils.drawCenteredTextWithShadow(context, tr, Text.literal("C L I E N T   1 . 2 1 . 1 1"), centerX, subY, subCol);

                String copyright = "© Mojang AB | by alone.vendetta";
                int copyW = tr.getWidth(copyright);
                RenderUtils.drawTextWithShadow(context, tr, copyright, this.width - copyW - 10, this.height - 15, EclipseColors.TEXT_SECONDARY);
            }
        } catch (Throwable ignored) {}
    }
}
