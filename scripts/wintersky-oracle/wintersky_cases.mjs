// wintersky 根包（Scene/Config/Emitter/Particle）移植版差分 golden 生成器。
// 用法：node scripts/wintersky-oracle/wintersky_cases.mjs
// 输出：src/test/resources/wintersky/wintersky_cases.json（冻结进仓库，测试运行不依赖 Node）
import { createRequire } from 'module';
import { writeFileSync, readFileSync, mkdirSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const here = dirname(fileURLToPath(import.meta.url));
const require = createRequire(join(here, '../../build/_oracle/node_modules/resolve-anchor.js'));
const Wintersky = require('wintersky');
const THREE = require('three');

// 浏览器 Image 接缝：Config 构造时 new Image()，src 赋值触发 onload
globalThis.Image = class {
    set src(v) {
        this._src = v;
        if (this.onload) this.onload();
    }
    get src() {
        return this._src;
    }
};

function mulberry32(a) {
    return function () {
        a |= 0;
        a = (a + 0x6D2B79F5) | 0;
        let t = Math.imul(a ^ (a >>> 15), 1 | a);
        t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
        return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
}

const fixturesDir = join(here, '../../src/test/resources/wintersky/fixtures');
const loadFixture = name => JSON.parse(readFileSync(join(fixturesDir, name + '.json'), 'utf8'));

const CHILD_EFFECTS = {
    'minecraft:basic_smoke_particle': loadFixture('basic_smoke'),
    'minecraft:basic_flame_particle': loadFixture('basic_flame'),
};

const FIXTURES = [
    'basic_flame',
    'basic_smoke',
    'lava_particle',
    'evoker_spell',
    'ominous_spawning_particle',
    'mobflame',
    'white_smoke',
    'portal_north_south',
    'basic_crit',
    'enchanting_table_particle',
    'dripstone_lava_drip',
    'zz_branches',
];

const TICKS = 45;

const ser = v => {
    if (typeof v !== 'number') return v;
    if (Number.isNaN(v)) return 'NaN';
    if (v === Infinity) return 'Infinity';
    if (v === -Infinity) return '-Infinity';
    return v;
};
const serVec = v => [ser(v.x), ser(v.y), ser(v.z)];

function dumpParticle(p) {
    const clr = p.geometry.getAttribute('clr').array;
    const uv = p.geometry.getAttribute('uv').array;
    return {
        pos: serVec(p.position),
        speed: serVec(p.speed),
        facing: serVec(p.facing_direction),
        rot: ser(p.rotation),
        age: ser(p.age),
        lifetime: ser(p.lifetime),
        scale: [ser(p.mesh.scale.x), ser(p.mesh.scale.y)],
        clr: [ser(clr[0]), ser(clr[1]), ser(clr[2]), ser(clr[3])],
        uv: Array.from(uv).map(ser),
        meshRot: [ser(p.mesh.rotation.x), ser(p.mesh.rotation.y), ser(p.mesh.rotation.z)],
        order: p.mesh.rotation.order,
    };
}

function runCase(name, fixtureName, ops) {
    Math.random = mulberry32(42);

    const scene = new Wintersky.Scene({
        fetchParticleFile: identifier => {
            const json = CHILD_EFFECTS[identifier];
            return json ? { json } : null;
        },
    });
    const camera = new THREE.Object3D();
    camera.position.set(3, 4, 5);
    camera.rotation.set(0.3, -0.7, 0.1);

    const emitter = new Wintersky.Emitter(scene, loadFixture(fixtureName));

    let tickEvents = [];
    emitter.on('end', () => tickEvents.push('end'));
    emitter.on('event', d => tickEvents.push('event:' + d.event_id));
    emitter.on('play_sound', d => tickEvents.push('sound:' + (d.sound_effect && d.sound_effect.event_name)));
    emitter.on('play_child_particle', d => tickEvents.push('child:' + (d.config && d.config.identifier)));
    emitter.on('start', () => tickEvents.push('start'));
    emitter.on('stop', () => tickEvents.push('stop'));

    emitter.start();

    const ticks = [];
    const dumpTick = () => {
        const out = {
            age: ser(emitter.age),
            view_age: ser(emitter.view_age),
            enabled: emitter.enabled,
            active_time: ser(emitter.active_time),
            sleep_time: ser(emitter.sleep_time),
            random_vars: emitter.random_vars.map(ser),
            np: emitter.particles.length,
            dead: emitter.dead_particles.length,
            nce: emitter.child_emitters.length,
            ncp: emitter.child_emitters.reduce((s, e) => s + e.particles.length, 0),
            events: tickEvents,
            particles: emitter.particles.map(dumpParticle),
        };
        tickEvents = [];
        ticks.push(out);
    };

    for (const op of ops) {
        if (op[0] === 'tick') {
            for (let i = 0; i < op[1]; i++) {
                emitter.tick();
                scene.updateFacingRotation(camera);
                dumpTick();
            }
        } else if (op[0] === 'jumpTo') {
            emitter.jumpTo(op[1]);
            scene.updateFacingRotation(camera);
            tickEvents = [];
        }
    }
    return { name, ticks };
}

const cases = [];
for (const fixture of FIXTURES) {
    cases.push(runCase(fixture, fixture, [['tick', TICKS]]));
}
cases.push(runCase('jump_basic_smoke', 'basic_smoke', [['tick', 10], ['jumpTo', 1.0], ['tick', 5]]));
cases.push(runCase('jump_back_basic_smoke', 'basic_smoke', [['tick', 40], ['jumpTo', 0.2], ['tick', 5]]));

const outDir = join(here, '../../src/test/resources/wintersky');
mkdirSync(outDir, { recursive: true });
writeFileSync(join(outDir, 'wintersky_cases.json'), JSON.stringify(cases));
console.log('cases:', cases.length);
for (const c of cases) {
    const last = c.ticks[c.ticks.length - 1];
    console.log(c.name.padEnd(28), 'ticks=' + c.ticks.length, 'np=' + last.np, 'nce=' + last.nce, 'ncp=' + last.ncp);
}
