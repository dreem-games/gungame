package com.gungame.world.objects.meta;

import aurelienribon.bodyeditor.BodyEditorLoader;
import com.badlogic.gdx.Gdx;
import com.gungame.assets.TextureManager;
import com.gungame.world.GameWorld;
import com.gungame.world.objects.phisical.*;
import lombok.Getter;

@Getter
public class GameObjectFactoryManager {
    private final GameObjectFactory<StaticGameObject> wallFactory;
    private final GameObjectFactory<Box> boxFactory;
    private final GameObjectFactory<Barrel> barrelFactory;
    private final GameObjectFactory<Hero> heroFactory;
    private final GameObjectFactory<Bullet> bulletFactory;
    private final GameObjectFactory<Grenade> grenadeFactory;

    public GameObjectFactoryManager(GameWorld world) {
        var wallMetadata = new GameObjectMetadataBuilder()
                .setType(GameObjectType.WALL)
                .setBodyName(TextureManager.AtlasType.LEVEL_1, "wall")
                .setSize(1, 1)
                .setLinearDamping(0)
                .setAngularDamping(10)
                .setRestitution(0.1f)
                .createGameObjectMetadata();
        var boxMetadata = new GameObjectMetadataBuilder()
                .setType(GameObjectType.BOX)
                .setBodyName(TextureManager.AtlasType.LEVEL_1, "box")
                .setSize(1, 1)
                .setMassData(10, .5f, .5f)
                .setLinearDamping(10)
                .setAngularDamping(100)
                .setRestitution(0.1f)
                .createGameObjectMetadata();
        var heroMetadata = new GameObjectMetadataBuilder()
                .setType(GameObjectType.HERO)
                .setBodyName(TextureManager.AtlasType.HERO, "hero")
                .setSize(1.25f, 1)
                .setMassData(100, .3f, .4f)
                .setLinearDamping(5)
                .setAngularDamping(10)
                .createGameObjectMetadata();
        var bulletMetadata = new GameObjectMetadataBuilder()
                .setType(GameObjectType.BULLET)
                .setBodyName(TextureManager.AtlasType.PROJECTILES, "bullet")
                .setSize(.25f, .05f)
                .setMassData(.001f, .8f, .11f)
                .setBullet()
                .createGameObjectMetadata();
        var barrelMetadata = new GameObjectMetadataBuilder()
                .setType(GameObjectType.BARREL)
                .setBodyName(TextureManager.AtlasType.LEVEL_1, "barrel")
                .setBodyCircleDiameter(.8f)
                .setMassData(12, .5f, .5f)
                .setLinearDamping(10)
                .setAngularDamping(100)
                .createGameObjectMetadata();
        var grenadeMetadata = new GameObjectMetadataBuilder()
                .setType(GameObjectType.GRENADE)
                .setBodyName(TextureManager.AtlasType.PROJECTILES, "grenade")
                .setSize(0.25f, 0.4f)
                .setMassData(.0f, .8f, .11f)
                .setLinearDamping(0)
                .setAngularDamping(0)
                .setRestitution(0.7f)
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
}
