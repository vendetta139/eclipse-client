package ru.eclipse.client.module.impl;

import ru.eclipse.client.module.Category;
import ru.eclipse.client.module.Module;
import ru.eclipse.client.module.setting.BooleanSetting;

public class TargetHUDModule extends Module {
    private final BooleanSetting show3DModel = new BooleanSetting("3D Моделька", "Отображать 3D-модель противника в виджете", true);

    public TargetHUDModule() {
        super("TargetHUD", "Информативная плашка с ХП противника при атаке и наведении", Category.HUD_OVERLAYS, 0, true);
        addSetting(show3DModel);
    }

    public BooleanSetting getShow3DModel() {
        return show3DModel;
    }
}
