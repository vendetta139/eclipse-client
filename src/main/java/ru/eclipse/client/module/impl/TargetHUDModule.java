package ru.eclipse.client.module.impl;

import ru.eclipse.client.module.Category;
import ru.eclipse.client.module.Module;
import ru.eclipse.client.module.setting.BooleanSetting;

public class TargetHUDModule extends Module {
    private final BooleanSetting show3DModel = new BooleanSetting("3D Моделька", "Рисует 3D-модель игрока вместо значка", true);

    public TargetHUDModule() {
        super("TargetHUD", "Показывает ХП, имя и дистанцию до цели", Category.HUD_OVERLAYS, 0, true);
        addSetting(show3DModel);
    }

    public BooleanSetting getShow3DModel() {
        return show3DModel;
    }
}
