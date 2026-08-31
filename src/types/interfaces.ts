import Phaser from 'phaser';

export interface IUpdateable {
    update(time: number, delta: number): void;
}

export interface IDestroyable {
    destroy(fromScene?: boolean): void;
    isDestroyed?: boolean;
}

export type IEntity = IUpdateable &
    IDestroyable & {
        id: string;
        gameObject: Phaser.GameObjects.GameObject;
    };
