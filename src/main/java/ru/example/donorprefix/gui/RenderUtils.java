package ru.example.donorprefix.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;

/**
 * Высокопроизводительный графический модуль для отрисовки сглаженных элементов интерфейса
 * в стиле modern dark glassmorphism: скругленные прямоугольники с антиалиасингом,
 * неоновые рамки, градиенты и мягкое внешнее свечение (glow / drop shadow).
 * Использует нативный API Minecraft 1.21.11 (Matrix3x2fStack, DrawContext.fill, DrawContext.drawText).
 */
public class RenderUtils {

    private static final String[] ASCII_CHAR_CACHE = new String[128];
    static {
        for (int i = 0; i < ASCII_CHAR_CACHE.length; i++) {
            ASCII_CHAR_CACHE[i] = String.valueOf((char) i);
        }
    }

    private static String charToString(char c) {
        return c < 128 ? ASCII_CHAR_CACHE[c] : String.valueOf(c);
    }

    public static void push(DrawContext context) {
        if (context == null) return;
        Matrix3x2fStack matrices = context.getMatrices();
        if (matrices != null) {
            matrices.pushMatrix();
        }
    }

    public static void pop(DrawContext context) {
        if (context == null) return;
        Matrix3x2fStack matrices = context.getMatrices();
        if (matrices != null) {
            matrices.popMatrix();
        }
    }

    public static void translate(DrawContext context, float x, float y, float z) {
        if (context == null) return;
        Matrix3x2fStack matrices = context.getMatrices();
        if (matrices != null) {
            matrices.translate(x, y);
        }
    }

    public static void scale(DrawContext context, float x, float y, float z) {
        if (context == null) return;
        Matrix3x2fStack matrices = context.getMatrices();
        if (matrices != null) {
            matrices.scale(x, y);
        }
    }

    public static void drawTextWithShadow(DrawContext context, TextRenderer tr, Text text, int x, int y, int color) {
        if (context == null || tr == null || text == null) return;
        context.drawText(tr, text, x, y, color, true);
    }

    public static void drawTextWithShadow(DrawContext context, TextRenderer tr, String text, int x, int y, int color) {
        if (context == null || tr == null || text == null) return;
        context.drawText(tr, text, x, y, color, true);
    }

    public static void drawText(DrawContext context, TextRenderer tr, Text text, int x, int y, int color) {
        if (context == null || tr == null || text == null) return;
        context.drawText(tr, text, x, y, color, false);
    }

    public static void drawText(DrawContext context, TextRenderer tr, String text, int x, int y, int color) {
        if (context == null || tr == null || text == null) return;
        context.drawText(tr, text, x, y, color, false);
    }

    public static void drawCenteredTextWithShadow(DrawContext context, TextRenderer tr, Text text, int centerX, int y, int color) {
        if (context == null || tr == null || text == null) return;
        try {
            int w = tr.getWidth(text);
            drawTextWithShadow(context, tr, text, centerX - w / 2, y, color);
        } catch (Throwable ignored) {}
    }

    public static void drawCenteredTextWithShadow(DrawContext context, TextRenderer tr, String text, int centerX, int y, int color) {
        if (context == null || tr == null || text == null) return;
        try {
            int w = tr.getWidth(text);
            drawTextWithShadow(context, tr, text, centerX - w / 2, y, color);
        } catch (Throwable ignored) {}
    }

    /**
     * Отрисовка градиентного текста с мягким переливом (стиль Eclipse)
    /**
     * Безопасная процедурная отрисовка градиентного текста (Eclipse Wave) посимвольно.
     * Не создает сложных объектов стилей TextColor, работает стабильно и сохраняет кернинг шрифта.
     */
    public static void drawGradientText(DrawContext context, TextRenderer textRenderer, String fullText, int startX, int y, int colorStart, int colorEnd, boolean animated, float alpha) {
        if (context == null || fullText == null || fullText.isEmpty()) return;
        if (textRenderer == null) {
            try {
                drawTextWithShadow(context, textRenderer, fullText, startX, y, colorStart);
            } catch (Throwable ignored) {}
            return;
        }

        int textX = startX;
        long time = System.currentTimeMillis();
        for (int i = 0; i < fullText.length(); i++) {
            char c = fullText.charAt(i);
            String charStr = charToString(c);

            // Плавная интерполяция между золотым (#F5A65B) и фиолетовым (#6A67CE) на основе позиции и времени
            float factor = animated
                    ? (float) (Math.sin((time / 400.0) + (i * 0.15)) * 0.5 + 0.5)
                    : (fullText.length() > 1 ? (float) i / (float) (fullText.length() - 1) : 0.0f);

            int r1 = (colorStart >> 16) & 0xFF;
            int g1 = (colorStart >> 8) & 0xFF;
            int b1 = colorStart & 0xFF;

            int r2 = (colorEnd >> 16) & 0xFF;
            int g2 = (colorEnd >> 8) & 0xFF;
            int b2 = colorEnd & 0xFF;

            int r = (int) (r1 + factor * (r2 - r1));
            int g = (int) (g1 + factor * (g2 - g1));
            int b = (int) (b1 + factor * (b2 - b1));

            int color = (0xFF << 24) | (r << 16) | (g << 8) | b;
            if (alpha < 0.999f) {
                color = multiplyAlpha(color, alpha);
            }

            drawTextWithShadow(context, textRenderer, charStr, textX, y, color);
            textX += textRenderer.getWidth(charStr);
        }
    }

    public static void drawGradientText(DrawContext context, TextRenderer textRenderer, String fullText, int startX, int y, int colorStart, int colorEnd, boolean animated) {
        drawGradientText(context, textRenderer, fullText, startX, y, colorStart, colorEnd, animated, 1.0f);
    }

    /**
     * Кросс-версионная безопасная отрисовка рамки прямоугольника (замена удаленного context.drawBorder).
     */
    public static void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        if (context == null || width <= 0 || height <= 0) return;
        context.fill(x, y, x + width, y + 1, color); // Верх
        context.fill(x, y + height - 1, x + width, y + height, color); // Низ
        context.fill(x, y + 1, x + 1, y + height - 1, color); // Лево
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color); // Право
    }

    /**
     * Отрисовка скругленного прямоугольника с антиалиасингом (smooth rounded corners).
     */
    public static void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) return;
        radius = Math.max(0, Math.min(radius, Math.min(width / 2, height / 2)));
        if (radius <= 1) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        // Основное тело по центру
        context.fill(x, y + radius, x + width, y + height - radius, color);

        // Верхние и нижние горизонтальные полосы скругления
        for (int i = 0; i < radius; i++) {
            int dy = radius - i;
            int inset = (int) Math.round(radius - Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            context.fill(x + inset, y + i, x + width - inset, y + i + 1, color);
            context.fill(x + inset, y + height - 1 - i, x + width - inset, y + height - i, color);
        }
    }

    /**
     * Отрисовка скругленной рамки (border).
     */
    public static void drawRoundedOutline(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) return;
        radius = Math.max(0, Math.min(radius, Math.min(width / 2, height / 2)));
        if (radius <= 1) {
            drawBorder(context, x, y, width, height, color);
            return;
        }

        // Прямые сегменты рамок
        context.fill(x + radius, y, x + width - radius, y + 1, color); // Верх
        context.fill(x + radius, y + height - 1, x + width - radius, y + height, color); // Низ
        context.fill(x, y + radius, x + 1, y + height - radius, color); // Лево
        context.fill(x + width - 1, y + radius, x + width, y + height - radius, color); // Право

        // Угловые точки по строкам
        for (int i = 0; i < radius; i++) {
            int dy = radius - i;
            int inset = (int) Math.round(radius - Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            // Верх
            context.fill(x + inset, y + i, x + inset + 1, y + i + 1, color);
            context.fill(x + width - 1 - inset, y + i, x + width - inset, y + i + 1, color);
            // Низ
            context.fill(x + inset, y + height - 1 - i, x + inset + 1, y + height - i, color);
            context.fill(x + width - 1 - inset, y + height - 1 - i, x + width - inset, y + height - i, color);
        }
    }

    /**
     * Отрисовка скругленной рамки с горизонтальным градиентом от color1 к color2.
     */
    public static void drawGradientRoundedOutline(DrawContext context, int x, int y, int width, int height, int radius, int color1, int color2) {
        if (width <= 0 || height <= 0 || context == null) return;
        radius = Math.max(0, Math.min(radius, Math.min(width / 2, height / 2)));
        if (radius <= 1) {
            drawGradientLineH(context, x, y, width, 1, color1, color2);
            drawGradientLineH(context, x, y + height - 1, width, 1, color1, color2);
            context.fill(x, y + 1, x + 1, y + height - 1, color1);
            context.fill(x + width - 1, y + 1, x + width, y + height - 1, color2);
            return;
        }

        // Верхний и нижний край
        drawGradientLineH(context, x + radius, y, width - radius * 2, 1, color1, color2);
        drawGradientLineH(context, x + radius, y + height - 1, width - radius * 2, 1, color1, color2);

        // Левый и правый прямой край
        context.fill(x, y + radius, x + 1, y + height - radius, color1);
        context.fill(x + width - 1, y + radius, x + width, y + height - radius, color2);

        // Скругления углов
        for (int i = 0; i < radius; i++) {
            int dy = radius - i;
            int inset = (int) Math.round(radius - Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            // Левый верх и левый низ: color1
            context.fill(x + inset, y + i, x + inset + 1, y + i + 1, color1);
            context.fill(x + inset, y + height - 1 - i, x + inset + 1, y + height - i, color1);
            // Правый верх и правый низ: color2
            context.fill(x + width - 1 - inset, y + i, x + width - inset, y + i + 1, color2);
            context.fill(x + width - 1 - inset, y + height - 1 - i, x + width - inset, y + height - i, color2);
        }
    }

    /**
     * Отрисовка скругленного прямоугольника с вертикальным градиентом.
     */
    public static void drawGradientRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int topColor, int bottomColor) {
        if (width <= 0 || height <= 0) return;
        radius = Math.max(0, Math.min(radius, Math.min(width / 2, height / 2)));
        if (radius <= 1) {
            context.fillGradient(x, y, x + width, y + height, topColor, bottomColor);
            return;
        }

        // Верхняя часть со скруглением
        for (int i = 0; i < radius; i++) {
            float t = (float) i / (float) Math.max(1, height - 1);
            int col = lerpColor(topColor, bottomColor, t);
            int dy = radius - i;
            int inset = (int) Math.round(radius - Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            context.fill(x + inset, y + i, x + width - inset, y + i + 1, col);
        }

        // Основное тело (один быстрый градиентный прямоугольник)
        int midTopCol = lerpColor(topColor, bottomColor, (float) radius / (float) Math.max(1, height - 1));
        int midBotCol = lerpColor(topColor, bottomColor, (float) (height - radius) / (float) Math.max(1, height - 1));
        context.fillGradient(x, y + radius, x + width, y + height - radius, midTopCol, midBotCol);

        // Нижняя часть со скруглением
        for (int i = height - radius; i < height; i++) {
            float t = (float) i / (float) Math.max(1, height - 1);
            int col = lerpColor(topColor, bottomColor, t);
            int dy = i - (height - radius);
            int inset = (int) Math.round(radius - Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            context.fill(x + inset, y + i, x + width - inset, y + i + 1, col);
        }
    }

    /**
     * Отрисовка мягкого внешнего свечения (Drop Shadow / Outer Glow).
     */
    public static void drawSoftGlow(DrawContext context, int x, int y, int width, int height, int radius, int glowRadius, int glowColor) {
        if (glowRadius <= 0) return;
        int baseAlpha = (glowColor >> 24) & 0xFF;
        if (baseAlpha <= 0) return;

        int stepAlpha1 = (int) (baseAlpha * 0.35f);
        int stepAlpha2 = (int) (baseAlpha * 0.15f);

        if (stepAlpha1 > 0) {
            int col1 = (stepAlpha1 << 24) | (glowColor & 0x00FFFFFF);
            drawRoundedOutline(context, x - 1, y - 1, width + 2, height + 2, radius + 1, col1);
        }
        if (stepAlpha2 > 0) {
            int col2 = (stepAlpha2 << 24) | (glowColor & 0x00FFFFFF);
            drawRoundedOutline(context, x - 2, y - 2, width + 4, height + 4, radius + 2, col2);
        }
    }

    /**
     * Отрисовка закругленной капсулы (Pill / Badge).
     */
    public static void drawPill(DrawContext context, int x, int y, int width, int height, int bgColor, int borderColor) {
        int radius = height / 2;
        drawRoundedRect(context, x, y, width, height, radius, bgColor);
        if ((borderColor & 0xFF000000) != 0) {
            drawRoundedOutline(context, x, y, width, height, radius, borderColor);
        }
    }

    public static int multiplyAlpha(int color, float factor) {
        int a = Math.round(((color >> 24) & 0xFF) * factor);
        a = MathHelper.clamp(a, 0, 255);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    public static int lerpColor(int c1, int c2, float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static float easeOutCubic(float x) {
        return 1.0f - (float) Math.pow(1.0f - MathHelper.clamp(x, 0f, 1f), 3);
    }

    public static float easeInOutQuad(float x) {
        x = MathHelper.clamp(x, 0f, 1f);
        return x < 0.5f ? 2f * x * x : 1f - (float) Math.pow(-2f * x + 2f, 2) / 2f;
    }

    public static int hsvToRgb(float h, float s, float v) {
        h = ((h % 360f) + 360f) % 360f;
        s = MathHelper.clamp(s, 0f, 1f);
        v = MathHelper.clamp(v, 0f, 1f);

        float c = v * s;
        float x = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m = v - c;

        float r = 0, g = 0, b = 0;
        int sextant = (int) (h / 60f);
        switch (sextant) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }

        int ir = MathHelper.clamp((int) ((r + m) * 255f), 0, 255);
        int ig = MathHelper.clamp((int) ((g + m) * 255f), 0, 255);
        int ib = MathHelper.clamp((int) ((b + m) * 255f), 0, 255);

        return 0xFF000000 | (ir << 16) | (ig << 8) | ib;
    }

    public static void rgbToHsv(int rgb, float[] hsv) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        float h = 0f;
        if (delta != 0f) {
            if (max == rf) {
                h = ((gf - bf) / delta) % 6f;
            } else if (max == gf) {
                h = ((bf - rf) / delta) + 2f;
            } else {
                h = ((rf - gf) / delta) + 4f;
            }
            h *= 60f;
            if (h < 0f) h += 360f;
        }

        float s = (max == 0f) ? 0f : (delta / max);
        float v = max;

        hsv[0] = h;
        hsv[1] = s;
        hsv[2] = v;
    }

    public static void drawHueBar(DrawContext context, int x, int y, int width, int height) {
        for (int i = 0; i < width; i++) {
            float hue = (float) i / (float) Math.max(1, width - 1) * 360f;
            int col = hsvToRgb(hue, 1f, 1f);
            context.fill(x + i, y, x + i + 1, y + height, col);
        }
        drawRoundedOutline(context, x, y, width, height, 2, 0x44FFFFFF);
    }

    public static void drawSaturationValueBox(DrawContext context, int x, int y, int width, int height, float hue) {
        for (int i = 0; i < width; i++) {
            float s = (float) i / (float) Math.max(1, width - 1);
            int topColor = hsvToRgb(hue, s, 1.0f);
            context.fillGradient(x + i, y, x + i + 1, y + height, topColor, 0xFF000000);
        }
        drawRoundedOutline(context, x, y, width, height, 2, 0x44FFFFFF);
    }

    public static void drawGradientLineH(DrawContext context, int x, int y, int width, int height, int startColor, int endColor) {
        if (width <= 0 || height <= 0) return;
        for (int i = 0; i < width; i++) {
            float t = (float) i / (float) Math.max(1, width - 1);
            int col = lerpColor(startColor, endColor, t);
            context.fill(x + i, y, x + i + 1, y + height, col);
        }
    }

    public static void drawCyberCard(DrawContext context, int x, int y, int width, int height, int radius, int bgColor, int borderColor, int glowColor) {
        if (width <= 0 || height <= 0) return;
        if ((glowColor & 0xFF000000) != 0) {
            drawSoftGlow(context, x, y, width, height, radius, 3, glowColor);
        }
        drawRoundedRect(context, x, y, width, height, radius, bgColor);
        if ((borderColor & 0xFF000000) != 0) {
            drawRoundedOutline(context, x, y, width, height, radius, borderColor);
        }
    }

    public static void drawSlider(DrawContext context, int x, int y, int width, int height, float value, int trackColor, int activeStart, int activeEnd, int knobBorder) {
        value = MathHelper.clamp(value, 0f, 1f);
        int trackHeight = Math.max(4, height / 3);
        int trackY = y + (height - trackHeight) / 2;

        // Фон дорожки (#1A1B26)
        drawRoundedRect(context, x, trackY, width, trackHeight, trackHeight / 2, EclipseTheme.TRACK_BG);
        drawRoundedOutline(context, x, trackY, width, trackHeight, trackHeight / 2, EclipseTheme.PANEL_BORDER);

        // Заполненная часть с плавным градиентом от золотого к индиго
        int fillWidth = (int) (width * value);
        if (fillWidth > 2) {
            drawGradientLineH(context, x, trackY, fillWidth, trackHeight, EclipseTheme.ACCENT_GOLD, EclipseTheme.ACCENT_INDIGO);
            drawSoftGlow(context, x, trackY, fillWidth, trackHeight, trackHeight / 2, 2, multiplyAlpha(EclipseTheme.ACCENT_INDIGO, 0.45f));
        }

        // Ползунок (ручка) со свечением
        int knobSize = height;
        int knobX = (int) (x + (width - knobSize) * value);
        drawRoundedRect(context, knobX, y, knobSize, knobSize, knobSize / 2, 0xFFFFFFFF);
        drawRoundedOutline(context, knobX, y, knobSize, knobSize, knobSize / 2, knobBorder);
        drawSoftGlow(context, knobX, y, knobSize, knobSize, knobSize / 2, 2, 0x55F5A65B);
    }

    public static void drawToggleSwitch(DrawContext context, int x, int y, int width, int height, boolean active, float animProgress) {
        animProgress = MathHelper.clamp(animProgress, 0f, 1f);
        int radius = height / 2;

        int offBg = EclipseTheme.PANEL_BORDER; // #232533
        int onBg = EclipseTheme.ACCENT_GOLD;   // #F5A65B
        int currentBg = AnimationUtil.lerpColor(offBg, onBg, animProgress);
        int currentBorder = AnimationUtil.lerpColor(0xFF161824, EclipseTheme.ACCENT_GOLD, animProgress);

        drawPill(context, x, y, width, height, currentBg, currentBorder);

        if (animProgress > 0.05f) {
            drawSoftGlow(context, x, y, width, height, radius, 3, multiplyAlpha(EclipseTheme.ACCENT_GOLD, animProgress * 0.5f));
        }

        // Ползунок скользит через easeOutQuart для маслянистой плавности
        float easedProgress = AnimationUtil.easeOutQuart(animProgress);
        int knobDiameter = height - 4;
        int minKnobX = x + 2;
        int maxKnobX = x + width - knobDiameter - 2;
        int knobX = Math.round(minKnobX + (maxKnobX - minKnobX) * easedProgress);
        int knobY = y + 2;

        int knobColor = AnimationUtil.lerpColor(0xFF7D8296, 0xFFFFFFFF, animProgress);
        drawRoundedRect(context, knobX, knobY, knobDiameter, knobDiameter, knobDiameter / 2, knobColor);
        drawRoundedOutline(context, knobX, knobY, knobDiameter, knobDiameter, knobDiameter / 2, 0x44000000);
    }
}

