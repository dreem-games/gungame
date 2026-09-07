import Phaser from 'phaser';

/**
 * Global Event Bus for decoupling components.
 * Components can emit and listen to events here instead of tightly coupling to each other.
 */
class EventBus extends Phaser.Events.EventEmitter {}

// Export a singleton instance
export const EventDispatcher = new EventBus();
