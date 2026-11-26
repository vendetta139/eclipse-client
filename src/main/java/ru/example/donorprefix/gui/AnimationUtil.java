package ru.example.donorprefix.gui;

import net.minecraft.util.math.MathHelper;

/**
 * ✦ AnimationUtil — Универсальное ядро плавных математических анимаций клиента ECLIPSE.
 * Работает на каждом кадре рендера независимо от частоты кадров (FPS) с поддержкой deltaTime
 * и физически приятных кривых Безье (Cubic, Quart, Sine, Back).
 */
public final class AnimationUtil {

    private AnimationUtil() {}

    // === КРИВЫЕ СГЛАЖИВАНИЯ (EASING FUNCTIONS) ===

    /**
     * Плавное замедление к концу движения (Ease Out Cubic).
     */
    public static float easeOutCubic(float x) {
        x = MathHelper.clamp(x, 0.0f, 1.0f);
        return 1.0f - (float) Math.pow(1.0f - x, 3.0);
    }

    /**
     * Плавное ускорение от начала движения (Ease In Cubic).
     */
    public static float easeInCubic(float x) {
        x = MathHelper.clamp(x, 0.0f, 1.0f);
        return x * x * x;
    }

    /**
     * Усиленное плавное замедление (Ease Out Quart) — идеально для переключателей и шкал.
     */
    public static float easeOutQuart(float x) {
        x = MathHelper.clamp(x, 0.0f, 1.0f);
        return 1.0f - (float) Math.pow(1.0f - x, 4.0);
    }

    /**
     * Мягкое гармоническое синусоидальное движение (Ease In Out Sine).
     */
    public static float easeInOutSine(float x) {
        x = MathHelper.clamp(x, 0.0f, 1.0f);
        return -(MathHelper.cos((float) Math.PI * x) - 1.0f) / 2.0f;
    }

    /**
     * Движение с легким пружинящим вылетом за пределы и возвратом (Ease Out Back) — для всплывающих окон и Toast.
     */
    public static float easeOutBack(float x) {
        x = MathHelper.clamp(x, 0.0f, 1.0f);
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float) Math.pow(x - 1.0f, 3.0) + c1 * (float) Math.pow(x - 1.0f, 2.0);
    }

    // === ДЕЛЬТА-ИНТЕРПОЛЯЦИЯ (FPS-INDEPENDENT LERP) ===

    /**
     * Плавная интерполяция значений с учетом deltaTime (в секундах).
     * Движение остается абсолютно идентичным при 30, 60, 144 или 360+ FPS.
     *
     * @param current  текущее значение
     * @param target   целевое значение
     * @param speed    скорость сглаживания (обычно 8.0f - 16.0f)
     * @param deltaSec прошедшее время кадра в секундах
     */
    public static float deltaLerp(float current, float target, float speed, float deltaSec) {
        if (Float.isNaN(current)) current = target;
        deltaSec = MathHelper.clamp(deltaSec, 0.0f, 0.1f); // Защита от лагов/фризов
        float factor = 1.0f - (float) Math.exp(-speed * deltaSec);
        return current + (target - current) * factor;
    }

    /**
     * Обычный линейный лерп с зажатием коэффициента.
     */
    public static float lerp(float current, float target, float step) {
        step = MathHelper.clamp(step, 0.0f, 1.0f);
        return current + (target - current) * step;
    }

    /**
     * Плавная интерполяция между двумя ARGB цветами с учетом фактора t (0..1).
     */
    public static int lerpColor(int c1, int c2, float t) {
        t = MathHelper.clamp(t, 0.0f, 1.0f);
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

    // === КЛАСС-ОБЕРТКА ДЛЯ АНИМИРУЕМЫХ ЗНАЧЕНИЙ (AnimatedValue) ===

    /**
     * Удобный контейнер для параметров с плавной анимацией к целевому значению.
     */
    public static class AnimatedValue {
        private float value;
        private float targetValue;
        private float speed;

        public AnimatedValue(float initialValue, float speed) {
            this.value = initialValue;
            this.targetValue = initialValue;
            this.speed = speed;
        }

        public AnimatedValue(float initialValue) {
            this(initialValue, 12.0f);
        }

        public void set(float target) {
            this.targetValue = target;
        }

        public void setImmediate(float val) {
            this.value = val;
            this.targetValue = val;
        }

        public float update(float deltaSec) {
            this.value = deltaLerp(this.value, this.targetValue, this.speed, deltaSec);
            if (Math.abs(this.value - this.targetValue) < 0.0008f) {
                this.value = this.targetValue;
            }
            return this.value;
        }

        public float get() {
            return this.value;
        }

        public float getTarget() {
            return this.targetValue;
        }

        public boolean isDone() {
            return Math.abs(this.value - this.targetValue) < 0.001f;
        }

        public void setSpeed(float speed) {
            this.speed = speed;
        }
    }
}
