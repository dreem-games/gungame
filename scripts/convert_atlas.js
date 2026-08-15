import fs from 'fs';

function parseLibGdxAtlas(text) {
    const lines = text.split('\n').map(l => l.trim()).filter(l => l.length > 0);
    const frames = {};
    let currentTexture = '';

    // Skip header (png name, size, format, filter, repeat)
    let i = 0;
    while(i < lines.length && !lines[i].includes('size:')) i++;
    i += 4; // skip size, format, filter, repeat

    while (i < lines.length) {
        const line = lines[i];
        if (!line.includes(':')) {
            // New region
            const name = line;
            i++;
            const rotate = lines[i++].split(':')[1].trim() === 'true';
            const xy = lines[i++].split(':')[1].split(',').map(s => parseInt(s.trim()));
            const size = lines[i++].split(':')[1].split(',').map(s => parseInt(s.trim()));
            const orig = lines[i++].split(':')[1].split(',').map(s => parseInt(s.trim()));
            const offset = lines[i++].split(':')[1].split(',').map(s => parseInt(s.trim()));
            const index = parseInt(lines[i++].split(':')[1].trim());

            frames[name] = {
                frame: { x: xy[0], y: xy[1], w: size[0], h: size[1] },
                rotated: rotate,
                trimmed: false,
                spriteSourceSize: { x: offset[0], y: offset[1], w: size[0], h: size[1] },
                sourceSize: { w: orig[0], h: orig[1] }
            };
        } else {
            i++;
        }
    }

    return { frames };
}

const atlases = ['hero', 'level1', 'explosion', 'projectiles'];
for (const name of atlases) {
    if (fs.existsSync(`public/assets/texture/${name}.atlas`)) {
        const text = fs.readFileSync(`public/assets/texture/${name}.atlas`, 'utf8');
        const json = parseLibGdxAtlas(text);
        fs.writeFileSync(`public/assets/texture/${name}.json`, JSON.stringify(json, null, 2));
    }
}
