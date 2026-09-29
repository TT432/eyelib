// Snowstorm Node oracle — resolve/load customization hooks（不改动 Snowstorm 源）。
// 映射表（NOTES.md §Stub 方案的落地，差异见文件内注释）：
//   vue                        → stubs/vue.mjs（curves.js 仅用 Vue.nextTick）
//   */components/ExpressionBar → stubs/expression_bar.mjs（ExpandedInput.setup=false 短路 UI 路径）
//   */components/Preview       → stubs/preview.mjs（View 对象字段 stub）
//   wintersky / three / molangjs → build/_oracle/node_modules 的 ESM 构建（真身）
//   tinycolor2                 → build/_oracle CJS 真身（Node CJS-ESM 互操作）
//   ** /examples/*.json        → load hook 直接 JSON.parse 转 default export
//   ./vscode_extension         → 不映射：真身 `typeof acquireVsCodeApi == 'function'` 在 Node 为
//                                false，export default false，零偏差（与 NOTES.md 的差异点）
import { createRequire } from 'node:module';
import { pathToFileURL, fileURLToPath } from 'node:url';
import { readFile } from 'node:fs/promises';

const STUBS = new URL('./stubs/', import.meta.url);
const oracleRequire = createRequire(new URL('../../build/_oracle/package.json', import.meta.url));

// 裸包名 → ESM 构建绝对路径（wintersky.esm.js 内部再 import 的 bare specifier 回本 hook）
const ESM_BUILDS = {
	wintersky: '../../build/_oracle/node_modules/wintersky/dist/wintersky.esm.js',
	three: '../../build/_oracle/node_modules/three/build/three.module.js',
	molangjs: '../../build/_oracle/node_modules/molangjs/dist/molang.esm.js',
};
const CJS_BARE = new Set(['tinycolor2']);

export async function resolve(specifier, context, nextResolve) {
	if (specifier === 'vue') {
		return { url: new URL('vue.mjs', STUBS).href, shortCircuit: true };
	}
	if (specifier.endsWith('/components/ExpressionBar') || specifier.endsWith('/components/ExpressionBar.vue')) {
		return { url: new URL('expression_bar.mjs', STUBS).href, shortCircuit: true };
	}
	if (specifier.endsWith('/components/Preview') || specifier.endsWith('/components/Preview.vue')) {
		return { url: new URL('preview.mjs', STUBS).href, shortCircuit: true };
	}
	if (Object.hasOwn(ESM_BUILDS, specifier)) {
		return { url: new URL(ESM_BUILDS[specifier], import.meta.url).href, shortCircuit: true };
	}
	if (CJS_BARE.has(specifier)) {
		return { url: pathToFileURL(oracleRequire.resolve(specifier)).href, shortCircuit: true };
	}
	// webpack 风格无扩展名相对导入（'./input'）→ 补 '.js'
	try {
		return await nextResolve(specifier, context);
	} catch (err) {
		if (err && err.code === 'ERR_MODULE_NOT_FOUND' && (specifier.startsWith('./') || specifier.startsWith('../'))) {
			return await nextResolve(specifier + '.js', context);
		}
		throw err;
	}
}

export async function load(url, context, nextLoad) {
	// webpack JSON import（import.js 的 ../examples/*.particle.json）→ default export
	if (url.endsWith('.json') && url.includes('/examples/')) {
		const raw = await readFile(fileURLToPath(url), 'utf8');
		return {
			format: 'module',
			source: `export default ${JSON.stringify(JSON.parse(raw))};`,
			shortCircuit: true,
		};
	}
	// Snowstorm 源无 package.json type 字段：直接声明 module，跳过 reparse 警告
	if (url.endsWith('.js') && url.includes('/_snowstorm_src/')) {
		return { format: 'module', source: await readFile(fileURLToPath(url), 'utf8'), shortCircuit: true };
	}
	return nextLoad(url, context);
}
