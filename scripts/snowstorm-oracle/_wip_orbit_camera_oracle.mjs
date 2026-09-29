// OrbitCamera（three r134 OrbitControls 数学移植）wip oracle：真 three OrbitControls 跑
// 固定操作序列，dump 每步相机 position/quaternion/target。
// 运行（仓库根）：node scripts/snowstorm-oracle/_wip_orbit_camera_oracle.mjs
// 输出：build/_orbit_oracle_dump.json（Java 侧 throwaway smoke 逐值比对，golden 不冻结，P4 统一处理）
import * as THREE from '../../build/_oracle/node_modules/three/build/three.module.js';
import {OrbitControls} from '../../build/_oracle/node_modules/three/examples/jsm/controls/OrbitControls.js';
import fs from 'fs';
// OrbitControls 构造器仅做 domElement === document 比较，给全局 document 一个哨兵
globalThis.document = {};


const CLIENT_HEIGHT = 600;

// 最小 domElement stub：捕获事件监听 + clientHeight/clientWidth
function makeDom() {
    const listeners = {};
    return {
        listeners,
        style: {},
        clientHeight: CLIENT_HEIGHT,
        clientWidth: 800,
        addEventListener(t, fn) { (listeners[t] ??= []).push(fn); },
        removeEventListener(t, fn) {
            const a = listeners[t];
            if (a) { const i = a.indexOf(fn); if (i >= 0) a.splice(i, 1); }
        },
        setPointerCapture() {}, releasePointerCapture() {},
    };
}
function fire(dom, type, ev) {
    for (const fn of [...(dom.listeners[type] || [])]) fn(ev);
}

// Snowstorm Preview.vue 参数
const camera = new THREE.PerspectiveCamera(45, 16 / 9, 0.1, 3000);
camera.position.set(-6, 3, -6);
const dom = makeDom();
const controls = new OrbitControls(camera, dom);
controls.target.set(0, 0.8, 0);
controls.screenSpacePanning = true;
controls.zoomSpeed = 1.4;

const steps = [];
function dump(op) {
    camera.updateMatrix(); // 模拟 renderer 每帧 updateMatrix（pan 读取 object.matrix 的时序）
    steps.push({
        op,
        position: camera.position.toArray(),
        quaternion: camera.quaternion.toArray(),
        target: controls.target.toArray(),
    });
}

let pointerId = 1;
function pointerDown(button, x, y) {
    fire(dom, 'pointerdown', {pointerId, pointerType: 'mouse', button, clientX: x, clientY: y});
}
function pointerMove(x, y) {
    fire(dom, 'pointermove', {pointerId, pointerType: 'mouse', clientX: x, clientY: y});
}
function pointerUp(x, y) {
    fire(dom, 'pointerup', {pointerId, pointerType: 'mouse', clientX: x, clientY: y});
    pointerId++;
}
function wheel(deltaY) {
    fire(dom, 'wheel', {deltaY, preventDefault() {}});
}

// ============================== 操作序列（Java smoke 逐步镜像） ==============================
controls.update();
dump('update');

// 左拖旋转：两段移动 → Java: rotate(30,-20,600) / rotate(30,40,600)
pointerDown(0, 400, 300);
pointerMove(430, 280);
dump('rotate(30,-20)');
pointerMove(460, 320);
dump('rotate(30,40)');
pointerUp(460, 320);

// 滚轮：in / out / in → Java: dolly(-120) / dolly(120) / dolly(-120)
wheel(-120);
dump('dolly(-120)');
wheel(120);
dump('dolly(120)');
wheel(-120);
dump('dolly(-120)#2');

// 右拖平移 → Java: pan(-20,10,600)
pointerDown(2, 400, 300);
pointerMove(380, 310);
dump('pan(-20,10)');
pointerUp(380, 310);

// 组合：再旋转 → Java: rotate(-10,30,600)
pointerDown(0, 100, 100);
pointerMove(90, 130);
dump('rotate(-10,30)');
pointerUp(90, 130);

fs.mkdirSync('build', {recursive: true});
fs.writeFileSync('build/_orbit_oracle_dump.json', JSON.stringify(steps, null, 2));
console.log('wrote build/_orbit_oracle_dump.json (' + steps.length + ' steps)');
for (const s of steps) {
    console.log(s.op.padEnd(16), s.position.map(v => v.toPrecision(17)).join(', '));
}
