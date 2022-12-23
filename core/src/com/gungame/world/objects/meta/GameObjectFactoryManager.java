package com.gungame.world.objects.meta;

import aurelienribon.bodyeditor.BodyEditorLoader;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.gungame.world.objects.phisical.Box;
import com.gungame.world.objects.phisical.Bullet;
import com.gungame.world.objects.phisical.Hero;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;


public class GameObjectFactoryManager implements Disposable {
    private static final Map<World, GameObjectFactoryManager> instances = Collections.synchronizedMap(new IdentityHashMap<>());

    private static final GameObjectMetadata WALL_METADATA = new GameObjectMetadataBuilder()
            .setType(GameObjectType.WALL)
            .setBodyName("wall", "png")
            .setSize(4, 4)
            .setLinearDamping(0)
            .setAngularDamping(10)
            .createGameObjectMetadata();
    private static final GameObjectMetadata BOX_METADATA = new GameObjectMetadataBuilder()
            .setType(GameObjectType.BOX)
            .setBodyName("box", "jpg")
            .setSize(4, 4)
            .setMassData(10, .5f, .5f)
            .setLinearDamping(10)
            .setAngularDamping(100)
            .createGameObjectMetadata();
    private static final GameObjectMetadata HERO_METADATA = new GameObjectMetadataBuilder()
            .setType(GameObjectType.HERO)
            .setBodyName("hero", "png")
            .setSize(5, 4)
            .setMassData(100, .3f, .4f)
            .setLinearDamping(5)
            .setAngularDamping(10)
            .createGameObjectMetadata();
    private static final GameObjectMetadata BULLET_METADATA = new GameObjectMetadataBuilder()
            .setType(GameObjectType.BULLET)
            .setBodyName("bullet", "png")
            .setSize(.5f, .1f)
            .setMassData(.0f, .8f, .11f)
            .setLinearDamping(0)
            .setAngularDamping(10)
            .setFriction(0f)
            .createGameObjectMetadata();

    private final GameObjectFactory<StaticGameObject> wallFactory;
    private final GameObjectFactory<Box> boxFactory;
    private final GameObjectFactory<Hero> heroFactory;
    private final GameObjectFactory<Bullet> bulletFactory;

    private GameObjectFactoryManager(World world) {
        var bodyLoader = new BodyEditorLoader(Gdx.files.internal("texture/bodies.json"));
        wallFactory = new GameObjectFactory<>(world, bodyLoader, WALL_METADATA);
        boxFactory = new GameObjectFactory<>(world, bodyLoader, BOX_METADATA);
        heroFactory = new GameObjectFactory<>(world, bodyLoader, HERO_METADATA);
        bulletFactory = new GameObjectFactory<>(world, bodyLoader, BULLET_METADATA);
    }

    public static GameObjectFactoryManager getInstance(World world) {
        return instances.computeIfAbsent(world, GameObjectFactoryManager::new);
    }

    public GameObjectFactory<StaticGameObject> getWallFactory() {
        return wallFactory;
    }

    public GameObjectFactory<Box> getBoxFactory() {
        return boxFactory;
    }

    public GameObjectFactory<Hero> getHeroFactory() {
        return heroFactory;
    }

    public GameObjectFactory<Bullet> getBulletFactory() {
        return bulletFactory;
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
