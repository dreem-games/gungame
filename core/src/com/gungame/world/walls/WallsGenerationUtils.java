package com.gungame.world.walls;

import com.badlogic.gdx.math.Vector2;
import com.gungame.world.GameObject;
import com.gungame.world.GameObjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static com.gungame.world.GameWorldConfig.HORIZONTAL_SIZE;
import static com.gungame.world.GameWorldConfig.VERTICAL_SIZE;

public class WallsGenerationUtils {
    private static final Random random = new Random();
    private static final Logger logger = LoggerFactory.getLogger(WallsGenerationUtils.class);

    public static void generateWalls(WallsFactory wallsFactory, float x, float y, float width, float height) {
        float h = y;
        while (h <= height) {
            wallsFactory.createWallPiece(x, h);
            wallsFactory.createWallPiece(x + width - wallsFactory.getWallPieceSize().x, h);
            h += wallsFactory.getWallPieceSize().y;
        }

        float w = x;
        while (w <= width) {
            wallsFactory.createWallPiece(w, y);
            wallsFactory.createWallPiece(w, y + height - wallsFactory.getWallPieceSize().y);
            w += wallsFactory.getWallPieceSize().x;
        }
    }

    public static void generateBoxes(WallsFactory wallsFactory, float x, float y, float width, float height, float filling) {
        Vector2 boxSize = wallsFactory.getBoxSize();
        int totalFits = (int) Math.min((width - x) / boxSize.x, (height - y) / boxSize.y);
        int totalToGenerate = (int) (totalFits * filling);

        for (int i = 0; i < totalToGenerate; i++) {
            generateBox(wallsFactory, x, y, width, height);
        }

        logger.debug("generated " + totalToGenerate + " boxes");
    }

    private static void generateBox(WallsFactory wallsFactory, float x, float y, float width, float height) {
        float boxX = random.nextFloat(x, x + width);
        float boxY = random.nextFloat(y, y + height);
        float boxRotation = random.nextFloat(-180, 180);
        wallsFactory.createBox(boxX, boxY, boxRotation);
        logger.debug("creating box(x={}, y={}, rotation={})", boxX, boxY, boxRotation);
    }

    public static void recreateBoxIfNesseseryOnCollision(GameObject objectA, GameObject objectB) {
        if (objectA.getType() != GameObjectType.BOX && objectB.getType() != GameObjectType.BOX
                || objectA.isActive() || objectB.isActive()) {
            return;
        }
        GameObject toDestroy = objectA.getType() == GameObjectType.BOX ? objectA : objectB;

        toDestroy.markForDestroy();
        logger.debug("destroying box(x={}, y={}, angle={})",
                toDestroy.getPosition().x, toDestroy.getPosition().y, toDestroy.getAngle());

        WallsFactory factory = (WallsFactory) toDestroy.getParent();
        float wallW = factory.getWallPieceSize().x, wallH = factory.getWallPieceSize().y;
        generateBox(factory, wallW, wallH, VERTICAL_SIZE - wallW, HORIZONTAL_SIZE - wallH);
    }
}
