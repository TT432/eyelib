// tinycolor2 1.4.2 golden oracle 生成器：wintersky 用到的颜色转换用例。
// 运行：node scripts/wintersky-oracle/tinycolor_cases.mjs（需在 build/_oracle 下运行）
// 输出：src/test/resources/wintersky/tinycolor_cases.json
import tinycolor from '../../build/_oracle/node_modules/tinycolor2/tinycolor.js';
import {fileURLToPath} from 'url';
import path from 'path';
import fs from 'fs';

const cases = [];
const add = (kind, input, expected) => cases.push({kind, input, expected});

// wintersky config.js parseColor 的输入面
for (const hex of ['#ffffff', '#000000', '#ff0000', '#00ff88', '#123456',
                   '#ffffffff', '#00000000', '#ff000080', '#12345678',
                   '#fff', '#f00', '#1234']) {
    const c = tinycolor(hex);
    add('parse_hex', {hex}, {rgb: c.toRgb(), hex8: c.toHex8String()});
}

// rgba 对象输入（parseColor 数组分支、campfire tint 等）
for (const obj of [{r: 255, g: 128, b: 0, a: 1}, {r: 0, g: 0, b: 0, a: 0},
                   {r: 12.5, g: 200.7, b: 255, a: 0.5}, {r: 255.4, g: 0.2, b: 77.9, a: 0.999},
                   {r: 51, g: 230, b: 128, a: 0.75}]) {
    const c = tinycolor(obj);
    add('parse_object', {obj}, {rgb: c.toRgb(), hex8: c.toHex8String()});
}

// mix（粒子 gradient 插值）
for (const [c1, c2, amount] of [['#ff0000ff', '#0000ffff', 50], ['#ff0000ff', '#0000ffff', 0],
                                ['#ff0000ff', '#0000ffff', 100], ['#80ff00c0', '#0080ff40', 33.333],
                                ['#ffffffff', '#00000000', 66.7]]) {
    const mixed = tinycolor.mix(tinycolor(c1), tinycolor(c2), amount);
    add('mix', {c1, c2, amount}, {rgb: mixed.toRgb(), hex8: mixed.toHex8String()});
}

const here = path.dirname(fileURLToPath(import.meta.url));
const target = path.resolve(here, '../../src/test/resources/wintersky/tinycolor_cases.json');
fs.mkdirSync(path.dirname(target), {recursive: true});
fs.writeFileSync(target, JSON.stringify({cases}, null, 1));
console.log('wrote', cases.length, 'cases ->', target);
