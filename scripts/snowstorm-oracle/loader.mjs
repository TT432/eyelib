// Snowstorm Node oracle — 全局 stub + module customization hooks 注册。
// 用法：node --import ./scripts/snowstorm-oracle/loader.mjs scripts/snowstorm-oracle/run_cases.mjs
// 必须在任何 Snowstorm 模块加载前执行（--import 保证）。
import { register } from 'node:module';

// ---------------------------------------------------------------- 确定性 Math.random
// 偏离记录：替换浏览器 Math.random 为 mulberry32（seed 固定），golden 跨运行可复现。
// Java 对拍侧不应比对随机产物（bbuid/guid 生成的 id），golden 中已归一化剔除。
export const ORACLE_SEED = 0x5EED1234;
let rng_state = ORACLE_SEED;
Math.random = function () {
	rng_state |= 0;
	rng_state = (rng_state + 0x6D2B79F5) | 0;
	let t = Math.imul(rng_state ^ (rng_state >>> 15), 1 | rng_state);
	t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
	return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
};

// ---------------------------------------------------------------- window / localStorage
// options.js 实证同时使用 window.localStorage 与裸 localStorage，两者都 stub（内存 Map）。
const storage = new Map();
const localStorageStub = {
	getItem: (k) => (storage.has(k) ? storage.get(k) : null),
	setItem: (k, v) => void storage.set(k, String(v)),
	removeItem: (k) => void storage.delete(k),
	clear: () => storage.clear(),
};
globalThis.window = globalThis;
globalThis.localStorage = localStorageStub;
window.scrollX = 0; // util.js getBoundingClientRect 助手（UI-only）
window.scrollY = 0;

// DOM 事件（edits.js / texture_edit.js 顶层 window.addEventListener('message'|'keydown')）
window.addEventListener = () => {};
window.removeEventListener = () => {};

// 事件构造器：edits.js registerEdit 的 `event instanceof InputEvent/KeyboardEvent` 需要
globalThis.InputEvent = class InputEvent {};
globalThis.KeyboardEvent = class KeyboardEvent {};

globalThis.requestAnimationFrame = () => 0;
globalThis.cancelAnimationFrame = () => {};

// Image：texture_edit.js updateCanvasFromSource 运行期使用；oracle 无 PNG 后端，直接 onerror
globalThis.Image = class Image {
	set src(_v) {
		if (this.onerror) queueMicrotask(() => this.onerror(new Error('oracle: no image backend')));
	}
};

// ---------------------------------------------------------------- document
// texture_edit.js 顶层 document.createElement('canvas') + document.addEventListener。
// canvas 2D 在 Node 无实现（NOTES.md）：fake ctx 仅保证 TextureClass 可构造；
// 像素级 texture oracle（后续 wave）需在此处换记录型 proxy。
function makeFakeCtx(canvas) {
	return {
		canvas,
		getImageData(x, y, w, h) {
			return { data: new Uint8ClampedArray(w * h * 4), width: w, height: h };
		},
		putImageData() {},
		clearRect() {},
		drawImage() {},
	};
}
function makeFakeCanvas() {
	const canvas = {
		width: 300,
		height: 150,
		getContext() {
			return makeFakeCtx(canvas);
		},
		toDataURL() {
			return `data:image/png;base64,oracle_fake(${canvas.width}x${canvas.height})`;
		},
	};
	return canvas;
}
function makeFakeElement() {
	return { style: {}, href: '', download: '', type: '', accept: '', multiple: false, files: [], click() {} };
}
globalThis.document = {
	createElement(tag) {
		if (tag === 'canvas') return makeFakeCanvas();
		return makeFakeElement();
	},
	addEventListener() {},
	removeEventListener() {},
	querySelector() {
		return null;
	},
	body: { appendChild() {}, removeChild() {} },
};

// ---------------------------------------------------------------- module hooks
register('./hooks.mjs', import.meta.url);
