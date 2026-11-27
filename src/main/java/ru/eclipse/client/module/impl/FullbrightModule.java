package ru.eclipse.client.module.impl;

import ru.eclipse.client.module.Category;
import ru.eclipse.client.module.Module;
import ru.eclipse.client.module.setting.ModeSetting;

public class FullbrightModule extends Module {
    private final ModeSetting mode = new ModeSetting("Режим", "Как именно осветлять мир", "Both", "NightVision", "Gamma", "Both");

    public FullbrightModule() {
        super("Fullbright", "Убирает темноту в пещерах и ночью", Category.WORLD_FOG, 0, true);
        addSetting(mode);
    }

    public ModeSetting getMode() {
        return mode;
    }
}
