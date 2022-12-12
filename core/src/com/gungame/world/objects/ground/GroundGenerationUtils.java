package com.gungame.world.objects.ground;

public class GroundGenerationUtils {

    public static void generateGrass(GroundContainer groundContainer, float x, float y, float width, float height) {
        float h = x, w = y;

        while (h <= height && w <= width) {
            groundContainer.createGrassPiece(w, h);

            w += groundContainer.getGrassSize().x;
            if (w > width) {
                h += groundContainer.getGrassSize().y;
                w = x;
            }
        }
    }
}
