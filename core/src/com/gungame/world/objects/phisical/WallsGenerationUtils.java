package com.gungame.world.objects.phisical;

import com.badlogic.gdx.math.Vector2;
import com.gungame.world.objects.meta.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static com.gungame.world.GameWorldConfig.HORIZONTAL_SIZE;
import static com.gungame.world.GameWorldConfig.VERTICAL_SIZE;

public class WallsGenerationUtils {
    private static final Random random = new Random();
    private static final Logger logger = LoggerFactory.getLogger(WallsGenerationUtils.class);

    public static void generateWalls(GameObjectFactory<StaticGameObject> wallsFactory, float x, float y, float width, float height) {
        var wallSize = wallsFactory.getObjectMetadata().size();
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
        Vector2 boxSize = boxFactory.getObjectMetadata().size();
        int totalFits = (int) Math.min((width - x) / boxSize.x, (height - y) / boxSize.y);
        int totalToGenerate = (int) (totalFits * filling);

        for (int i = 0; i < totalToGenerate; i++) {
            generateBox(boxFactory, x, y, width, height);
        }

        logger.debug("generated " + totalToGenerate + " boxes");
    }

    private static void generateBox(GameObjectFactory<Box> boxFactory, float x, float y, float width, float height) {
        float boxX = random.nextFloat(x, x + width);
        float boxY = random.nextFloat(y, y + height);
        float boxRotation = random.nextFloat(-180, 180);
        boxFactory.create(boxX, boxY, boxRotation);
        logger.debug("creating box(x={}, y={}, rotation={})", boxX, boxY, boxRotation);
    }

    public static void recreateBoxIfNecessaryOnCollision(GameObject objectA, GameObject objectB) {
        if (objectA.getType() != GameObjectType.BOX && objectB.getType() != GameObjectType.BOX
                || objectA.isActive() || objectB.isActive()) {
            return;
        }
        var toDestroy = objectA.getType() == GameObjectType.BOX ? objectA : objectB;

        toDestroy.markForDestroy();
        logger.debug("destroying box(x={}, y={}, angle={})",
                toDestroy.getPosition().x, toDestroy.getPosition().y, toDestroy.getAngle());

        var factoryManager = GameObjectFactoryManager.getInstance(toDestroy.getWorld());
        var wallSize = factoryManager.getWallFactory().getObjectMetadata().size();
        float wallW = wallSize.x, wallH = wallSize.y;
        generateBox(factoryManager.getBoxFactory(), wallW, wallH, VERTICAL_SIZE - wallW, HORIZONTAL_SIZE - wallH);
    }
}
