package com.gungame.world.objects.phisical;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.gungame.world.objects.meta.*;

import java.util.Random;

import static com.gungame.world.GameWorldConfig.HORIZONTAL_SIZE;
import static com.gungame.world.GameWorldConfig.VERTICAL_SIZE;

public class WallsGenerationUtils {
    private static final String LOG_TAG = WallsGenerationUtils.class.getSimpleName();
    private static final Random random = new Random();

    public static void generateWalls(GameObjectFactory<StaticGameObject> wallsFactory, float x, float y, float width, float height) {
        var wallSize = wallsFactory.getObjectMetadata().getSize();
        float h = y;
        while (h <= height) {
            wallsFactory.createImmediately(x, h, 0);
            wallsFactory.createImmediately(x + width - wallSize.x, h, 0);
            h += wallSize.y;
        }

        float w = x;
        while (w <= width) {
            wallsFactory.createImmediately(w, y, 0);
            wallsFactory.createImmediately(w, y + height - wallSize.y, 0);
            w += wallSize.x;
        }
    }

    public static void generateBoxes(GameObjectFactory<Box> boxFactory, float x, float y, float width, float height, float filling) {
        Vector2 boxSize = boxFactory.getObjectMetadata().getSize();
        int totalFits = (int) Math.min((width - x) / boxSize.x, (height - y) / boxSize.y);
        int totalToGenerate = (int) (totalFits * filling);

        for (int i = 0; i < totalToGenerate; i++) {
            generateBox(boxFactory, x, y, width, height);
        }

        Gdx.app.debug(LOG_TAG, "generated " + totalToGenerate + " boxes");
    }

    private static void generateBox(GameObjectFactory<Box> boxFactory, float x, float y, float width, float height) {
        float boxX = random.nextFloat(x, x + width);
        float boxY = random.nextFloat(y, y + height);
        float boxRotation = random.nextFloat(-180, 180);
        boxFactory.create(boxX, boxY, boxRotation);
        Gdx.app.debug(LOG_TAG, "creating box(x=%s, y=%s, rotation=%s)".formatted(boxX, boxY, boxRotation));
    }

    public static void recreateBoxIfNecessaryOnCollision(GameObject objectA, GameObject objectB) {
        if (objectA.getType() != GameObjectType.BOX && objectB.getType() != GameObjectType.BOX
                || objectA.isActive() || objectB.isActive()) {
            return;
        }
        var toDestroy = objectA.getType() == GameObjectType.BOX ? objectA : objectB;

        toDestroy.markForDestroy();
        Gdx.app.debug(LOG_TAG, "destroying box(x=%s, y=%s, angle=%s)".formatted(
                toDestroy.getPosition().x, toDestroy.getPosition().y, toDestroy.getAngle()));

        var factoryManager = GameObjectFactoryManager.getInstance(toDestroy.getWorld());
        var wallSize = factoryManager.getWallFactory().getObjectMetadata().getSize();
        float wallW = wallSize.x, wallH = wallSize.y;
        generateBox(factoryManager.getBoxFactory(), wallW, wallH, VERTICAL_SIZE - wallW, HORIZONTAL_SIZE - wallH);
    }
}
