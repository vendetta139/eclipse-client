package ru.eclipse.client.module.setting;

public class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;
    private final String suffix;

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double step, String suffix) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
    }

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
        this(name, description, defaultValue, min, max, step, "");
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public String getSuffix() {
        return suffix;
    }

    public float getFloatValue() {
        return value != null ? value.floatValue() : (float) min;
    }

    public int getIntValue() {
        return value != null ? value.intValue() : (int) min;
    }

    @Override
    public void setValue(Double value) {
        if (value == null) return;
        double clamped = Math.max(min, Math.min(max, value));
        if (step > 0) {
            clamped = Math.round(clamped / step) * step;
        }
        super.setValue(clamped);
    }
}
