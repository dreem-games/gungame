package com.gungame.controller;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.utils.Null;
import com.gungame.world.objects.phisical.Hero;

public class ControllersManager {
    private final HeroController joystickController;
    private final @Null HeroController additionalJoystickController;
    private final HeroController keyboardController;
    private HeroController activeHeroController;

    public ControllersManager(Hero hero, @Null Hero additionalHero, Camera camera) {
        joystickController = new HeroJoystickController(hero, camera, null);
        keyboardController = new HeroKeyboardHeroController(hero, camera);  // только для гг работает

        if (additionalHero != null && HeroJoystickController.connectedControllersCount() > 1) {
            additionalJoystickController = new HeroJoystickController(additionalHero, camera, (HeroJoystickController) joystickController);
        } else {
            additionalJoystickController = null;
        }
    }

    public void control() {
        if (additionalJoystickController != null) {
            additionalJoystickController.control();
        }

        if (activeHeroController != null) {
            if (activeHeroController.control()) {
                return;
            }
        }

        if (!joystickController.control()) {
            activeHeroController = joystickController;
        } else if (keyboardController.control()) {
            activeHeroController = keyboardController;
        } else {
            activeHeroController = null;
        }
    }
}
