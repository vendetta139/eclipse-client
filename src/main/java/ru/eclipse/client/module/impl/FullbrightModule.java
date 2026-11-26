package ru.eclipse.client.module.impl;

import ru.eclipse.client.module.Category;
import ru.eclipse.client.module.Module;
import ru.eclipse.client.module.setting.ModeSetting;

public class FullbrightModule extends Module {
    private final ModeSetting mode = new ModeSetting("Режим", "Способ осветления мира", "Both", "NightVision", "Gamma", "Both");

    public FullbrightModule() {
        super("Fullbright", "Максимальная яркость в пещерах и ночью без зелий", Category.WORLD_FOG, 0, true);
        addSetting(mode);
    }

    public ModeSetting getMode() {
        return mode;
    }
}
