package com.gungame.controller;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.utils.Null;
import com.gungame.world.objects.phisical.Hero;

public class ControllersManager {
    private final HeroController heroController;
    private final @Null HeroController additionalJoystickController;

    public ControllersManager(Hero hero, @Null Hero additionalHero, Camera camera) {
        int controllersCount = HeroJoystickController.connectedControllersCount();
        if (controllersCount > 1) {
            heroController = new HeroJoystickController(hero, camera, true);
        } else {
            heroController = new HeroKeyboardHeroController(hero, camera);
        }

        if (additionalHero != null && controllersCount > 0) {
            additionalJoystickController = new HeroJoystickController(additionalHero, camera, false);
        } else {
            additionalJoystickController = null;
        }
    }

    public void control() {
        heroController.control();
        if (additionalJoystickController != null) {
            additionalJoystickController.control();
        }
    }
}
