package com.gungame.world.objects.imaginary;

public class GroundGenerationUtils {

    public static void generateGrass(GroundContainer groundContainer, float startX, float startY, float width, float height) {
        float w = startX;
        float h = startY;
        float endingWidth = startX + width;
        float endingHeight = startY + height;

        while (h <= endingHeight && w <= endingWidth) {
            groundContainer.createGrassPiece(w, h);

            w += groundContainer.getGrassSize().x;
            if (w > endingWidth) {
                h += groundContainer.getGrassSize().y;
                w = startX;
            }
        }
    }
}
