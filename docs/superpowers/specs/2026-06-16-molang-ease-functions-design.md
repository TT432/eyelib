# Molang math.ease_* 函数库 — 实现规格

**Date:** 2026-06-16
**Scope:** 30 个缓动函数（国际版基岩版 1.21.120+ 新增）

## 目标

在 `MolangMath.java` 中添加 30 个 `math.ease_*` 函数，签名统一为 `ease_xxx(float start, float end, float t)`，其中 `t` 是 [0, 1] 的进度值。

## 签名

所有 30 个函数的签名一致（3 参数，与 `lerp` 一致）：

```java
public static float ease_xxx(float start, float end, float t)
```

返回 `start + (end - start) * easingCurve(t)`，其中 `easingCurve(t)` 是标准缓动曲线。

## 公式（t ∈ [0, 1]）

### 常量

```java
private static final float C1_BACK = 1.70158F;
private static final float C2_BACK = 1.70158F * 1.525F;
private static final float C3_BACK = 1.70158F + 1F;   // 2.70158
private static final float C4_ELASTIC = (float) (2 * Math.PI / 3);        // ≈ 2.0944
private static final float C5_ELASTIC = (float) (2 * Math.PI / 4.5);      // ≈ 1.3963
private static final float N_BOUNCE = 7.5625F;
private static final float D_BOUNCE = 2.75F;
```

### Sine

```java
ease_in_sine(t) = 1 - cos(t * π/2)
ease_out_sine(t) = sin(t * π/2)
ease_inout_sine(t) = -(cos(π * t) - 1) / 2
```

### Quad

```java
ease_in_quad(t) = t * t
ease_out_quad(t) = 1 - (1 - t) * (1 - t)
ease_inout_quad(t) = t < 0.5 ? 2 * t * t : 1 - (-2 * t + 2) * (-2 * t + 2) / 2
```

### Cubic

```java
ease_in_cubic(t) = t * t * t
ease_out_cubic(t) = 1 - (1 - t)³
ease_inout_cubic(t) = t < 0.5 ? 4 * t³ : 1 - (-2t + 2)³ / 2
```

### Quart

```java
ease_in_quart(t) = t⁴
ease_out_quart(t) = 1 - (1 - t)⁴
ease_inout_quart(t) = t < 0.5 ? 8 * t⁴ : 1 - (-2t + 2)⁴ / 2
```

### Quint

```java
ease_in_quint(t) = t⁵
ease_out_quint(t) = 1 - (1 - t)⁵
ease_inout_quint(t) = t < 0.5 ? 16 * t⁵ : 1 - (-2t + 2)⁵ / 2
```

### Expo

```java
ease_in_expo(t) = t == 0 ? 0 : pow(2, 10 * t - 10)
ease_out_expo(t) = t == 1 ? 1 : 1 - pow(2, -10 * t)
ease_inout_expo(t):
    t == 0 → 0
    t == 1 → 1
    t < 0.5 → pow(2, 20 * t - 10) / 2
    else → (2 - pow(2, -20 * t + 10)) / 2
```

### Circ

```java
ease_in_circ(t) = 1 - sqrt(1 - t * t)
ease_out_circ(t) = sqrt(1 - (t - 1) * (t - 1))
ease_inout_circ(t):
    t < 0.5 → (1 - sqrt(1 - (2t)²)) / 2
    else → (sqrt(1 - (-2t + 2)²) + 1) / 2
```

### Back

```java
ease_in_back(t) = C3_BACK * t³ - C1_BACK * t²
ease_out_back(t) = 1 + C3_BACK * (t-1)³ + C1_BACK * (t-1)²
ease_inout_back(t):
    t < 0.5 → ((2t)² * ((C2_BACK + 1) * 2t - C2_BACK)) / 2
    else → ((2t - 2)² * ((C2_BACK + 1) * (2t - 2) + C2_BACK) + 2) / 2
```

### Bounce

```java
ease_out_bounce(t):
    if t < 1/D_BOUNCE → N_BOUNCE * t²
    elif t < 2/D_BOUNCE → t -= 1.5/D_BOUNCE; N_BOUNCE * t² + 0.75
    elif t < 2.5/D_BOUNCE → t -= 2.25/D_BOUNCE; N_BOUNCE * t² + 0.9375
    else → t -= 2.625/D_BOUNCE; N_BOUNCE * t² + 0.984375

ease_in_bounce(t) = 1 - ease_out_bounce(1 - t)
ease_inout_bounce(t):
    t < 0.5 → (1 - ease_out_bounce(1 - 2t)) / 2
    else → (1 + ease_out_bounce(2t - 1)) / 2
```

### Elastic

```java
ease_in_elastic(t):
    t == 0 → 0
    t == 1 → 1
    else → -pow(2, 10*t - 10) * sin((t*10 - 10.75) * C4_ELASTIC)

ease_out_elastic(t):
    t == 0 → 0
    t == 1 → 1
    else → pow(2, -10*t) * sin((t*10 - 0.75) * C4_ELASTIC) + 1

ease_inout_elastic(t):
    t == 0 → 0
    t == 1 → 1
    t < 0.5 → -(pow(2, 20*t - 10) * sin((20*t - 11.125) * C5_ELASTIC)) / 2
    else → (pow(2, -20*t + 10) * sin((20*t - 11.125) * C5_ELASTIC)) / 2 + 1
```

## 实现方式

每个函数的实现模式：

```java
public static float ease_in_back(float start, float end, float t) {
    float eased = /* easingCurve(t) */;
    return start + (end - start) * eased;
}
```

将 easingCurve 提取为 private static 方法以复用（ease_in_bounce 复用 ease_out_bounce 等）。

## 文件影响

| 文件 | 变更 |
|---|---|
| `MolangMath.java` | 新增 30 个 public 方法 + private 缓动曲线辅助方法 + 常量 |

## 测试

```java
// 边界值
assert ease_in_quad(0, 1, 0) == 0;
assert ease_in_quad(0, 1, 1) == 1;
assert ease_in_quad(10, 20, 0) == 10;
assert ease_in_quad(10, 20, 1) == 20;

// 对称性：ease_in(f, 0, 1-t) == 1 - ease_out(f, 0, 1, t)（对 bounce）
// 中点值
assert abs(ease_inout_sine(0, 1, 0.5) - 0.5) < 0.01;  // 中点约为 0.5

// ease_out_bounce 边界
assert ease_out_bounce(0, 1, 0) == 0;
assert ease_out_bounce(0, 1, 1) == 1;

// ease_in_elastic 边界
assert ease_in_elastic(0, 1, 0) == 0;
assert ease_in_elastic(0, 1, 1) == 1;
```
