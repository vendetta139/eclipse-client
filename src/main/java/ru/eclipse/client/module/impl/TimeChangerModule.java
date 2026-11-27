package ru.eclipse.client.module.impl;

import ru.eclipse.client.module.Category;
import ru.eclipse.client.module.Module;
import ru.eclipse.client.module.setting.ModeSetting;
import ru.eclipse.client.module.setting.NumberSetting;

public class TimeChangerModule extends Module {
    private final ModeSetting mode = new ModeSetting("Режим", "Выбор времени суток", "Sunset", "Day", "Noon", "Sunset", "Night", "Midnight", "Custom");
    private final NumberSetting customTime = new NumberSetting("Свое время", "Точное время в тиках", 12000.0, 0.0, 24000.0, 500.0, "t");

    public TimeChangerModule() {
        super("TimeChanger", "Фиксирует время суток на клиенте", Category.WORLD_FOG, 0, false);
        addSetting(mode);
        addSetting(customTime);
    }

    public long getFixedTime() {
        if (!isEnabled()) return -1;
        if (mode.is("Day")) return 1000L;
        if (mode.is("Noon")) return 6000L;
        if (mode.is("Sunset")) return 12500L;
        if (mode.is("Night")) return 14000L;
        if (mode.is("Midnight")) return 18000L;
        if (mode.is("Custom")) return customTime.getValue().longValue();
        return -1;
    }
}
