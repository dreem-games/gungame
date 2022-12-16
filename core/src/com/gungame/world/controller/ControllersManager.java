package com.gungame.world.controller;

import com.badlogic.gdx.graphics.Camera;
import com.gungame.world.objects.phisical.Hero;

public class ControllersManager {

    private final HeroController joystickController;
    private final HeroController keyboardController;
    private HeroController activeController;

    public ControllersManager(Hero hero, Camera camera) {
        joystickController = new HeroJoystickController(hero, camera);
        keyboardController = new HeroKeyboardHeroController(hero, camera);
    }

    public void control() {
        if (activeController != null) {
            if (activeController.control()) {
                return;
            }
        }

        if (joystickController.control()) {
            activeController = joystickController;
        } else if (keyboardController.control()) {
            activeController = keyboardController;
        }
    }
}
