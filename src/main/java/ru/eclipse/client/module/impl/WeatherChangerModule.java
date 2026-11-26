package ru.eclipse.client.module.impl;

import ru.eclipse.client.module.Category;
import ru.eclipse.client.module.Module;
import ru.eclipse.client.module.setting.ModeSetting;

public class WeatherChangerModule extends Module {
    private final ModeSetting mode = new ModeSetting("Погода", "Фиксация погоды", "Clear", "Clear", "Rain", "Thunder");

    public WeatherChangerModule() {
        super("WeatherChanger", "Визуальная фиксация погоды на клиенте (ясно, дождь, гроза)", Category.WORLD_FOG, 0, false);
        addSetting(mode);
    }

    public ModeSetting getMode() {
        return mode;
    }
}
