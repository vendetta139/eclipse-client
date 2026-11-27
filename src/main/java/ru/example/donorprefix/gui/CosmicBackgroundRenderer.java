package ru.example.donorprefix.gui;

import net.minecraft.client.gui.DrawContext;

import java.util.Random;

/**
 * Отрисовка глубокого монохромного графитового фона (#09090B -> #121215)
 * с тонкими ахроматическими частицами пыли без цветных акцентов.
 */
public final class CosmicBackgroundRenderer {

    private static final int STAR_COUNT = 55;
    private static final StarParticle[] STARS = new StarParticle[STAR_COUNT];
    private static boolean initialized = false;
    private static final Random RANDOM = new Random(1337);
    private static long lastUpdateTime = 0;

    private static final class StarParticle {
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
        if (initialized && width > 0 && height > 0) return;
        initialized = true;
        for (int i = 0; i < STAR_COUNT; i++) {
            StarParticle p = new StarParticle();
            p.x = RANDOM.nextFloat() * Math.max(100, width);
            p.y = RANDOM.nextFloat() * Math.max(100, height);
            p.speedY = 4.0f + RANDOM.nextFloat() * 8.0f;
            p.speedX = (RANDOM.nextFloat() - 0.5f) * 3.0f;
            p.size = RANDOM.nextFloat() < 0.2f ? 2.0f : 1.0f;
            p.baseAlpha = 0.14f + RANDOM.nextFloat() * 0.35f;
            p.pulseSpeed = 1.2f + RANDOM.nextFloat() * 2.0f;
            p.seed = RANDOM.nextFloat() * 10.0f;

            float type = RANDOM.nextFloat();
            if (type < 0.35f) {
                p.color = EclipseColors.TEXT_PRIMARY;
            } else if (type < 0.70f) {
                p.color = EclipseColors.BORDER_ACTIVE;
            } else {
                p.color = EclipseColors.TEXT_SECONDARY;
            }
            STARS[i] = p;
        }
    }

    public static void render(DrawContext context, int width, int height, float alpha) {
        if (context == null || width <= 0 || height <= 0 || alpha <= 0.001f) return;

        initStars(width, height);

        long now = System.currentTimeMillis();
        float deltaSec = lastUpdateTime > 0 ? (now - lastUpdateTime) / 1000.0f : 0.016f;
        deltaSec = Math.max(0.0f, Math.min(0.1f, deltaSec));
        lastUpdateTime = now;

        context.fillGradient(0, 0, width, height, EclipseColors.BG_WINDOW, EclipseColors.BG_CARD);

        float timeSec = (now % 10000000L) / 1000.0f;

        for (StarParticle p : STARS) {
            p.y -= p.speedY * deltaSec;
            p.x += p.speedX * deltaSec;

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

            float sinePulse = 0.5f + 0.5f * (float) Math.sin((double) (timeSec * p.pulseSpeed + p.seed));
            float pAlpha = p.baseAlpha * sinePulse * alpha;
            if (pAlpha <= 0.01f) continue;

            int col = EclipseTheme.withAlpha(p.color, pAlpha);
            int px = Math.round(p.x);
            int py = Math.round(p.y);
            int sz = Math.round(p.size);

            context.fill(px, py, px + sz, py + sz, col);
        }
    }
}
