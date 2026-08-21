const fs = require('fs');
const json = JSON.parse(fs.readFileSync('public/assets/texture/bodies.json', 'utf8'));
console.log(Object.keys(json));
if (json.hero) console.log("hero", Object.keys(json.hero));
