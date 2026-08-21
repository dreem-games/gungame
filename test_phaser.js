const fs = require('fs');
const json = JSON.parse(fs.readFileSync('public/assets/texture/bodies.json', 'utf8'));

// Simulated Phaser fromJSON parsing
const config = json.rigidBodies.find(b => b.name === 'hero');
console.log(config.fixtures); // This is what PhysicsEditor format uses, wait... our file has 'polygons' and 'shapes'
console.log(config.shapes);
