package ru.eclipse.client.module.impl;

import ru.eclipse.client.module.Category;
import ru.eclipse.client.module.Module;
import ru.eclipse.client.module.setting.NumberSetting;

public class LowFireModule extends Module {
    private final NumberSetting height = new NumberSetting("Сдвиг по Y", "Насколько низко опустить огонь", -0.30, -0.60, 0.0, 0.05);

    public LowFireModule() {
        super("LowFire", "Опускает огонь ниже, чтобы не мешал обзору", Category.HUD_OVERLAYS, 0, true);
        addSetting(height);
    }

    public NumberSetting getHeight() {
        return height;
    }
}
