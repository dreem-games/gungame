package com.gungame.world;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.TimeUtils;
import com.gungame.world.collision.GameContactListener;
import com.gungame.world.controller.ControllersManager;
import com.gungame.world.objects.imaginary.GroundContainer;
import com.gungame.world.objects.imaginary.GroundGenerationUtils;
import com.gungame.world.objects.meta.GameObject;
import com.gungame.world.objects.meta.GameObjectFactoryManager;
import com.gungame.world.objects.meta.GameObjectUtils;
import com.gungame.world.objects.phisical.WallsGenerationUtils;

import static com.gungame.world.GameWorldConfig.*;

public class GameWorld implements Disposable {
    private static final float WORLD_STEP_TIME = 1/60f;

    private ControllersManager controllersManager;
    private GameObjectFactoryManager factoryManager;
    private GroundContainer groundContainer;
    private World world;
    private Box2DDebugRenderer debugRenderer;

    private float lastWorldStepTime;
    private float timeAccumulator;

    public void init(Camera camera) {
        Box2D.init();
        if (PHYSICS_DEBUG_MODE) {
            debugRenderer = new Box2DDebugRenderer(true, true, true, true, true, true);
        }

        world = new World(new Vector2(0, 0), true);
        world.setContactListener(new GameContactListener());
        factoryManager = GameObjectFactoryManager.getInstance(world);
        groundContainer = new GroundContainer();

        WallsGenerationUtils.generateWalls(factoryManager.getWallFactory(), 0, 0, VERTICAL_SIZE, HORIZONTAL_SIZE);
        var wallsSize = factoryManager.getWallFactory().getObjectMetadata().size();
        float wallW = wallsSize.x, wallH = wallsSize.y;

        var hero = factoryManager.getHeroFactory().createImmediately(10, 10, 20);
        controllersManager = new ControllersManager(hero, camera);
        GroundGenerationUtils.generateGrass(groundContainer, wallW, wallH, VERTICAL_SIZE - wallW, HORIZONTAL_SIZE - wallH);
        WallsGenerationUtils.generateBoxes(factoryManager.getBoxFactory(), wallW, wallH, VERTICAL_SIZE - wallW, HORIZONTAL_SIZE - wallH, .4f);
    }

    @Override
    public void dispose() {
        factoryManager.dispose();
        groundContainer.dispose();
        world.dispose();
    }

    public void render(SpriteBatch batch, Camera camera) {
        float currentTime = TimeUtils.nanoTime() / 1000000f;
        float frameTime = Math.min(currentTime - lastWorldStepTime, 0.25f);
        lastWorldStepTime = currentTime;

        timeAccumulator += frameTime;
        if (timeAccumulator >= WORLD_STEP_TIME) {
            GameObjectUtils.getGameObjects(world).forEach(GameObject::update);
            factoryManager.executeUpdates();
            controllersManager.control();
            world.step(WORLD_STEP_TIME, 6, 2);
            timeAccumulator -= WORLD_STEP_TIME;
        }
        groundContainer.draw(batch);
        GameObjectUtils.getVisibleGameObjects(world).forEach(it -> it.draw(batch));
        if (debugRenderer != null) {
            debugRenderer.render(world, camera.combined);
        }
    }
}
