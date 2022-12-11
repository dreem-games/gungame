package com.gungame.world.hero;

import aurelienribon.bodyeditor.BodyEditorLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.gungame.world.GameObject;
import com.gungame.world.GameObjectType;
import com.gungame.world.GameObjectUtils;

public class HeroFactory implements Disposable {

    private final Vector2 heroSize = new Vector2(5, 4);
    private final Texture heroTexture = new Texture("texture/hero.png");
    private final World world;
    private final BodyEditorLoader bodyLoader;

    public HeroFactory(World world, BodyEditorLoader bodyLoader) {
        this.world = world;
        this.bodyLoader = bodyLoader;
    }

    public GameObject createMainHero(float x, float y, float rotation) {
        return GameObjectUtils.createGameObject(world, bodyLoader, "hero",
                heroTexture, heroSize, GameObjectType.HERO, x, y, rotation, this);
    }

    @Override
    public void dispose() {
        heroTexture.dispose();
    }
}
