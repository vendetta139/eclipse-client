package ru.example.donorprefix.visual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import ru.example.donorprefix.gui.RenderUtils;

/**
 * Кастомный PvP-прицел (Custom Crosshair):
 * - Режимы: Точка (Dot), Крестик (Cross), Круг (Circle), Плюс с точкой
 * - Настройка размера, зазора (gap), толщины линий
 * - Темная окантовка для идеальной видимости на любом фоне (небо, снег, тьма)
 * - Поддержка любого цвета и переливающейся радуги
 */
public class CustomCrosshairRenderer {

    public static void render(DrawContext context, MinecraftClient client) {
        VisualConfig cfg = VisualConfig.getInstance();
        if (!cfg.customCrosshair || client.options.hudHidden) return;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        int cx = screenW / 2;
        int cy = screenH / 2;

        int color = cfg.getEffectiveCrosshairColor();
        int outlineColor = 0xBB000000;

        int size = Math.max(1, cfg.crosshairSize);
        int gap = Math.max(0, cfg.crosshairGap);
        int thick = Math.max(1, cfg.crosshairThickness);
        int halfThick = thick / 2;

        switch (cfg.crosshairType) {
            case 0 -> {
                // Точка (Dot)
                int dotSize = Math.max(2, size);
                int dx = cx - dotSize / 2;
                int dy = cy - dotSize / 2;
                context.fill(dx - 1, dy - 1, dx + dotSize + 1, dy + dotSize + 1, outlineColor);
                context.fill(dx, dy, dx + dotSize, dy + dotSize, color);
            }
            case 1 -> {
                // Крестик (Cross)
                drawOutlinedRect(context, cx - halfThick, cy - gap - size, thick, size, color, outlineColor);
                drawOutlinedRect(context, cx - halfThick, cy + gap + 1, thick, size, color, outlineColor);
                drawOutlinedRect(context, cx - gap - size, cy - halfThick, size, thick, color, outlineColor);
                drawOutlinedRect(context, cx + gap + 1, cy - halfThick, size, thick, color, outlineColor);

                if (cfg.crosshairCenterDot) {
                    drawOutlinedRect(context, cx - 1, cy - 1, 2, 2, color, outlineColor);
                }
            }
            case 2 -> {
                // Круг (Circle)
                int radius = Math.max(3, gap + size);
                drawCircle(context, cx, cy, radius, color, outlineColor);
                if (cfg.crosshairCenterDot) {
                    drawOutlinedRect(context, cx - 1, cy - 1, 2, 2, color, outlineColor);
                }
            }
            case 3 -> {
                // T-Shape (без верхнего луча)
                drawOutlinedRect(context, cx - halfThick, cy + gap + 1, thick, size, color, outlineColor);
                drawOutlinedRect(context, cx - gap - size, cy - halfThick, size, thick, color, outlineColor);
                drawOutlinedRect(context, cx + gap + 1, cy - halfThick, size, thick, color, outlineColor);

                if (cfg.crosshairCenterDot) {
                    drawOutlinedRect(context, cx - 1, cy - 1, 2, 2, color, outlineColor);
                }
            }
        }
    }

    private static void drawOutlinedRect(DrawContext context, int x, int y, int w, int h, int color, int outlineColor) {
        // Контур
        context.fill(x - 1, y - 1, x + w + 1, y + h + 1, outlineColor);
        // Заливка
        context.fill(x, y, x + w, y + h, color);
    }

    private static void drawCircle(DrawContext context, int cx, int cy, int radius, int color, int outlineColor) {
        int segments = 24;
        for (int i = 0; i < segments; i++) {
            double a1 = i * 2 * Math.PI / segments;
            int px = (int) Math.round(cx + Math.cos(a1) * radius);
            int py = (int) Math.round(cy + Math.sin(a1) * radius);
            context.fill(px - 1, py - 1, px + 2, py + 2, outlineColor);
            context.fill(px, py, px + 1, py + 1, color);
        }
    }
}
