package ru.example.donorprefix.gui;

import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ✦ CosmicBackgroundRenderer — Отрисовка глубокого космического фона (#0B0B10)
 * с радиальным свечением короны и плавно парящими частицами звёздной пыли (Space Eclipse).
 */
public final class CosmicBackgroundRenderer {

    private static final int STAR_COUNT = 55;
    private static final List<StarParticle> STARS = new ArrayList<>();
    private static final Random RANDOM = new Random(1337);
    private static long lastUpdateTime = 0;

    private static class StarParticle {
        float x;
        float y;
        float speedY;
        float speedX;
        float size;
        float baseAlpha;
        float pulseSpeed;
        float seed;
        int color;
    }

    private static void initStars(int width, int height) {
        if (!STARS.isEmpty() && width > 0 && height > 0) return;
        STARS.clear();
        for (int i = 0; i < STAR_COUNT; i++) {
            StarParticle p = new StarParticle();
            p.x = RANDOM.nextFloat() * Math.max(100, width);
            p.y = RANDOM.nextFloat() * Math.max(100, height);
            p.speedY = 4.0f + RANDOM.nextFloat() * 8.0f; // Медленный подъем вверх
            p.speedX = (RANDOM.nextFloat() - 0.5f) * 3.0f; // Легкий дрейф в стороны
            p.size = RANDOM.nextFloat() < 0.25f ? 2.0f : 1.0f;
            p.baseAlpha = 0.20f + RANDOM.nextFloat() * 0.55f;
            p.pulseSpeed = 1.2f + RANDOM.nextFloat() * 2.0f;
            p.seed = RANDOM.nextFloat() * 10.0f;

            // Звездная пыль: золотистая, индиго и белая
            float type = RANDOM.nextFloat();
            if (type < 0.35f) {
                p.color = EclipseTheme.ACCENT_GOLD;
            } else if (type < 0.70f) {
                p.color = EclipseTheme.ACCENT_INDIGO;
            } else {
                p.color = 0xFFFFFFFF;
            }
            STARS.add(p);
        }
    }

    public static void render(DrawContext context, int width, int height, float alpha) {
        if (context == null || width <= 0 || height <= 0 || alpha <= 0.001f) return;

        initStars(width, height);

        long now = System.currentTimeMillis();
        float deltaSec = lastUpdateTime > 0 ? (now - lastUpdateTime) / 1000.0f : 0.016f;
        deltaSec = Math.max(0.0f, Math.min(0.1f, deltaSec));
        lastUpdateTime = now;

        // 1. Основной глубокий сплошной темный фон космоса (#0B0B10) на весь экран
        context.fill(0, 0, width, height, 0xFF0B0B10);

        // 2. Плавный вертикальный градиент (#0B0B10 -> #141520) без концентрических циклов
        context.fillGradient(0, 0, width, height, 0xFF0B0B10, 0xFF141520);

        // 3. Плавно парящие частицы звездной пыли с синусоидальным мерцанием
        float timeSec = (now % 10000000L) / 1000.0f;

        for (StarParticle p : STARS) {
            // Обновление позиций
            p.y -= p.speedY * deltaSec;
            p.x += p.speedX * deltaSec;

            // Зацикливание по границам экрана
            if (p.y < -5) {
                p.y = height + 5;
                p.x = RANDOM.nextFloat() * width;
            } else if (p.y > height + 5) {
                p.y = -5;
            }
            if (p.x < -5) {
                p.x = width + 5;
            } else if (p.x > width + 5) {
                p.x = -5;
            }

            // Мерцание по синусоиде (через стандартный Math.sin, 100% совместимый с любой версией)
            float sinePulse = 0.5f + 0.5f * (float) Math.sin((double) (timeSec * p.pulseSpeed + p.seed));
            float pAlpha = p.baseAlpha * sinePulse * alpha;
            if (pAlpha <= 0.01f) continue;

            int col = EclipseTheme.withAlpha(p.color, pAlpha);
            int px = Math.round(p.x);
            int py = Math.round(p.y);
            int sz = Math.round(p.size);

            context.fill(px, py, px + sz, py + sz, col);

            // Мягкий ореол вокруг более крупных частиц
            if (sz > 1 && pAlpha > 0.3f) {
                int haloCol = EclipseTheme.withAlpha(p.color, pAlpha * 0.25f);
                context.fill(px - 1, py, px + sz + 1, py + sz, haloCol);
                context.fill(px, py - 1, px + sz, py + sz + 1, haloCol);
            }
        }
    }
}
