// Preview.vue stub：emitter.js 顶层触达 View.scene.add(Scene.space)；其余字段按
// Preview.vue 的 View 对象（96-200 行）shape 预置，供后续 wave 的运行期路径使用。
export const View = {
	scene: { add() {}, remove() {} },
	camera: {
		position: { toArray: () => [-6, 3, -6], length: () => 8.124, set() {} },
	},
	controls: { target: { toArray: () => [0, 0.8, 0], set() {} } },
	renderer: { setClearColor() {}, render() {} },
	canvas: null,
	placeholder_variables: {},
	PlaybackController: {
		start() { return this; },
		stop() { return this; },
		toggle() {},
	},
	screenshot() { return 'data:image/png;base64,oracle_fake'; },
};
export default {};
