package ru.eclipse.client.module.impl;

import ru.eclipse.client.module.Category;
import ru.eclipse.client.module.Module;
import ru.eclipse.client.module.setting.ModeSetting;

public class NoFogModule extends Module {
    private final ModeSetting mode = new ModeSetting("Режим", "Где отключать туман", "All", "All", "Atmosphere", "Water", "Lava");

    public NoFogModule() {
        super("NoFog", "Отключает туман в мире, воде и лаве", Category.WORLD_FOG, 0, true);
        addSetting(mode);
    }

    public ModeSetting getMode() {
        return mode;
    }
}
