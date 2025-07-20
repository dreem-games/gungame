package com.gungame.world.collision;

public enum CollisionCategory {
    ALL(0b11111),
    ALL_PHYSICAL(0b00111),
    NORMAL_OBJECTS(0b00110),
    MICRO_OBJECTS(0b00001),
    SMALL_OBJECTS(0b00010),
    HEIGHT_OBJECTS(0b00100),
    HIGH_LIGHT(0b01000),
    LOW_LIGHT(0b10000);

    private final short bits;

    CollisionCategory(int bits) {
        this.bits = (short) bits;
    }

    public short getBits() {
        return bits;
    }
}
