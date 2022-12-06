package com.gungame.world.walls;

import com.badlogic.gdx.math.Vector2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class WallsGenerationUtils {
    private static final Random random = new Random();
    private static final Logger logger = LoggerFactory.getLogger(WallsGenerationUtils.class);

    public static void generateWalls(WallsEngine wallsEngine, float x, float y, float width, float height) {
        float h = y;
        while (h <= height) {
            wallsEngine.createWallPiece(x, h);
            wallsEngine.createWallPiece(x + width - wallsEngine.getWallPieceSize().x, h);
            h += wallsEngine.getWallPieceSize().y;
        }

        float w = x;
        while (w <= width) {
            wallsEngine.createWallPiece(w, y);
            wallsEngine.createWallPiece(w, y + height - wallsEngine.getWallPieceSize().y);
            w += wallsEngine.getWallPieceSize().x;
        }
    }

    public static void generateBoxes(WallsEngine wallsEngine, float x, float y, float width, float height, float filling) {
        // TODO: упростить
        Vector2 boxSize = wallsEngine.getBoxSize();
        int totalFits = (int) Math.min((width - x) / boxSize.x, (height - y) / boxSize.y);
        int totalToGenerate = (int) (totalFits * filling);

        for (int i = 0; i < totalToGenerate; i++) {
            while (true) {
                float boxX = random.nextFloat(x, x + width);
                float boxY = random.nextFloat(y, y + height);
                float boxRotation = random.nextFloat(-180, 180);
                if (wallsEngine.createBox(boxX, boxY, boxRotation)) {
                    logger.debug("created box(x={}, y={}, rotation={})", boxX, boxY, boxRotation);
                    break;
                }
            }
        }

        logger.debug("generated " + totalToGenerate + " boxes");
    }
}
