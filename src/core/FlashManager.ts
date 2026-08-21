import Phaser from 'phaser';

export class FlashManager {
    private scene: Phaser.Scene;

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
        this.createFlash(x, y, radius, 'flash_cone', angle);
    }

    private createFlash(x: number, y: number, radius: number, texture: string, rotation: number) {
        // Find objects that X, Y is currently inside (to ignore them)
        const ignoredBodies = new Set<MatterJS.BodyType>();
        const bodies = this.scene.matter.world.getAllBodies();

        for (const body of bodies) {
            if (this.scene.matter.containsPoint(body as any, x, y)) {
                ignoredBodies.add(body as any);
            }
        }

        const rtSize = radius * 2;
        const rt = this.scene.add.renderTexture(x, y, rtSize, rtSize);
        rt.setOrigin(0.5, 0.5);
        rt.setDepth(10);
        rt.setBlendMode(Phaser.BlendModes.ADD);

        // Draw the light texture into the center of the render texture
        // The texture is 512x512, we need to scale it to fit the radius
        const scale = (radius * 2) / 512;
        const lightImage = this.scene.add.image(rtSize / 2, rtSize / 2, texture);
        lightImage.setScale(scale);
        lightImage.setRotation(rotation);

        // Render it
        rt.draw(lightImage);
        lightImage.destroy();

        // Build shadow polygons and erase them
        const shadowGraphics = this.scene.add.graphics();
        shadowGraphics.fillStyle(0xffffff, 1);

        const maxDistance = radius * 2; // Arbitrary far distance for shadows

        for (const body of bodies) {
            if (ignoredBodies.has(body as any)) continue;

            const gameObject = body.gameObject as Phaser.GameObjects.GameObject | undefined;
            if (!gameObject?.getData('blocksVision')) continue;

            // Get vertices of the body
            const vertices = (body as any).vertices as {x: number, y: number}[];
            if (!vertices || vertices.length === 0) continue;

            // For each vertex, project it outward
            for (let i = 0; i < vertices.length; i++) {
                const v1 = vertices[i];
                const v2 = vertices[(i + 1) % vertices.length];

                // Calculate angles
                const angle1 = Math.atan2(v1.y - y, v1.x - x);
                const angle2 = Math.atan2(v2.y - y, v2.x - x);

                // Project points
                const pv1 = {
                    x: v1.x + Math.cos(angle1) * maxDistance,
                    y: v1.y + Math.sin(angle1) * maxDistance
                };
                const pv2 = {
                    x: v2.x + Math.cos(angle2) * maxDistance,
                    y: v2.y + Math.sin(angle2) * maxDistance
                };

                // Draw the shadow quad
                // Coordinates must be relative to the render texture center
                shadowGraphics.beginPath();
                shadowGraphics.moveTo(v1.x - x + rtSize / 2, v1.y - y + rtSize / 2);
                shadowGraphics.lineTo(pv1.x - x + rtSize / 2, pv1.y - y + rtSize / 2);
                shadowGraphics.lineTo(pv2.x - x + rtSize / 2, pv2.y - y + rtSize / 2);
                shadowGraphics.lineTo(v2.x - x + rtSize / 2, v2.y - y + rtSize / 2);
                shadowGraphics.closePath();
                shadowGraphics.fillPath();
            }
        }

        // Erase shadows from the render texture
        rt.erase(shadowGraphics);
        shadowGraphics.destroy();

        // Fade out tween
        this.scene.tweens.add({
            targets: rt,
            alpha: 0,
            duration: 150,
            ease: 'Power2',
            onComplete: () => {
                rt.destroy();
            }
        });
    }
}
