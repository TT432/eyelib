// vue stub：实证（NOTES.md）curves.js 仅用 Vue.nextTick，回调内是 Curve.vue 的 updateSVG
// UI 刷新，模型层无数据副作用 → 收为 no-op 队列（不执行回调，记录偏离）。
const pending = [];
export function nextTick(cb) {
	if (typeof cb === 'function') pending.push(cb);
	return Promise.resolve();
}
export default { nextTick };
