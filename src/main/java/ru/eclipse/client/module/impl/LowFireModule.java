package ru.eclipse.client.module.impl;

import ru.eclipse.client.module.Category;
import ru.eclipse.client.module.Module;
import ru.eclipse.client.module.setting.NumberSetting;

public class LowFireModule extends Module {
    private final NumberSetting height = new NumberSetting("Сдвиг по Y", "Опускает анимацию огня вниз", -0.30, -0.60, 0.0, 0.05);

    public LowFireModule() {
        super("LowFire", "Опускает уровень огня на экране от первого лица", Category.HUD_OVERLAYS, 0, true);
        addSetting(height);
    }

    public NumberSetting getHeight() {
        return height;
    }
}
