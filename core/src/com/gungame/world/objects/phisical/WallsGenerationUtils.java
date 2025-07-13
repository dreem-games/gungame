package com.gungame.world.objects.phisical;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.gungame.world.objects.meta.GameObject;
import com.gungame.world.objects.meta.GameObjectFactory;
import com.gungame.world.objects.meta.StaticGameObject;

import static com.badlogic.gdx.math.MathUtils.random;

public class WallsGenerationUtils {
    private static final String LOG_TAG = WallsGenerationUtils.class.getSimpleName();

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

    public static void  generateBoxes(GameObjectFactory<Box> boxFactory, float x, float y, float width, float height, float filling) {
        Vector2 boxSize = boxFactory.getObjectMetadata().getSize();
        int totalFits = (int) Math.min((width - x) / boxSize.x, (height - y) / boxSize.y);
        int totalToGenerate = (int) (totalFits * filling);

        for (int i = 0; i < totalToGenerate; i++) {
            generateObject(boxFactory, x, y, width, height);
        }
        Gdx.app.debug(LOG_TAG, "generated " + totalToGenerate + " boxes");
    }

    public static void generateBarrels(GameObjectFactory<Barrel> barrelFactory, float x, float y, float width, float height, float filling) {
        Vector2 barrelSize = barrelFactory.getObjectMetadata().getSize();
        int totalFits = (int) Math.min((width - x) / barrelSize.x, (height - y) / barrelSize.y);
        int totalToGenerate = (int) (totalFits * filling);

        for (int i = 0; i < totalToGenerate; i++) {
            generateObject(barrelFactory, x, y, width, height);
        }
        Gdx.app.debug(LOG_TAG, "generated " + totalToGenerate + " boxes");
    }

    private static <T extends GameObject> void generateObject(GameObjectFactory<T> factory, float x, float y, float width, float height) {
        float boxX = random.nextFloat(x, x + width);
        float boxY = random.nextFloat(y, y + height);
        float boxRotation = random.nextFloat(-180, 180);
        factory.create(boxX, boxY, boxRotation);
        Gdx.app.debug(LOG_TAG, "creating box(x=%s, y=%s, rotation=%s)".formatted(boxX, boxY, boxRotation));
    }
}
