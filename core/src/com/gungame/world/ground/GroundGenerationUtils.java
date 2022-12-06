package com.gungame.world.ground;

public class GroundGenerationUtils {

    public static void generateGrass(GroundEngine groundEngine, float x, float y, float width, float height) {
        float h = x, w = y;

        while (h <= height && w <= width) {
            groundEngine.createGrassPiece(w, h);

            w += groundEngine.getGrassSize().x;
            if (w > width) {
                h += groundEngine.getGrassSize().y;
                w = x;
            }
        }
    }
}
