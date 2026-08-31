import Phaser from 'phaser';

export class FlashManager {
    private scene: Phaser.Scene;
    private flashId: number = 0;

    constructor(scene: Phaser.Scene) {
        this.scene = scene;
        this.createTextures();
    }

    private createTextures() {
        if (!this.scene.textures.exists('flash_radial')) {
            const canvas = document.createElement('canvas');
            canvas.width = 512;
            canvas.height = 512;
            const ctx = canvas.getContext('2d');
            if (ctx) {
                const gradient = ctx.createRadialGradient(256, 256, 0, 256, 256, 256);
                gradient.addColorStop(0, 'rgba(255, 50, 50, 0.8)');
                gradient.addColorStop(0.4, 'rgba(255, 0, 0, 0.4)');
                gradient.addColorStop(1, 'rgba(255, 0, 0, 0)');
                ctx.fillStyle = gradient;
                ctx.fillRect(0, 0, 512, 512);
            }
            this.scene.textures.addCanvas('flash_radial', canvas);
        }

        if (!this.scene.textures.exists('flash_cone')) {
            const canvas = document.createElement('canvas');
            canvas.width = 512;
            canvas.height = 512;
            const ctx = canvas.getContext('2d');
            if (ctx) {
                const gradient = ctx.createRadialGradient(256, 256, 0, 256, 256, 256);
                gradient.addColorStop(0, 'rgba(255, 200, 100, 0.9)');
                gradient.addColorStop(0.3, 'rgba(255, 50, 0, 0.6)');
                gradient.addColorStop(1, 'rgba(255, 0, 0, 0)');
                ctx.fillStyle = gradient;
                ctx.beginPath();
                ctx.moveTo(256, 256);
                ctx.arc(256, 256, 256, -Math.PI / 4, Math.PI / 4);
                ctx.closePath();
                ctx.fill();
            }
            this.scene.textures.addCanvas('flash_cone', canvas);
        }
    }

    public createExplosionFlash(x: number, y: number, radius: number) {
        this.createFlash(x, y, radius, 'flash_radial', 0);
    }

    public createShotFlash(x: number, y: number, angle: number, radius: number) {
        const key = `flash_cone_${this.flashId++}`;
        const canvas = document.createElement('canvas');
        canvas.width = 512;
        canvas.height = 512;

        const ctx = canvas.getContext('2d');
        const source = this.scene.textures.get('flash_cone').getSourceImage() as CanvasImageSource;
        if (ctx) {
            const bodies = this.scene.matter.world.getAllBodies().filter((body) => {
                const object = body.gameObject as Phaser.GameObjects.GameObject | undefined;
                return (
                    object?.getData('blocksVision') && !this.scene.matter.containsPoint(body as MatterJS.BodyType, x, y)
                );
            });

            ctx.beginPath();
            ctx.moveTo(256, 256);
            for (let i = 0; i <= 24; i++) {
                const rayAngle = angle - Math.PI / 4 + (Math.PI / 2) * (i / 24);
                const endX = x + Math.cos(rayAngle) * radius;
                const endY = y + Math.sin(rayAngle) * radius;
                let nearest = 1;

                for (const body of bodies) {
                    const vertices = (body as MatterJS.BodyType).vertices;
                    if (!vertices?.length) continue;

                    for (let j = 0; j < vertices.length; j++) {
                        const start = vertices[j];
                        const end = vertices[(j + 1) % vertices.length];
                        const rayX = endX - x;
                        const rayY = endY - y;
                        const edgeX = end.x - start.x;
                        const edgeY = end.y - start.y;
                        const denominator = rayX * edgeY - rayY * edgeX;
                        if (denominator === 0) continue;

                        const offsetX = start.x - x;
                        const offsetY = start.y - y;
                        const rayProgress = (offsetX * edgeY - offsetY * edgeX) / denominator;
                        const edgeProgress = (offsetX * rayY - offsetY * rayX) / denominator;
                        if (rayProgress >= 0 && rayProgress < nearest && edgeProgress >= 0 && edgeProgress <= 1) {
                            nearest = rayProgress;
                        }
                    }
                }

                ctx.lineTo(256 + Math.cos(rayAngle) * nearest * 256, 256 + Math.sin(rayAngle) * nearest * 256);
            }
            ctx.closePath();
            ctx.clip();
            ctx.translate(256, 256);
            ctx.rotate(angle);
            ctx.drawImage(source, -256, -256);
        }

        this.scene.textures.addCanvas(key, canvas);
        this.createFlash(x, y, radius, key, 0, () => this.scene.textures.remove(key));
    }

    private createFlash(x: number, y: number, radius: number, texture: string, rotation: number, cleanup?: () => void) {
        const flash = this.scene.add
            .image(x, y, texture)
            .setScale((radius * 2) / 512)
            .setRotation(rotation)
            .setDepth(10);

        this.scene.tweens.add({
            targets: flash,
            alpha: 0,
            duration: 150,
            ease: 'Power2',
            onComplete: () => {
                flash.destroy();
                cleanup?.();
            }
        });
    }
}
