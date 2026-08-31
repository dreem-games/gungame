import Phaser from 'phaser';
import { createNoise4D } from 'simplex-noise';

export function generateSeamlessNoiseTexture(scene: Phaser.Scene, key: string, size: number = 512) {
    if (scene.textures.exists(key)) {
        return;
    }

    const canvas = document.createElement('canvas');
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d')!;
    const imgData = ctx.createImageData(size, size);

    const noise4D = createNoise4D();

    const noiseScale = 3.0; // Increased frequency for more details

    for (let x = 0; x < size; x++) {
        for (let y = 0; y < size; y++) {
            const s = x / size;
            const t = y / size;

            const dx = Math.cos(s * 2 * Math.PI) * noiseScale;
            const dy = Math.sin(s * 2 * Math.PI) * noiseScale;
            const dz = Math.cos(t * 2 * Math.PI) * noiseScale;
            const dw = Math.sin(t * 2 * Math.PI) * noiseScale;

            let noiseValue = noise4D(dx, dy, dz, dw);
            noiseValue = (noiseValue + 1) / 2;

            // Add an octave for detail
            let noiseValue2 = noise4D(dx * 2, dy * 2, dz * 2, dw * 2);
            noiseValue2 = (noiseValue2 + 1) / 2;

            const finalNoise = noiseValue * 0.8 + noiseValue2 * 0.2;

            // Keep values near white so MULTIPLY only adds subtle shading,
            // not dark blotches that hide the grass.
            const color = 255 - Math.floor(finalNoise * 45);

            const index = (x + y * size) * 4;
            imgData.data[index + 0] = color;
            imgData.data[index + 1] = color;
            imgData.data[index + 2] = color;
            imgData.data[index + 3] = 255;
        }
    }

    ctx.putImageData(imgData, 0, 0);
    scene.textures.addCanvas(key, canvas);
}

// Мягкий тёмный след (radial gradient: центр — непрозрачный, края — в ноль)
export function generateScorchTexture(scene: Phaser.Scene, key: string, size: number = 256) {
    if (scene.textures.exists(key)) {
        return;
    }

    const canvas = document.createElement('canvas');
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d')!;

    const cx = size / 2;
    const gradient = ctx.createRadialGradient(cx, cx, 0, cx, cx, cx);
    gradient.addColorStop(0, 'rgba(0, 0, 0, 0.55)');
    gradient.addColorStop(0.55, 'rgba(0, 0, 0, 0.30)');
    gradient.addColorStop(1, 'rgba(0, 0, 0, 0)');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, size, size);

    scene.textures.addCanvas(key, canvas);
}
