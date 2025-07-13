package com.gungame.world.objects.imaginary;

public class GroundGenerationUtils {

    public static void generateGrass(GroundContainer groundContainer, float x, float y, float width, float height) {
        float h = x, w = y;
        float endingWidth = x + width;
        float endingHigh = y + height;

        while (h <= endingHigh && w <= endingWidth) {
            groundContainer.createGrassPiece(w, h);

            w += groundContainer.getGrassSize().x;
            if (w > endingWidth) {
                h += groundContainer.getGrassSize().y;
                w = x;
            }
        }
    }
}
