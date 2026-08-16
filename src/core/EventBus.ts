import Phaser from 'phaser';

/**
 * Global Event Bus for decoupling components.
 * Components can emit and listen to events here instead of tightly coupling to each other.
 */
class EventBus extends Phaser.Events.EventEmitter {
    constructor() {
        super();
    }
}

// Export a singleton instance
export const EventDispatcher = new EventBus();

// Define standard events
export enum GameEvents {
    PLAYER_HEALTH_CHANGED = 'player-health-changed',
    PLAYER_DIED = 'player-died',
    WEAPON_FIRED = 'weapon-fired',
    GAME_OVER = 'game-over'
}