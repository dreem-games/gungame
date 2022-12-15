package com.gungame.world.objects.meta;

import aurelienribon.bodyeditor.BodyEditorLoader;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.gungame.world.objects.phisical.Bullet;
import com.gungame.world.objects.phisical.Hero;
import com.gungame.world.objects.phisical.Box;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import static com.gungame.world.objects.meta.GameObjectUtils.createMassData;

public class GameObjectFactoryManager implements Disposable {
    private static final Map<World, GameObjectFactoryManager> instances = Collections.synchronizedMap(new IdentityHashMap<>());

    private static final GameObjectMetadata WALL_METADATA = new GameObjectMetadata(
            GameObjectType.WALL, "texture/wall.png", "wall", new Vector2(4, 4), createMassData(10, .5f, .5f));
    private static final GameObjectMetadata BOX_METADATA = new GameObjectMetadata(
            GameObjectType.BOX, "texture/box.jpg", "box", new Vector2(4, 4), createMassData(10, .5f, .5f));
    private static final GameObjectMetadata HERO_METADATA = new GameObjectMetadata(
            GameObjectType.HERO, "texture/hero.png", "hero", new Vector2(5, 4), createMassData(100, .3f, .4f));
    private static final GameObjectMetadata BULLET_METADATA = new GameObjectMetadata(
            GameObjectType.BULLET, "texture/bullet.png", "bullet", new Vector2(.5f, .1f), GameObjectUtils.createMassData(.1f, .8f, .11f));

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
