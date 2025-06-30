package com.gungame.controller;

import com.badlogic.gdx.graphics.Camera;
import com.gungame.world.objects.phisical.Hero;

public class ControllersManager {

    private final HeroController joystickController;
    private final HeroController keyboardController;
    private HeroController activeController;
    private boolean isKeyboard;

    public ControllersManager(Hero hero, Camera camera, boolean keyboard) {
        joystickController = new HeroJoystickController(hero, camera);
        keyboardController = new HeroKeyboardHeroController(hero, camera);
        this.isKeyboard = keyboard;
    }

    public void control() {
        if (activeController != null) {
            if (activeController.control()) {
                return;
            }
        }

        if (!isKeyboard) {
            activeController = joystickController;
        } else {
            activeController = keyboardController;
        }
    }
}
