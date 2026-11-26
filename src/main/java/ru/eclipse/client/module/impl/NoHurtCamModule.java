package ru.eclipse.client.module.impl;

import ru.eclipse.client.module.Category;
import ru.eclipse.client.module.Module;

public class NoHurtCamModule extends Module {
    public NoHurtCamModule() {
        super("NoHurtCam", "Отключает раздражающую тряску экрана при получении урона", Category.ANIMATIONS, 0, true);
    }
}
