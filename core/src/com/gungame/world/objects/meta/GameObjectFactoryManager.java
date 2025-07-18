package com.gungame.world.objects.meta;

import aurelienribon.bodyeditor.BodyEditorLoader;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import com.gungame.world.GameWorld;
import com.gungame.world.objects.phisical.*;
import lombok.Getter;

@Getter
public class GameObjectFactoryManager implements Disposable {
    private final GameObjectFactory<StaticGameObject> wallFactory;
    private final GameObjectFactory<Box> boxFactory;
    private final GameObjectFactory<Barrel> barrelFactory;
    private final GameObjectFactory<Hero> heroFactory;
    private final GameObjectFactory<Bullet> bulletFactory;
    private final GameObjectFactory<Grenade> grenadeFactory;

    public GameObjectFactoryManager(GameWorld world) {
        var wallMetadata = new GameObjectMetadataBuilder()
                .setType(GameObjectType.WALL)
                .setBodyName("wall", "png")
                .setSize(2, 2)
                .setLinearDamping(0)
                .setAngularDamping(10)
                .createGameObjectMetadata();
        var boxMetadata = new GameObjectMetadataBuilder()
                .setType(GameObjectType.BOX)
                .setBodyName("box", "jpg")
                .setSize(2, 2)
                .setMassData(10, .5f, .5f)
                .setLinearDamping(10)
                .setAngularDamping(100)
                .createGameObjectMetadata();
        var heroMetadata = new GameObjectMetadataBuilder()
                .setType(GameObjectType.HERO)
                .setBodyName("hero", "png")
                .setSize(2.5f, 2)
                .setMassData(100, .3f, .4f)
                .setLinearDamping(5)
                .setAngularDamping(10)
                .createGameObjectMetadata();
        var bulletMetadata = new GameObjectMetadataBuilder()
                .setType(GameObjectType.BULLET)
                .setBodyName("bullet", "png")
                .setSize(.5f, .1f)
                .setMassData(.001f, .8f, .11f)
                .setBullet()
                .createGameObjectMetadata();
        var barrelMetadata = new GameObjectMetadataBuilder()
                .setType(GameObjectType.BARREL)
                .setBodyName("barrel", "png")
                .setSize(3, 3)
                .setMassData(10, .5f, .5f)
                .setLinearDamping(10)
                .setAngularDamping(100)
                .createGameObjectMetadata();
        var grenadeMetadata = new GameObjectMetadataBuilder()
                .setType(GameObjectType.GRENADE)
                .setBodyName("grenade", "png")
                .setSize(1f, 1f)
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
        barrelFactory = new GameObjectFactory<>(world, bodyLoader, barrelMetadata);
        grenadeFactory = new GameObjectFactory<>(world, bodyLoader, grenadeMetadata);
    }

    public void executeUpdates() {
        wallFactory.executeObjectsUpdates();
        boxFactory.executeObjectsUpdates();
        heroFactory.executeObjectsUpdates();
        bulletFactory.executeObjectsUpdates();
        barrelFactory.executeObjectsUpdates();
        grenadeFactory.executeObjectsUpdates();
    }

    @Override
    public void dispose() {
        wallFactory.dispose();
        boxFactory.dispose();
        heroFactory.dispose();
        bulletFactory.dispose();
        barrelFactory.dispose();
        grenadeFactory.dispose();
    }
}
