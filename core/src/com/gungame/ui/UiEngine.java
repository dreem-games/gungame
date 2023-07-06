package com.gungame.ui;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.gungame.world.objects.phisical.Hero;

import java.util.ArrayList;
import java.util.List;

public class UiEngine implements Disposable {
    private final Hero mainHero;
    private final List<Ui> uis;

    public UiEngine(Hero mainHero) {
        this.uis = new ArrayList<>();
        this.mainHero = mainHero;
        initUis();
    }

    @Override
    public void dispose() {
        uis.forEach(Ui::dispose);
    }

    public void draw(SpriteBatch batch, Camera camera) {
        uis.forEach(it -> it.draw(batch, camera, mainHero));
    }

    public void initUis() {
        uis.add(new HeroStaminaBar());
    }
}
