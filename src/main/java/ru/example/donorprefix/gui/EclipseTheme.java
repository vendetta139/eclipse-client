package ru.example.donorprefix.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * ✦ ECLIPSE THEME — Единая дизайн-система визуального клиента ECLIPSE VISUAL MOD.
 * Стиль: Dark Minimalism / Space Eclipse (глубокий обсидиан, матовое стекло, двойной градиент золота и индиго).
 * Полностью кросс-версионная и краш-безопасная реализация графических примитивов.
 */
public final class EclipseTheme {

    // === БАЗОВАЯ ЦВЕТОВАЯ ПАЛИТРА ЗАТМЕНИЯ ===
    /** Основной глубокий фон космоса / загрузочного экрана (#0B0B10) */
    public static final int BG_DEEP = 0xFF0B0B10;

    /** Фон панелей меню (полупрозрачный матовый обсидиан #12131C с эффектом стекла) */
    public static final int PANEL_BG = 0xE612131C;
    public static final int PANEL_BG_OPAQUE = 0xFF12131C;
    public static final int PANEL_BG_CARD = 0xB8161824;

    /** Тонкая 1px обводка панелей и рамок (#232533) */
    public static final int PANEL_BORDER = 0xFF232533;
    public static final int PANEL_BORDER_HOVER = 0xFF35394E;

    /** Теплый золотистый акцент короны затмения (#F5A65B) */
    public static final int ACCENT_GOLD = 0xFFF5A65B;

    /** Холодный фиолетовый / индиго акцент тени затмения (#6A67CE) */
    public static final int ACCENT_INDIGO = 0xFF6A67CE;

    /** Дополнительный глубокий фиолетовый для неоновых теней */
    public static final int ACCENT_VIOLET_DEEP = 0xFF3D3580;

    /** Основной четкий контрастный текст (#EAECEF) */
    public static final int TEXT_PRIMARY = 0xFFEAECEF;

    /** Приглушенный текст для подсказок, меток и версий (#7D8296) */
    public static final int TEXT_MUTED = 0xFF7D8296;

    /** Фон подложки прогресс-баров и треков слайдеров (#1A1B26) */
    public static final int TRACK_BG = 0xFF1A1B26;

    /** Иконка мода для шапки и бейджей внутри меню */
    public static final Identifier ECLIPSE_ICON = Identifier.of("donor-prefix-client", "textures/gui/eclipse_icon.png");

    private EclipseTheme() {}

    // === УТИЛИТЫ РАБОТЫ С ЦВЕТОМ И ГРАДИЕНТАМИ ===

    /**
     * Плавная интерполяция между двумя ARGB цветами.
     */
    public static int lerpColor(int c1, int c2, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int a1 = (c1 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int a2 = (c2 >> 24) & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Возвращает цвет фирменного градиента Eclipse (от теплого золота #F5A65B к индиго #6A67CE).
     */
    public static int getEclipseGradient(float t) {
        return lerpColor(ACCENT_GOLD, ACCENT_INDIGO, t);
    }

    /**
     * Изменяет прозрачность цвета с сохранением RGB компонентов.
     */
    public static int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255.0f)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    /**
     * Умножает существующий альфа-канал цвета на коэффициент.
     */
    public static int multiplyAlpha(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int newAlpha = Math.max(0, Math.min(255, (int) (a * factor)));
        return (newAlpha << 24) | (color & 0x00FFFFFF);
    }

    /**
     * Кросс-версионная безопасная отрисовка горизонтального градиента через батчинг DrawContext.
     */
    public static void drawHorizontalGradient(DrawContext context, int x, int y, int width, int height, int startColor, int endColor) {
        if (width <= 0 || height <= 0 || context == null) return;
        for (int i = 0; i < width; i++) {
            float t = (float) i / (float) Math.max(1, width - 1);
            int col = lerpColor(startColor, endColor, t);
            context.fill(x + i, y, x + i + 1, y + height, col);
        }
    }

    /**
     * Кросс-версионная безопасная отрисовка вертикального градиента.
     */
    public static void drawVerticalGradient(DrawContext context, int x, int y, int width, int height, int topColor, int bottomColor) {
        if (width <= 0 || height <= 0 || context == null) return;
        try {
            context.fillGradient(x, y, x + width, y + height, topColor, bottomColor);
        } catch (Throwable ignored) {
            for (int i = 0; i < height; i++) {
                float t = (float) i / (float) Math.max(1, height - 1);
                int col = lerpColor(topColor, bottomColor, t);
                context.fill(x, y + i, x + width, y + i + 1, col);
            }
        }
    }

    /**
     * Отрисовка аккуратного значка (иконки кольца затмения).
     * Безопасна для любой версии: сначала пробует динамический drawTexture,
    /**
     * Отрисовка аккуратного значка (иконки кольца затмения).
     */
    public static void drawEclipseBadge(DrawContext context, int x, int y, int size, float alpha) {
        drawEclipseBadge(context, x, y, size, alpha, 0f);
    }

    /**
     * Безопасная отрисовка анимированного векторного значка затмения:
     * - Чистая математическая геометрия (без уродливых непрозрачных растровых плашек)
     * - Плавное дыхание короны и вращающийся блик
     * - Корректное масштабирование для любого размера (от 16px до 64px)
     */
    public static void drawEclipseBadge(DrawContext context, int x, int y, int size, float alpha, float animationTime) {
        if (context == null || size <= 0 || alpha <= 0.001f) return;

        float centerX = x + size / 2.0f;
        float centerY = y + size / 2.0f;
        float radius = Math.max(4.0f, (size / 2.0f) - 2.5f);

        float timeSec = (System.currentTimeMillis() % 10000000L) / 1000.0f;
        drawProceduralEclipseRing(context, centerX, centerY, radius, alpha, timeSec);
    }

    /**
     * Процедурная отрисовка кольца затмения Space Eclipse для экрана загрузки и шапки меню.
     * Построена на чистой математической геометрии через DrawContext.fill,
     * с плавным градиентным свечением (золото <-> индиго), затемненным лунным телом,
     * вращающимся бликом короны и тонкими световыми струями.
     */
    public static void drawProceduralEclipseRing(DrawContext context, float cx, float cy, float radius, float alpha, float timeSec) {
        if (context == null || alpha <= 0.001f) return;

        try {
            int rInt = Math.round(radius);
            int glowSpread = Math.max(2, Math.round(radius * 0.45f));
            int totalR = rInt + glowSpread;

            // 1. Внешняя мягкая корона (Corona Glow) с затуханием
            for (int dy = -totalR; dy <= totalR; dy++) {
                int y = Math.round(cy + dy);
                float distCenter = Math.abs(dy);
                if (distCenter > totalR) continue;

                double outerX = Math.sqrt(Math.max(0, totalR * totalR - dy * dy));
                double innerX = (distCenter <= rInt) ? Math.sqrt(Math.max(0, rInt * rInt - dy * dy)) : 0.0;

                // Градиент по вертикали: верх - теплое золото (#F5A65B), низ - индиго (#6A67CE)
                float tNorm = ((float) dy / (float) totalR + 1.0f) * 0.5f;
                int baseCol = lerpColor(ACCENT_GOLD, ACCENT_INDIGO, tNorm);

                // Затухание альфы к внешнему краю
                float glowAlpha = (1.0f - (float) distCenter / (float) totalR) * 0.26f * alpha;
                int glowCol = withAlpha(baseCol, glowAlpha);

                int x1Left = Math.round(cx - (float) outerX);
                int x2Left = Math.round(cx - (float) innerX);
                if (x2Left > x1Left) {
                    context.fill(x1Left, y, x2Left, y + 1, glowCol);
                }

                int x1Right = Math.round(cx + (float) innerX);
                int x2Right = Math.round(cx + (float) outerX);
                if (x2Right > x1Right) {
                    context.fill(x1Right, y, x2Right, y + 1, glowCol);
                }
            }

            // 2. Две тонкие диагональные световые струи (как в стиле Space Eclipse)
            float rayLength = radius * 1.6f;
            drawDiagonalRay(context, cx, cy, -0.7f, -0.5f, rayLength, ACCENT_GOLD, 0.35f * alpha);
            drawDiagonalRay(context, cx, cy, 0.7f, 0.5f, rayLength, ACCENT_INDIGO, 0.30f * alpha);

            // 3. Четкое кольцо обода (Sharp Rim) толщиной ~3px
            float rimInner = radius - 1.5f;
            float rimOuter = radius + 1.5f;
            int rimOuterInt = (int) Math.ceil(rimOuter);

            for (int dy = -rimOuterInt; dy <= rimOuterInt; dy++) {
                int y = Math.round(cy + dy);
                double dySq = dy * dy;

                if (dySq > rimOuter * rimOuter) continue;
                double outX = Math.sqrt(rimOuter * rimOuter - dySq);
                double inX = (dySq < rimInner * rimInner) ? Math.sqrt(rimInner * rimInner - dySq) : 0.0;

                float tRim = ((float) dy / rimOuter + 1.0f) * 0.5f;
                int rimCol = withAlpha(lerpColor(ACCENT_GOLD, ACCENT_INDIGO, tRim), 0.95f * alpha);

                int xl1 = Math.round(cx - (float) outX);
                int xl2 = Math.round(cx - (float) inX);
                if (xl2 > xl1) context.fill(xl1, y, xl2, y + 1, rimCol);

                int xr1 = Math.round(cx + (float) inX);
                int xr2 = Math.round(cx + (float) outX);
                if (xr2 > xr1) context.fill(xr1, y, xr2, y + 1, rimCol);
            }

            // 4. Внутренний темный диск Луны (#0B0B10)
            float moonR = radius - 1.4f;
            int moonRInt = (int) Math.floor(moonR);
            int moonColor = withAlpha(BG_DEEP, alpha);

            for (int dy = -moonRInt; dy <= moonRInt; dy++) {
                int y = Math.round(cy + dy);
                double dx = Math.sqrt(Math.max(0, moonR * moonR - dy * dy));
                int x1 = Math.round(cx - (float) dx);
                int x2 = Math.round(cx + (float) dx);
                if (x2 > x1) {
                    context.fill(x1, y, x2, y + 1, moonColor);
                }
            }

            // 5. Вращающийся блик короны затмения (Specular Flare)
            float flareAngle = (timeSec * 1.1f) % (float) (Math.PI * 2.0);
            float flareX = cx + (float) Math.cos(flareAngle) * radius;
            float flareY = cy + (float) Math.sin(flareAngle) * radius;
            int flareCol = withAlpha(0xFFFFFFFF, 0.70f * alpha);
            int flareDimCol = withAlpha(ACCENT_GOLD, 0.35f * alpha);

            int fx = Math.round(flareX);
            int fy = Math.round(flareY);
            context.fill(fx - 1, fy - 1, fx + 2, fy + 2, flareCol);
            context.fill(fx - 3, fy, fx + 4, fy + 1, flareDimCol);
            context.fill(fx, fy - 3, fx + 1, fy + 4, flareDimCol);

        } catch (Throwable ignored) {}
    }

    private static void drawDiagonalRay(DrawContext context, float cx, float cy, float dirX, float dirY, float length, int color, float alpha) {
        if (alpha <= 0.001f) return;
        int steps = Math.round(length);
        for (int i = 0; i < steps; i++) {
            float t = (float) i / (float) steps;
            float rayAlpha = (1.0f - t) * alpha;
            int col = withAlpha(color, rayAlpha);
            int rx = Math.round(cx + dirX * (radiusOffset(t) + i));
            int ry = Math.round(cy + dirY * (radiusOffset(t) + i));
            context.fill(rx, ry, rx + 1, ry + 1, col);
        }
    }

    private static float radiusOffset(float t) {
        return 14.0f;
    }

    /**
     * Отрисовка минималистичной типографики ECLIPSE VISUAL MOD с эффектом дыхания.
     */
    public static void drawProceduralSplashTypography(DrawContext context, TextRenderer textRenderer, int screenWidth, int screenHeight, float alpha, float timeSec) {
        if (context == null || textRenderer == null || alpha <= 0.001f) return;

        try {
            // Плавное дыхание прозрачности (Alpha Pulse)
            float pulse = 0.86f + 0.14f * (float) Math.sin(timeSec * 2.2f);
            float titleAlpha = alpha * pulse;

            String title = "E  C  L  I  P  S  E";
            int titleY = screenHeight / 2 + 32;
            int titleColor = withAlpha(TEXT_PRIMARY, titleAlpha);
            RenderUtils.drawCenteredTextWithShadow(context, textRenderer, Text.literal(title), screenWidth / 2, titleY, titleColor);

            String subtitle = "V I S U A L   M O D";
            int subY = titleY + 14;
            int subColor = withAlpha(TEXT_MUTED, alpha * 0.85f);
            RenderUtils.drawCenteredTextWithShadow(context, textRenderer, Text.literal(subtitle), screenWidth / 2, subY, subColor);
        } catch (Throwable ignored) {}
    }

    /**
     * Отрисовка ультратонкого минималистичного прогресс-бара загрузки:
     * Ширина 180px, высота 2px, подложка #1A1B26, градиент от #F5A65B к #6A67CE.
     */
    public static void drawSplashProgressBar(DrawContext context, TextRenderer textRenderer, int screenWidth, int screenHeight, float smoothProgress, float alpha) {
        if (context == null || alpha <= 0.001f) return;

        try {
            int barWidth = 180;
            int barHeight = 2;
            int barX = (screenWidth - barWidth) / 2;
            int barY = screenHeight - 48;

            // 1. Подложка трека прогресс-бара (#1A1B26)
            int trackColor = withAlpha(TRACK_BG, alpha);
            context.fill(barX, barY, barX + barWidth, barY + barHeight, trackColor);

            // 2. Заполнение с градиентом от золотистого к фиолетовому
            int fillWidth = Math.round(barWidth * Math.max(0.0f, Math.min(1.0f, smoothProgress)));
            if (fillWidth > 0) {
                int startCol = withAlpha(ACCENT_GOLD, alpha);
                int endCol = withAlpha(getEclipseGradient(smoothProgress), alpha);
                drawHorizontalGradient(context, barX, barY, fillWidth, barHeight, startCol, endCol);

                // Мягкий светящийся бегунок на острие прогресса
                if (fillWidth < barWidth && fillWidth > 2) {
                    int tipColor = withAlpha(0xFFFFFFFF, alpha * 0.75f);
                    context.fill(barX + fillWidth - 2, barY - 1, barX + fillWidth, barY + barHeight + 1, tipColor);
                }
            }

            // 3. Аккуратная минималистичная подпись процентов (0% ... 100%)
            if (textRenderer != null) {
                int percent = Math.max(0, Math.min(100, Math.round(smoothProgress * 100.0f)));
                String percentStr = percent + "%";
                int textY = barY - 12;
                int pctColor = withAlpha(TEXT_MUTED, alpha * 0.75f);
                RenderUtils.drawCenteredTextWithShadow(context, textRenderer, Text.literal(percentStr), screenWidth / 2, textY, pctColor);
            }
        } catch (Throwable ignored) {}
    }
}
