package com.gungame.world;

import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.TimeUtils;
import com.gungame.ui.UiEngine;
import com.gungame.world.collision.GameContactListener;
import com.gungame.controller.ControllersManager;
import com.gungame.world.objects.imaginary.GroundContainer;
import com.gungame.world.objects.imaginary.GroundGenerationUtils;
import com.gungame.world.objects.meta.GameObject;
import com.gungame.world.objects.meta.GameObjectFactoryManager;
import com.gungame.world.objects.meta.GameObjectUtils;
import com.gungame.world.objects.phisical.Hero;
import com.gungame.world.objects.phisical.WallsGenerationUtils;
import lombok.Getter;

import static com.gungame.world.GameWorldConfig.*;

public class GameWorld implements Disposable {
    private @Getter World phisicsWorld;
    private @Getter GameObjectFactoryManager physicalObjectFactoryManager;
    private @Getter RayHandler rayHandler;

    private ControllersManager controllersManager;
    private UiEngine uiEngine;
    private UiEngine uiEngine2;
    private GroundContainer groundContainer;
    private Box2DDebugRenderer debugRenderer;
    private @Getter Hero hero;
    private @Getter Hero hero2;

    private float lastWorldStepTime;
    public boolean isWorldToRestart = false;
    private float deathTime = 0;

    public void init(Camera camera) {
        Box2D.init();
        if (PHYSICS_DEBUG_MODE) {
            debugRenderer = new Box2DDebugRenderer(true, true, true, true, true, true);
        }

        phisicsWorld = new World(new Vector2(0, 0), true);
        phisicsWorld.setContactListener(new GameContactListener());

        rayHandler = new RayHandler(phisicsWorld);
        rayHandler.setAmbientLight(0f); // Тьма вне источников света
        rayHandler.setCulling(true);    // Оптимизация
        rayHandler.setBlur(true); // необязательно
        rayHandler.setShadows(true); // обязательно!

        physicalObjectFactoryManager = new GameObjectFactoryManager(this);
        groundContainer = new GroundContainer();

        WallsGenerationUtils.generateWalls(physicalObjectFactoryManager.getWallFactory(), 0, 0, VERTICAL_SIZE, HORIZONTAL_SIZE);
        var wallsSize = physicalObjectFactoryManager.getWallFactory().getObjectMetadata().getSize();
        float wallW = wallsSize.x, wallH = wallsSize.y;

        hero = physicalObjectFactoryManager.getHeroFactory().createImmediately(10, 10, 20);
        uiEngine = new UiEngine(hero, true);

        hero2 = physicalObjectFactoryManager.getHeroFactory().createImmediately(80, 40, 200);
        uiEngine2 = new UiEngine(hero2, false);

        controllersManager = new ControllersManager(hero, hero2, camera);

        GroundGenerationUtils.generateGrass(groundContainer, wallW, wallH, VERTICAL_SIZE - wallW * 2, HORIZONTAL_SIZE - wallH * 2);
        float wallW17 = wallW * 1.7f, wallH17 = wallH * 1.7f;
        WallsGenerationUtils.generateBoxes(physicalObjectFactoryManager.getBoxFactory(), wallW17, wallH17, VERTICAL_SIZE - wallW17 * 2, HORIZONTAL_SIZE - wallH17 * 2, .8f);
    }

    /**
     * Метод отсчитывает 3 секунды
     * после смерти одного из героев
     * затем сообщает что его нужно перезапустить
     */
    public void checkWorldForRestart(float now) {
        if ((hero.isToDestroy() || hero2.isToDestroy()) && deathTime == 0) {
            deathTime = now;
        }
        if (now - deathTime > 3000 && now - deathTime < 10000) {
            isWorldToRestart = true;
        }
    }

    @Override
    public void dispose() {
        uiEngine.dispose();
        physicalObjectFactoryManager.dispose();
        groundContainer.dispose();
        GameObjectUtils.getGameObjectsStream(phisicsWorld).forEach(GameObject::dispose);
        phisicsWorld.dispose();
        rayHandler.dispose();
    }

    public void render(SpriteBatch batch, OrthographicCamera camera) {
        float currentTime = TimeUtils.nanoTime() / 1000000f;
        float frameTime = Math.min(currentTime - lastWorldStepTime, 0.25f);
        lastWorldStepTime = currentTime;

        // шаг физического мира
        GameObjectUtils.getGameObjectsStream(phisicsWorld).forEach(GameObject::update);
        physicalObjectFactoryManager.executeUpdates();
        controllersManager.control();
        phisicsWorld.step(frameTime, 6, 2);
        rayHandler.update();

        // отрисовка графического
        groundContainer.drawBatch(batch);
        GameObjectUtils.getVisibleGameObjects(phisicsWorld).forEach(it -> it.draw(batch));
        if (debugRenderer != null) {
            debugRenderer.render(phisicsWorld, camera.combined);
        }
        uiEngine.draw(batch, camera);
        uiEngine2.draw(batch, camera);
        rayHandler.setCombinedMatrix(camera);
        rayHandler.render();

        checkWorldForRestart(currentTime);
    }
}
