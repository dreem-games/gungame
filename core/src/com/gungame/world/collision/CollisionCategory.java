package com.gungame.world.collision;

public enum CollisionCategory {
    ALL(0b11),
    SMALL_OBJECTS(0b01),
    HEIGHT_OBJECTS(0b10);

    private final short bitMask;

    CollisionCategory(int BIT_MASK) {
        this.bitMask = (short) BIT_MASK;
    }

    public short getBitMask() {
        return bitMask;
    }
}
