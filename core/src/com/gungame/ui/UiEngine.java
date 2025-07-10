package com.gungame.ui;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.gungame.world.objects.phisical.Hero;

import java.util.ArrayList;
import java.util.List;

public class UiEngine implements Disposable {
    private final Hero hero;
    private final List<Ui> uis;
    private final boolean isMainHero;

    public UiEngine(Hero hero, boolean isMainHero) {
        this.isMainHero = isMainHero;
        this.uis = new ArrayList<>();
        this.hero = hero;
        initUis();
    }

    @Override
    public void dispose() {
        uis.forEach(Ui::dispose);
    }

    public void draw(SpriteBatch batch, Camera camera) {
        // TODO: вне мира это рисовать бы...
        uis.forEach(it -> it.draw(batch, camera, hero));
    }

    public void initUis() {
        uis.add(new HeroStaminaBar(isMainHero));
        uis.add(new Ammo(isMainHero));
        uis.add(new HeroHealthBar(isMainHero));
    }
}
