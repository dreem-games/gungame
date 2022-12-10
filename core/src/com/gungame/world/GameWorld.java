package com.gungame.world;

import aurelienribon.bodyeditor.BodyEditorLoader;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.TimeUtils;
import com.gungame.world.ground.GroundContainer;
import com.gungame.world.ground.GroundGenerationUtils;
import com.gungame.world.hero.HeroController;
import com.gungame.world.hero.HeroFactory;
import com.gungame.world.hero.HeroKeyboardHeroController;
import com.gungame.world.walls.WallsFactory;
import com.gungame.world.walls.WallsGenerationUtils;

import static com.gungame.world.GameWorldConfig.*;

public class GameWorld implements Disposable {
    private static final float WORLD_STEP_TIME = 1/60f;

    private HeroController heroController;
    private WallsFactory wallsFactory;
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
        var bodyEditorLoader = new BodyEditorLoader(Gdx.files.internal("texture/bodies.json"));

        world = new World(new Vector2(0, 0), true);
        world.setContactListener(new GameContactListener());
        var heroFactory = new HeroFactory(world, bodyEditorLoader);
        wallsFactory = new WallsFactory(world, bodyEditorLoader);
        groundContainer = new GroundContainer();

        WallsGenerationUtils.generateWalls(wallsFactory, 0, 0, VERTICAL_SIZE, HORIZONTAL_SIZE);
        float wallW = wallsFactory.getWallPieceSize().x, wallH = wallsFactory.getWallPieceSize().y;

        heroController = new HeroKeyboardHeroController(heroFactory.createMainHero(10, 10, 20), camera);
        GroundGenerationUtils.generateGrass(groundContainer, wallW, wallH, VERTICAL_SIZE - wallW, HORIZONTAL_SIZE - wallH);
        WallsGenerationUtils.generateBoxes(wallsFactory, wallW, wallH, VERTICAL_SIZE - wallW, HORIZONTAL_SIZE - wallH, .4f);
    }

    @Override
    public void dispose() {
        wallsFactory.dispose();
        groundContainer.dispose();
        world.dispose();
    }

    public void render(SpriteBatch batch, Camera camera) {
        float currentTime = TimeUtils.nanoTime() / 1000000f;
        float frameTime = Math.min(currentTime - lastWorldStepTime, 0.25f);
        lastWorldStepTime = currentTime;

        timeAccumulator += frameTime;
        if (timeAccumulator >= WORLD_STEP_TIME) {
            GameObjectUtils.updateGameObjects(world);
            wallsFactory.executeObjectsUpdates();
            heroController.control();
            world.step(WORLD_STEP_TIME, 6, 2);
            timeAccumulator -= WORLD_STEP_TIME;
        }

        groundContainer.draw(batch);
        GameObjectUtils.getSyncedBodySprites(world)
                .forEach(sprite -> sprite.draw(batch));
        if (debugRenderer != null) {
            debugRenderer.render(world, camera.combined);
        }
    }
}
