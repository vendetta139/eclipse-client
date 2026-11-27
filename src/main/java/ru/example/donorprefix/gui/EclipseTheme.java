package ru.example.donorprefix.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * ECLIPSE THEME — Монохромная дизайн-система клиента ECLIPSE.
 */
public final class EclipseTheme {

    public static final int BG_DEEP = EclipseColors.BG_WINDOW;
    public static final int PANEL_BG = EclipseColors.BG_WINDOW_TRANSLUCENT;
    public static final int PANEL_BG_OPAQUE = EclipseColors.BG_CARD;
    public static final int PANEL_BG_CARD = EclipseColors.BG_CARD;

    public static final int PANEL_BORDER = EclipseColors.BORDER;
    public static final int PANEL_BORDER_HOVER = EclipseColors.BORDER_ACTIVE;

    public static final int ACCENT_WHITE = EclipseColors.ACCENT_WHITE;
    public static final int TEXT_PRIMARY = EclipseColors.TEXT_PRIMARY;
    public static final int TEXT_MUTED = EclipseColors.TEXT_SECONDARY;
    public static final int TRACK_BG = EclipseColors.TRACK_BG;

    public static final Identifier ECLIPSE_ICON = Identifier.of("donor-prefix-client", "textures/gui/eclipse_icon.png");

    private EclipseTheme() {}

    public static int lerpColor(int c1, int c2, float t) {
        return RenderUtils.lerpColor(c1, c2, t);
    }

    public static int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255.0f)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    public static int multiplyAlpha(int color, float factor) {
        return RenderUtils.multiplyAlpha(color, factor);
    }

    public static void drawVerticalGradient(DrawContext context, int x, int y, int width, int height, int topColor, int bottomColor) {
        if (width <= 0 || height <= 0 || context == null) return;
        context.fillGradient(x, y, x + width, y + height, topColor, bottomColor);
    }

    public static void drawEclipseBadge(DrawContext context, int x, int y, int size, float alpha) {
        drawEclipseBadge(context, x, y, size, alpha, 0f);
    }

    public static void drawEclipseBadge(DrawContext context, int x, int y, int size, float alpha, float animationTime) {
        if (context == null || size <= 0 || alpha <= 0.001f) return;

        float centerX = x + size / 2.0f;
        float centerY = y + size / 2.0f;
        float radius = Math.max(4.0f, (size / 2.0f) - 2.0f);
        float timeSec = (System.currentTimeMillis() % 10000000L) / 1000.0f;
        drawProceduralEclipseRing(context, centerX, centerY, radius, alpha, timeSec);
    }

    /**
     * Монохромный знак затмения: чёрный внутренний диск (#09090B) и тонкое резкое белое кольцо короны (#F4F4F5).
     */
    public static void drawProceduralEclipseRing(DrawContext context, float cx, float cy, float radius, float alpha, float timeSec) {
        if (context == null || alpha <= 0.001f) return;

        try {
            float rimThickness = Math.max(1.2f, radius * 0.14f);
            float rimOuter = radius + rimThickness * 0.5f;
            float rimInner = Math.max(1.0f, radius - rimThickness * 0.5f);
            int rimOuterInt = (int) Math.ceil(rimOuter);

            // Тонкая мягкая белая корона вокруг кольца (строго ахроматическая)
            int haloOuterInt = rimOuterInt + Math.max(1, Math.round(radius * 0.18f));
            for (int dy = -haloOuterInt; dy <= haloOuterInt; dy++) {
                int y = Math.round(cy + dy);
                double dySq = dy * dy;
                double outX = Math.sqrt(Math.max(0.0, haloOuterInt * haloOuterInt - dySq));
                double inX = (Math.abs(dy) <= rimOuter) ? Math.sqrt(Math.max(0.0, rimOuter * rimOuter - dySq)) : 0.0;
                float distFactor = 1.0f - (float) Math.abs(dy) / (float) Math.max(1, haloOuterInt);
                int haloCol = withAlpha(EclipseColors.BORDER_ACTIVE, 0.14f * distFactor * alpha);

                int xl1 = Math.round(cx - (float) outX);
                int xl2 = Math.round(cx - (float) inX);
                if (xl2 > xl1) context.fill(xl1, y, xl2, y + 1, haloCol);

                int xr1 = Math.round(cx + (float) inX);
                int xr2 = Math.round(cx + (float) outX);
                if (xr2 > xr1) context.fill(xr1, y, xr2, y + 1, haloCol);
            }

            // Резкое белое кольцо короны
            int rimColor = withAlpha(EclipseColors.TEXT_PRIMARY, 0.96f * alpha);
            for (int dy = -rimOuterInt; dy <= rimOuterInt; dy++) {
                int y = Math.round(cy + dy);
                double dySq = dy * dy;
                if (dySq > rimOuter * rimOuter) continue;

                double outX = Math.sqrt(rimOuter * rimOuter - dySq);
                double inX = (dySq < rimInner * rimInner) ? Math.sqrt(rimInner * rimInner - dySq) : 0.0;

                int xl1 = Math.round(cx - (float) outX);
                int xl2 = Math.round(cx - (float) inX);
                if (xl2 > xl1) context.fill(xl1, y, xl2, y + 1, rimColor);

                int xr1 = Math.round(cx + (float) inX);
                int xr2 = Math.round(cx + (float) outX);
                if (xr2 > xr1) context.fill(xr1, y, xr2, y + 1, rimColor);
            }

            // Чёрное тело диска Луны (#09090B)
            int moonRInt = (int) Math.floor(rimInner);
            int moonColor = withAlpha(EclipseColors.BG_WINDOW, alpha);
            for (int dy = -moonRInt; dy <= moonRInt; dy++) {
                int y = Math.round(cy + dy);
                double dx = Math.sqrt(Math.max(0, rimInner * rimInner - dy * dy));
                int x1 = Math.round(cx - (float) dx);
                int x2 = Math.round(cx + (float) dx);
                if (x2 > x1) {
                    context.fill(x1, y, x2, y + 1, moonColor);
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void drawProceduralSplashTypography(DrawContext context, TextRenderer textRenderer, int screenWidth, int screenHeight, float alpha, float timeSec) {
        if (context == null || textRenderer == null || alpha <= 0.001f) return;

        try {
            float pulse = 0.88f + 0.12f * (float) Math.sin(timeSec * 2.0f);
            int titleColor = withAlpha(EclipseColors.TEXT_PRIMARY, alpha * pulse);
            int titleY = screenHeight / 2 + 32;
            RenderUtils.drawCenteredTextWithShadow(context, textRenderer, Text.literal("E  C  L  I  P  S  E"), screenWidth / 2, titleY, titleColor);

            int subColor = withAlpha(EclipseColors.TEXT_SECONDARY, alpha * 0.9f);
            RenderUtils.drawCenteredTextWithShadow(context, textRenderer, Text.literal("C L I E N T   1 . 2 1 . 1 1"), screenWidth / 2, titleY + 14, subColor);
        } catch (Throwable ignored) {}
    }

    public static void drawSplashProgressBar(DrawContext context, TextRenderer textRenderer, int screenWidth, int screenHeight, float smoothProgress, float alpha) {
        if (context == null || alpha <= 0.001f) return;

        try {
            int barWidth = 180;
            int barHeight = 2;
            int barX = (screenWidth - barWidth) / 2;
            int barY = screenHeight - 48;

            context.fill(barX, barY, barX + barWidth, barY + barHeight, withAlpha(EclipseColors.TRACK_BG, alpha));

            int fillWidth = Math.round(barWidth * Math.max(0.0f, Math.min(1.0f, smoothProgress)));
            if (fillWidth > 0) {
                context.fill(barX, barY, barX + fillWidth, barY + barHeight, withAlpha(EclipseColors.TEXT_PRIMARY, alpha));
            }

            if (textRenderer != null) {
                int percent = Math.max(0, Math.min(100, Math.round(smoothProgress * 100.0f)));
                int pctColor = withAlpha(EclipseColors.TEXT_SECONDARY, alpha * 0.85f);
                RenderUtils.drawCenteredTextWithShadow(context, textRenderer, Text.literal(percent + "%"), screenWidth / 2, barY - 12, pctColor);
            }
        } catch (Throwable ignored) {}
    }
}
