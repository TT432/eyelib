// Snowstorm Node oracle — 用例运行器（wave-3 首个用例集：util/options/gradient）。
// 运行：node --import ./scripts/snowstorm-oracle/loader.mjs scripts/snowstorm-oracle/run_cases.mjs
// 输出：stdout 打印 golden JSON，同时冻结到 src/test/resources/snowstorm/snowstorm_cases.json。
import { writeFile, mkdir } from 'node:fs/promises';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { dirname, join } from 'node:path';
const HERE = dirname(fileURLToPath(import.meta.url));
const REPO = join(HERE, '..', '..');
const SRC = (f) => pathToFileURL(join(REPO, 'build', '_snowstorm_src', 'src', f)).href;
const GOLDEN = join(REPO, 'src', 'test', 'resources', 'snowstorm', 'snowstorm_cases.json');

const cases = [];
function record(name, input, output) {
	cases.push({ name, input, output });
}

// gradient 点的 id 为 bbuid(8) 随机产物：golden 归一化剔除（Java 侧 WinterskyRandom 流不同，
// 对拍只比行为不比随机 id）。
function stripIds(points) {
	return points.map(({ percent, color }) => ({ percent, color }));
}

// ---------------------------------------------------------------- util.js
const util = await import(SRC('util.js'));

const compileInput = {
	identifier: 'test:demo',
	num: 0.123456789,
	rounded: 1.000000001,
	str: 'line\n"quoted"\ttab\\back',
	bool: true,
	nil: null,
	inf: Infinity,
	arr: [1, 'two', [3], { nested: false }],
	empty_arr: [],
	empty_obj: {},
};
record('util.compileJSON.default', compileInput, util.compileJSON(compileInput));
record('util.compileJSON.small', compileInput, util.compileJSON(compileInput, { small: true }));

record('util.pathToName.strip_ext', 'textures/particle/snowflake.png', util.pathToName('textures/particle/snowflake.png'));
record('util.pathToName.keep_ext', ['textures/particle/snowflake.png', true], util.pathToName('textures/particle/snowflake.png', true));
record('util.pathToName.backslash', 'foo\\bar\\baz.qux', util.pathToName('foo\\bar\\baz.qux'));
record('util.pathToName.no_ext', 'a/b/plain', util.pathToName('a/b/plain'));

record('util.trimFloatNumber.long', 0.123456789, util.trimFloatNumber(0.123456789));
record('util.trimFloatNumber.int', 3, util.trimFloatNumber(3));
record('util.trimFloatNumber.half', 2.5, util.trimFloatNumber(2.5));
record('util.trimFloatNumber.neg_zero_quirk', -0.00001, util.trimFloatNumber(-0.00001));
record('util.trimFloatNumber.empty', '', util.trimFloatNumber(''));
record('util.trimFloatNumber.digits', [0.987654321, 2], util.trimFloatNumber(0.987654321, 2));

// Math.snapToValues 是 Math 扩展（非 export）：import util.js 后即可用
record('util.snapToValues.no_snap', [50, [0, 100], 12], Math.snapToValues(50, [0, 100], 12));
record('util.snapToValues.snap_low', [11, [0, 100], 12], Math.snapToValues(11, [0, 100], 12));
record('util.snapToValues.snap_high', [95, [0, 100], 12], Math.snapToValues(95, [0, 100], 12));
record('util.snapToValues.exact_epsilon_edge', [12, [0, 100], 12], Math.snapToValues(12, [0, 100], 12));

// lineify：import.js updateInputsFromConfig 的闭包局部函数，未导出，无法不改源直接调用；
// 由 wave-2 import 用例经 updateInputsFromConfig 覆盖（golden meta.notes 记录）。

// ---------------------------------------------------------------- options.js
{
	const options = await import(SRC('options.js'));
	record('options.defaults', null, { ...options.OptionValues });

	options.setOption('grid_visible', false);
	options.setOption('minecraft_block_visible', true);
	record('options.after_setOption', ['grid_visible=false', 'minecraft_block_visible=true'], {
		values: { ...options.OptionValues },
		stored: localStorage.getItem('snowstorm.options'),
	});
}
{
	// 预填 localStorage 后用 ?query 重新实例化模块，验证启动加载分支
	localStorage.setItem('snowstorm.options', JSON.stringify({ grid_visible: false, axis_helper_visible: false, unknown_key: 123 }));
	const options = await import(SRC('options.js') + '?stored=1');
	record('options.load_stored', '{"grid_visible":false,"axis_helper_visible":false,"unknown_key":123}', { ...options.OptionValues });
	localStorage.removeItem('snowstorm.options');
}

// ---------------------------------------------------------------- gradient.js
{
	// 循环 import 求值顺序（实证）：gradient.js 直接作入口会 TDZ 崩溃
	// （gradient→input→edits→export→input_structure 时 input_structure 的 body 在 Input 类
	// 初始化前执行 new Input）。经 input_structure.js 入口则 input.js body 先完成
	// （环上依赖遇 in-progress 即跳过），与 webpack 实际加载顺序一致。
	await import(SRC('input_structure.js'));
	const { default: Gradient } = await import(SRC('gradient.js'));
	const g = new Gradient({ type: 'gradient', value: [] });
	record('gradient.init', '{type:"gradient",value:[]}', {
		value: stripIds(g.value),
		selected: { percent: g.selected.percent, color: g.selected.color },
	});

	g.addPoint();
	record('gradient.after_addPoint', null, {
		value: stripIds(g.value),
		selected: { percent: g.selected.percent, color: g.selected.color },
	});

	// change 的 node 参数仅用于 :active 滑动检测（DOM），传 null 走非滑动分支
	g.change({ hex8: '#ff000080' }, null, true);
	record('gradient.after_change', '{hex8:"#ff000080"}', {
		value: stripIds(g.value),
		selected: { percent: g.selected.percent, color: g.selected.color },
	});

	record('gradient.export_range1', 1, g.export(1));
	record('gradient.export_range2_5', 2.5, g.export(2.5));

	g.removePoint();
	record('gradient.after_removePoint', null, {
		value: stripIds(g.value),
		selected: { percent: g.selected.percent, color: g.selected.color },
	});

	g.reset();
	record('gradient.after_reset', null, {
		value: stripIds(g.value),
		selected: { percent: g.selected.percent, color: g.selected.color },
	});
}

// ---------------------------------------------------------------- golden 输出
const golden = {
	meta: {
		tool: 'scripts/snowstorm-oracle/run_cases.mjs',
		snowstorm_version: 'v3.2.2',
		node: process.version,
		math_random_seed: '0x5EED1234 (mulberry32，替换浏览器 Math.random 保证可复现)',
		notes: [
			'lineify 为 import.js updateInputsFromConfig 闭包局部函数，未导出，本集不含；wave-2 import 用例覆盖。',
			'gradient 点 id（bbuid 随机）已归一化剔除，Java 对拍不比随机产物。',
		],
	},
	cases,
};
const json = JSON.stringify(golden, null, 2) + '\n';
await mkdir(dirname(GOLDEN), { recursive: true });
await writeFile(GOLDEN, json);
console.log(json);
console.error(`[oracle] ${cases.length} cases → ${GOLDEN}`);
