import { IEntity } from '../types/interfaces';

export class EntityManager {
    private entities: Map<string, IEntity> = new Map();
    private entitiesToRemove: string[] = [];

    public add(entity: IEntity) {
        this.entities.set(entity.id, entity);
    }

    public remove(id: string) {
        this.entitiesToRemove.push(id);
    }

    public get(id: string): IEntity | undefined {
        return this.entities.get(id);
    }

    public update(time: number, delta: number) {
        // First process removals from previous frames/events
        this.processRemovals();

        // Update all active entities
        for (const [_, entity] of this.entities) {
            if (entity.isDestroyed) {
                this.remove(entity.id);
            } else {
                entity.update(time, delta);
            }
        }
    }

    private processRemovals() {
        if (this.entitiesToRemove.length === 0) return;

        for (const id of this.entitiesToRemove) {
            const entity = this.entities.get(id);
            if (entity) {
                entity.destroy();
                this.entities.delete(id);
            }
        }

        this.entitiesToRemove = [];
    }

    public clear() {
        for (const [_, entity] of this.entities) {
            entity.destroy();
        }
        this.entities.clear();
        this.entitiesToRemove = [];
    }
}