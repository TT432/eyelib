// three.js r134 golden oracle 生成器：为 Java 移植层生成冻结用例。
// 运行：node scripts/wintersky-oracle/three_cases.mjs（需在 build/_oracle 下运行）
// 输出：src/test/resources/wintersky/three_cases.json
import * as THREE from '../../build/_oracle/node_modules/three/build/three.module.js';
import {fileURLToPath} from 'url';
import path from 'path';
import fs from 'fs';

const cases = [];
const add = (kind, input, expected) => cases.push({kind, input, expected});

// ---- Vector3 ----
{
    const v = new THREE.Vector3(1.5, -2.25, 3.75);
    const q = new THREE.Quaternion(0.18257418583505536, 0.3651483716701107, 0.5477225575051661, 0.7302967433402214);
    const r = v.clone().applyQuaternion(q);
    add('vec3_apply_quat', {v: [1.5, -2.25, 3.75], q: [0.18257418583505536, 0.3651483716701107, 0.5477225575051661, 0.7302967433402214]}, r.toArray());

    const e = new THREE.Euler(0.3, -1.2, 2.1, 'XYZ');
    add('vec3_apply_euler_xyz', {v: [1.5, -2.25, 3.75], e: [0.3, -1.2, 2.1, 'XYZ']}, v.clone().applyEuler(e).toArray());
    const e2 = new THREE.Euler(0.3, -1.2, 2.1, 'YXZ');
    add('vec3_apply_euler_yxz', {v: [1.5, -2.25, 3.75], e: [0.3, -1.2, 2.1, 'YXZ']}, v.clone().applyEuler(e2).toArray());

    add('vec3_reflect', {v: [1, -2, 0.5], n: [0, 1, 0]}, new THREE.Vector3(1, -2, 0.5).reflect(new THREE.Vector3(0, 1, 0)).toArray());
    add('vec3_reflect_tilted', {v: [3, -1, 2], n: [0.2672612419124244, 0.5345224838248488, 0.8017837257372732]},
        new THREE.Vector3(3, -1, 2).reflect(new THREE.Vector3(0.2672612419124244, 0.5345224838248488, 0.8017837257372732)).toArray());
    add('vec3_add_scaled', {a: [1, 2, 3], b: [4, -5, 6], s: -0.5}, new THREE.Vector3(1, 2, 3).addScaledVector(new THREE.Vector3(4, -5, 6), -0.5).toArray());
    add('vec3_normalize_zero', {v: [0, 0, 0]}, new THREE.Vector3(0, 0, 0).normalize().toArray());
    add('vec3_cross', {a: [1, 0.5, -2], b: [-3, 2, 0.25]}, new THREE.Vector3(1, 0.5, -2).cross(new THREE.Vector3(-3, 2, 0.25)).toArray());
}

// ---- Quaternion ----
for (const order of ['XYZ', 'YXZ', 'ZXY', 'ZYX', 'YZX', 'XZY']) {
    const q = new THREE.Quaternion().setFromEuler(new THREE.Euler(0.4, -0.9, 2.3, order));
    add('quat_from_euler', {e: [0.4, -0.9, 2.3], order}, q.toArray());
}
{
    add('quat_from_unit_vectors', {from: [0, 1, 0], to: [0.5773502691896258, 0.5773502691896258, 0.5773502691896258]},
        new THREE.Quaternion().setFromUnitVectors(new THREE.Vector3(0, 1, 0), new THREE.Vector3(0.5773502691896258, 0.5773502691896258, 0.5773502691896258)).toArray());
    // 反向向量分支（r < EPSILON）
    add('quat_from_unit_vectors_opposite', {from: [0, 1, 0], to: [0, -1, 0]},
        new THREE.Quaternion().setFromUnitVectors(new THREE.Vector3(0, 1, 0), new THREE.Vector3(0, -1, 0)).toArray());
    add('quat_from_unit_vectors_opposite_x', {from: [1, 0, 0], to: [-1, 0, 0]},
        new THREE.Quaternion().setFromUnitVectors(new THREE.Vector3(1, 0, 0), new THREE.Vector3(-1, 0, 0)).toArray());

    const a = new THREE.Quaternion().setFromEuler(new THREE.Euler(0.5, 0.25, -1, 'XYZ'));
    const b = new THREE.Quaternion().setFromEuler(new THREE.Euler(-1.5, 0.75, 0.1, 'YXZ'));
    add('quat_multiply', {a: a.toArray(), b: b.toArray()}, a.clone().multiply(b).toArray());
    add('quat_premultiply', {a: a.toArray(), b: b.toArray()}, a.clone().premultiply(b).toArray());
    add('quat_invert', {q: a.toArray()}, a.clone().invert().toArray());

    const m = new THREE.Matrix4().makeRotationFromEuler(new THREE.Euler(0.7, -0.2, 1.9, 'ZYX'));
    add('quat_from_rotation_matrix', {m: m.toArray()}, new THREE.Quaternion().setFromRotationMatrix(m).toArray());
    const m2 = new THREE.Matrix4().makeRotationFromEuler(new THREE.Euler(2.9, 0.1, -0.4, 'XYZ'));
    add('quat_from_rotation_matrix2', {m: m2.toArray()}, new THREE.Quaternion().setFromRotationMatrix(m2).toArray());
}

// ---- Euler ----
for (const order of ['XYZ', 'YXZ', 'ZXY', 'ZYX', 'YZX', 'XZY']) {
    const m = new THREE.Matrix4().makeRotationFromEuler(new THREE.Euler(0.4, -0.9, 2.3, order));
    const e = new THREE.Euler().setFromRotationMatrix(m, order);
    add('euler_from_matrix', {m: m.toArray(), order}, [e.x, e.y, e.z, e.order]);
}
{
    const e = new THREE.Euler(0.4, -0.9, 2.3, 'XYZ');
    add('euler_reorder', {e: [0.4, -0.9, 2.3, 'XYZ'], order: 'YXZ'}, (() => {
        const r = e.clone().reorder('YXZ');
        return [r.x, r.y, r.z, r.order];
    })());
    // 万向锁分支
    const g = new THREE.Euler(0.1, Math.PI / 2, 0.3, 'XYZ');
    const m = new THREE.Matrix4().makeRotationFromEuler(g);
    const back = new THREE.Euler().setFromRotationMatrix(m, 'XYZ');
    add('euler_from_matrix_gimbal', {m: m.toArray(), order: 'XYZ'}, [back.x, back.y, back.z, back.order]);
}

// ---- Matrix4 ----
{
    const pos = new THREE.Vector3(1, -2, 3);
    const quat = new THREE.Quaternion().setFromEuler(new THREE.Euler(0.3, 1.1, -0.7, 'YXZ'));
    const scl = new THREE.Vector3(2, 0.5, 3);
    const m = new THREE.Matrix4().compose(pos, quat, scl);
    add('mat4_compose', {pos: pos.toArray(), quat: quat.toArray(), scale: scl.toArray()}, m.toArray());

    const p = new THREE.Vector3(), q = new THREE.Quaternion(), s = new THREE.Vector3();
    m.decompose(p, q, s);
    add('mat4_decompose', {m: m.toArray()}, {pos: p.toArray(), quat: q.toArray(), scale: s.toArray()});

    const a = new THREE.Matrix4().compose(new THREE.Vector3(1, 2, 3),
        new THREE.Quaternion().setFromEuler(new THREE.Euler(0.2, 0.3, 0.4)), new THREE.Vector3(1, 2, 1));
    const b = new THREE.Matrix4().compose(new THREE.Vector3(-3, 0, 1),
        new THREE.Quaternion().setFromEuler(new THREE.Euler(-0.5, 0.9, 0.1)), new THREE.Vector3(0.5, 1, 2));
    add('mat4_multiply', {a: a.toArray(), b: b.toArray()}, a.clone().multiply(b).toArray());
    add('mat4_invert', {m: a.toArray()}, a.clone().invert().toArray());
    add('mat4_determinant', {m: b.toArray()}, b.determinant());

    const look = new THREE.Matrix4().lookAt(new THREE.Vector3(1, 2, 3), new THREE.Vector3(0, 0, 0), new THREE.Vector3(0, 1, 0));
    add('mat4_look_at', {eye: [1, 2, 3], target: [0, 0, 0], up: [0, 1, 0]}, look.toArray());
    // eye==target 退化分支
    const lookDeg = new THREE.Matrix4().lookAt(new THREE.Vector3(0, 0, 0), new THREE.Vector3(0, 0, 0), new THREE.Vector3(0, 1, 0));
    add('mat4_look_at_degenerate', {eye: [0, 0, 0], target: [0, 0, 0], up: [0, 1, 0]}, lookDeg.toArray());

    const withScale = new THREE.Matrix4().compose(new THREE.Vector3(1, 0, 0),
        new THREE.Quaternion().setFromEuler(new THREE.Euler(0, 0.6, 0)), new THREE.Vector3(2, 4, 8));
    add('mat4_extract_rotation', {m: withScale.toArray()}, new THREE.Matrix4().extractRotation(withScale).toArray());
}

// ---- Object3D 场景图 ----
{
    // 父链：root → mid → leaf，各自有平移/旋转/缩放
    const root = new THREE.Object3D();
    root.position.set(1, 2, 3);
    root.rotation.set(0.1, 0.2, 0.3);
    root.scale.set(2, 2, 2);
    const mid = new THREE.Object3D();
    mid.position.set(0.5, -1, 2);
    mid.rotation.set(-0.4, 0.9, 0.1, 'YXZ');
    mid.scale.set(0.5, 1, 1.5);
    const leaf = new THREE.Object3D();
    leaf.position.set(1, 1, 1);
    leaf.rotation.set(2.0, -0.3, 0.8);
    root.add(mid);
    mid.add(leaf);
    root.updateMatrixWorld(true);

    add('object3d_world_matrix', {}, leaf.matrixWorld.toArray());
    add('object3d_world_position', {}, leaf.getWorldPosition(new THREE.Vector3()).toArray());
    add('object3d_world_quaternion', {}, leaf.getWorldQuaternion(new THREE.Quaternion()).toArray());
    add('object3d_local_to_world', {v: [0.25, -0.5, 2]}, leaf.localToWorld(new THREE.Vector3(0.25, -0.5, 2)).toArray());
    add('object3d_world_to_local', {v: [3, -2, 5]}, leaf.worldToLocal(new THREE.Vector3(3, -2, 5)).toArray());

    // lookAt（有父节点，父带旋转缩放）
    const watcher = new THREE.Object3D();
    watcher.position.set(0.2, 0.3, 0.4);
    mid.add(watcher);
    root.updateMatrixWorld(true);
    watcher.lookAt(new THREE.Vector3(10, 0, -5));
    add('object3d_look_at', {}, watcher.quaternion.toArray());
    // lookAt 后 Euler 联动
    add('object3d_look_at_euler_sync', {}, [watcher.rotation.x, watcher.rotation.y, watcher.rotation.z, watcher.rotation.order]);

    // Euler→Quaternion 联动：直接改 rotation 字段
    const o = new THREE.Object3D();
    o.rotation.set(0.3, -1.1, 2.2, 'YXZ');
    add('object3d_rotation_sync_quat', {e: [0.3, -1.1, 2.2, 'YXZ']}, o.quaternion.toArray());
    o.rotation.z += 0.5;
    add('object3d_rotation_z_increment', {}, o.quaternion.toArray());
}

// ---- Plane / Line3 / Sphere ----
{
    const plane = new THREE.Plane().setComponents(0, 1, 0, 0);
    const hit = new THREE.Line3(new THREE.Vector3(0, 1, 0), new THREE.Vector3(0, -1, 0));
    add('plane_intersects_line', {plane: [0, 1, 0, 0], line: [[0, 1, 0], [0, -1, 0]]}, plane.intersectsLine(hit));
    const target = new THREE.Vector3();
    const inter = plane.intersectLine(hit, target);
    add('plane_intersect_line_hit', {plane: [0, 1, 0, 0], line: [[0, 1, 0], [0, -1, 0]]}, inter ? inter.toArray() : null);

    const miss = new THREE.Line3(new THREE.Vector3(0, 1, 0), new THREE.Vector3(1, 2, 3));
    add('plane_intersect_line_miss', {plane: [0, 1, 0, 0], line: [[0, 1, 0], [1, 2, 3]]}, plane.intersectLine(miss, new THREE.Vector3()));

    const tilted = new THREE.Plane().setComponents(1, 2, 3, -4).normalize();
    const l2 = new THREE.Line3(new THREE.Vector3(-5, 0, 0), new THREE.Vector3(5, 1, 1));
    const i2 = tilted.intersectLine(l2, new THREE.Vector3());
    add('plane_intersect_line_tilted', {plane: [1, 2, 3, -4], normalize: true, line: [[-5, 0, 0], [5, 1, 1]]}, i2 ? i2.toArray() : null);

    const sphere = new THREE.Sphere(new THREE.Vector3(0, 0.05, 0), 0.1);
    add('plane_intersects_sphere', {plane: [0, 1, 0, 0], center: [0, 0.05, 0], radius: 0.1}, plane.intersectsSphere(sphere));
    const sphereFar = new THREE.Sphere(new THREE.Vector3(0, 0.5, 0), 0.1);
    add('plane_intersects_sphere_far', {plane: [0, 1, 0, 0], center: [0, 0.5, 0], radius: 0.1}, plane.intersectsSphere(sphereFar));

    // kill_plane 路径：setComponents 后 wintersky 会再 calculate → 已含 normalize 验证
    const kp = new THREE.Plane().setComponents(0, 2, 0, -1);
    add('plane_set_components_not_normalized', {plane: [0, 2, 0, -1]}, [kp.normal.x, kp.normal.y, kp.normal.z, kp.constant]);
}

// ---- 曲线 ----
{
    const pts = [[-1, 0.1], [0, 0.9], [1, -0.4], [2, 0.7], [3, 0.0]].map(p => new THREE.Vector2(p[0], p[1]));
    const spline = new THREE.SplineCurve(pts);
    for (const t of [0, 0.13, 0.25, 0.5, 0.77, 1.0]) {
        add('spline_curve', {points: pts.map(p => p.toArray()), t}, spline.getPoint(t).toArray());
    }

    const bez = new THREE.CubicBezierCurve(
        new THREE.Vector2(0, 0), new THREE.Vector2(0.33, 1.2),
        new THREE.Vector2(0.66, -0.8), new THREE.Vector2(1, 0.5));
    for (const t of [0, 0.2, 0.5, 0.83, 1.0]) {
        add('cubic_bezier', {v0: [0, 0], v1: [0.33, 1.2], v2: [0.66, -0.8], v3: [1, 0.5], t}, bez.getPoint(t).toArray());
    }
}

// ---- PlaneGeometry(2,2) ----
{
    const geo = new THREE.PlaneGeometry(2, 2);
    add('plane_geometry', {width: 2, height: 2}, {
        position: Array.from(geo.getAttribute('position').array),
        normal: Array.from(geo.getAttribute('normal').array),
        uv: Array.from(geo.getAttribute('uv').array),
        index: geo.getIndex().array ? Array.from(geo.getIndex().array) : geo.getIndex(),
    });
}

const out = {cases};
const here = path.dirname(fileURLToPath(import.meta.url));
const target = path.resolve(here, '../../src/test/resources/wintersky/three_cases.json');
fs.mkdirSync(path.dirname(target), {recursive: true});
fs.writeFileSync(target, JSON.stringify(out, null, 1));
console.log('wrote', cases.length, 'cases ->', target);
