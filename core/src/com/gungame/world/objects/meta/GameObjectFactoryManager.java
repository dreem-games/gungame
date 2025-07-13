package com.gungame.world.objects.meta;

import aurelienribon.bodyeditor.BodyEditorLoader;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import com.gungame.world.GameWorld;
import com.gungame.world.objects.phisical.Box;
import com.gungame.world.objects.phisical.Bullet;
import com.gungame.world.objects.phisical.Hero;
import lombok.Getter;

@Getter
public class GameObjectFactoryManager implements Disposable {
    private final GameObjectFactory<StaticGameObject> wallFactory;
    private final GameObjectFactory<Box> boxFactory;
    private final GameObjectFactory<Hero> heroFactory;
    private final GameObjectFactory<Bullet> bulletFactory;

    public GameObjectFactoryManager(GameWorld world) {
        var wallMetadata = new GameObjectMetadataBuilder()
                .setType(GameObjectType.WALL)
                .setBodyName("wall", "png")
                .setSize(4, 4)
                .setLinearDamping(0)
                .setAngularDamping(10)
                .createGameObjectMetadata();
        var boxMetadata = new GameObjectMetadataBuilder()
                .setType(GameObjectType.BOX)
                .setBodyName("box", "jpg")
                .setSize(4, 4)
                .setMassData(10, .5f, .5f)
                .setLinearDamping(10)
                .setAngularDamping(100)
                .createGameObjectMetadata();
        var heroMetadata = new GameObjectMetadataBuilder()
                .setType(GameObjectType.HERO)
                .setBodyName("hero", "png")
                .setSize(5, 4)
                .setMassData(100, .3f, .4f)
                .setLinearDamping(5)
                .setAngularDamping(10)
                .createGameObjectMetadata();
        var bulletMetadata = new GameObjectMetadataBuilder()
                .setType(GameObjectType.BULLET)
                .setBodyName("bullet", "png")
                .setSize(1.f, .2f)
                .setMassData(.0f, .8f, .11f)
                .setLinearDamping(0)
                .setAngularDamping(10)
                .setFriction(0f)
                .createGameObjectMetadata();

        var bodyLoader = new BodyEditorLoader(Gdx.files.internal("texture/bodies.json"));
        wallFactory = new GameObjectFactory<>(world, bodyLoader, wallMetadata);
        boxFactory = new GameObjectFactory<>(world, bodyLoader, boxMetadata);
        heroFactory = new GameObjectFactory<>(world, bodyLoader, heroMetadata);
        bulletFactory = new GameObjectFactory<>(world, bodyLoader, bulletMetadata);
    }

    public void executeUpdates() {
        wallFactory.executeObjectsUpdates();
        boxFactory.executeObjectsUpdates();
        heroFactory.executeObjectsUpdates();
        bulletFactory.executeObjectsUpdates();
    }

    @Override
    public void dispose() {
        wallFactory.dispose();
        boxFactory.dispose();
        heroFactory.dispose();
        bulletFactory.dispose();
    }
}
