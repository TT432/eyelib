// Molang 移植版差分测试用例生成器（Node golden oracle）。
// 用法：node scripts/wintersky-oracle/molang_cases.mjs
// 输出：src/test/resources/wintersky/molang_cases.json（冻结进仓库，测试运行不依赖 Node）
import { createRequire } from 'module';
import { writeFileSync, mkdirSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const here = dirname(fileURLToPath(import.meta.url));
const require = createRequire(join(here, '../../build/_oracle/node_modules/resolve-anchor.js'));
const MolangModule = require('molangjs');
const Molang = MolangModule.default || MolangModule;

function mulberry32(a) {
    return function () {
        a |= 0;
        a = (a + 0x6D2B79F5) | 0;
        let t = Math.imul(a ^ (a >>> 15), 1 | a);
        t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
        return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
}

// handler 夹具：
//  none   —— 无 variableHandler
//  values —— variable.custom=4，query.add(a,b)=a+b，其余未定义
//  ctxfn  —— global_variables['query.double'] = x => x*2
const cases = [
    { name: 'precedence_mul_add', exprs: ['1+2*3'] },
    { name: 'parens', exprs: ['(1+2)*3'] },
    { name: 'left_assoc_minus', exprs: ['10-4-3'] },
    { name: 'mul_negative', exprs: ['2*-3'] },
    { name: 'unary_minus', exprs: ['-5'] },
    { name: 'minus_minus', exprs: ['1--2'] },
    { name: 'division', exprs: ['3/2', '7/2'] },
    { name: 'div_by_zero', exprs: ['1/0'] },
    { name: 'zero_div_zero', exprs: ['0/0'] },
    { name: 'negative_mod', exprs: ['-3%2'] },
    { name: 'negator', exprs: ['!0', '!5', '!!3'] },
    { name: 'and_chain', exprs: ['2>1&&3>2', '2>1&&0'] },
    { name: 'or_chain', exprs: ['0||5', '0||0'] },
    { name: 'ternary', exprs: ['1?2:3', '0?2:3', '0?2', '5?'] },
    { name: 'compare_eq_chain', exprs: ['1<2==1', '2!=2', '3>=3', '3<=2'] },
    { name: 'null_coalesce_unassigned', exprs: ['q.x ?? 5'] },
    { name: 'null_coalesce_assigned', exprs: ['v.a=7;v.a ?? 5'] },
    { name: 'math_abs', exprs: ['math.abs(-3)'] },
    { name: 'math_sin_deg', exprs: ['math.sin(90)', 'math.sin(30)'] },
    { name: 'math_cos_deg', exprs: ['math.cos(180)'] },
    { name: 'math_sin_rad', useRadians: true, exprs: ['math.sin(1.5707963267948966)'] },
    { name: 'math_sqrt_pow', exprs: ['math.sqrt(2)', 'math.pow(2,10)'] },
    { name: 'math_exp_ln', exprs: ['math.exp(1)', 'math.ln(10)'] },
    { name: 'math_ceil_round', exprs: ['math.ceil(2.1)', 'math.round(2.5)', 'math.round(-2.5)'] },
    { name: 'math_trunc_floor', exprs: ['math.trunc(-2.7)', 'math.floor(-2.1)'] },
    { name: 'math_mod', exprs: ['math.mod(-7,3)', 'math.mod(7,3)'] },
    { name: 'math_min_max', exprs: ['math.min(2,3)', 'math.max(2,3)'] },
    { name: 'math_clamp', exprs: ['math.clamp(5,1,3)', 'math.clamp(-5,1,3)'] },
    { name: 'math_lerp', exprs: ['math.lerp(0,10,0.25)'] },
    { name: 'math_lerprotate', exprs: ['math.lerprotate(350,10,0.5)', 'math.lerprotate(10,50,0.5)'] },
    { name: 'math_inverse_trig', exprs: ['math.asin(1)', 'math.acos(-1)', 'math.atan(1)', 'math.atan2(1,1)'] },
    { name: 'math_hermite', exprs: ['math.hermite_blend(0.5)', 'math.hermite_blend(0.25)'] },
    { name: 'math_min_angle', exprs: ['math.min_angle(190)', 'math.min_angle(-190)'] },
    { name: 'math_pi', exprs: ['math.pi'] },
    { name: 'scope_loop_alloc', exprs: ['t.a=0;loop(4,{t.a=t.a+1});t.a'] },
    { name: 'loop_break', exprs: ['t.a=0;loop(10,{t.a=t.a+1;t.a>2?break});t.a'] },
    { name: 'loop_continue', exprs: ['t.a=0;t.b=0;loop(5,{t.a=t.a+1;t.a==2?continue;t.b=t.b+10});t.b'] },
    { name: 'return_statement', exprs: ['t.a=1;return t.a+1;t.a=5'] },
    { name: 'query_in_range', exprs: ['query.in_range(5,1,10)', 'query.in_range(15,1,10)'] },
    { name: 'query_all', exprs: ['query.all(2,2,2)', 'query.all(2,2,3)', 'query.all(2)'] },
    { name: 'query_any', exprs: ['query.any(2,1,2,3)', 'query.any(2,1,3)'] },
    { name: 'query_approx_eq', exprs: ['query.approx_eq(0.1+0.2,0.3)', 'query.approx_eq(1,2)'] },
    { name: 'query_quoted_string_arg', exprs: ["query.any(1,'a',1)", "query.all('a','a','a')"] },
    { name: 'string_literal_allocation', exprs: ["v.x = 'abc'"] },
    { name: 'handler_query_add', handler: 'values', exprs: ['query.add(2,3)'] },
    { name: 'handler_variable', handler: 'values', exprs: ['variable.custom*2'] },
    { name: 'ctx_query_function', handler: 'ctxfn', exprs: ['query.double(21)'] },
    { name: 'temp_variable_persist', exprs: ['t.a=5;variable.b=t.a*2;variable.b'] },
    { name: 'variable_sequence', exprs: ['v.count = v.count + 1', 'v.count = v.count + 1', 'v.count'] },
    { name: 'nested_brackets', exprs: ['((1))', '{2}', 'math.clamp(math.sin(30)*10,0,10)'] },
    { name: 'unassigned_variable', exprs: ['v.a'] },
    { name: 'seeded_random', seed: 42, exprs: ['math.random(0,1)', 'math.random(5,5)', 'math.random_integer(1,10)'] },
    { name: 'seeded_die_roll', seed: 7, exprs: ['math.die_roll(3,1,6)', 'math.die_roll_integer(2,1,6)'] },
    { name: 'seeded_loop_random', seed: 123, exprs: ['t.s=0;loop(5,{t.s=t.s+math.random(0,1)});t.s'] },
    { name: 'number_fast_path', exprs: ['42', '-17', '3.25'] },
];

const realRandom = Math.random;
const out = [];
for (const c of cases) {
    const m = new Molang();
    if (c.useRadians) m.use_radians = true;
    if (c.handler === 'values') {
        m.variableHandler = (name, ctx, args) => {
            if (name === 'query.add') return args[0] + args[1];
            if (name === 'variable.custom') return 4;
            return undefined;
        };
    } else if (c.handler === 'ctxfn') {
        m.global_variables['query.double'] = (x) => x * 2;
    }
    if (c.seed !== undefined) Math.random = mulberry32(c.seed);
    const expected = [];
    try {
        for (const expr of c.exprs) {
            const v = m.parse(expr);
            if (typeof v === 'number' && !Number.isFinite(v)) {
                expected.push({ special: Number.isNaN(v) ? 'NaN' : v > 0 ? 'Infinity' : '-Infinity' });
            } else if (typeof v === 'number' && v === 0 && 1 / v === -Infinity) {
                expected.push({ special: '-0' });
            } else {
                expected.push(v);
            }
        }
    } finally {
        Math.random = realRandom;
    }
    out.push({
        name: c.name,
        seed: c.seed ?? null,
        useRadians: !!c.useRadians,
        handler: c.handler ?? 'none',
        exprs: c.exprs,
        expected,
    });
}

const dest = join(here, '../../src/test/resources/wintersky');
mkdirSync(dest, { recursive: true });
writeFileSync(join(dest, 'molang_cases.json'), JSON.stringify(out, null, 2));
console.log(`wrote ${out.length} cases`);
