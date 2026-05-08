# 07-02 硬件验证清单

**计划:** 07-02 — ClientSmokeExitCodeTest & Hardware Verification
**需求:** CORR-03, CORR-04
**状态:** 待用户执行

## 前置条件

- Windows 机器，已安装 JDK 17
- 项目已检出到 `E:\_ideaProjects\qylEyelib`，当前提交为 `HEAD`
- **必须使用 cmd.exe**（非 PowerShell）——`echo %ERRORLEVEL%` 是 cmd 特有的

---

## 静态验证已完成（自动化）

以下内容已通过 `ClientSmokeExitCodeTest.java`（12 个测试全部通过）进行了静态验证：

- ✅ `handleExit()` 使用条件退出码：`int exitCode = (failedCount > 0) ? 1 : 0`
- ✅ `Runtime.getRuntime().halt(exitCode)` 使用变量，非硬编码字面量
- ✅ 活动代码（去除注释后）中无 `halt(0)` 回归
- ✅ `buildJUnitXml()` 生成有效的 JUnit XML — 包含 `<testsuite>`、`<testcase>`、`<failure>` 元素
- ✅ `escapeXml()` 正确转义全部 5 种 XML 特殊字符

---

## 第 1 步：验证 `runClient` ——无 smoke 干扰（CORR-03 硬件验证）

1. 打开 **cmd.exe**
2. 进入项目根目录：`cd E:\_ideaProjects\qylEyelib`
3. 清理之前运行的文件：`rmdir /s /q run 2>nul & rmdir /s /q run\clientsmoke 2>nul`
4. 启动：`gradlew runClient`
5. 等待 Minecraft 完全加载（出现主菜单）
6. **验证：** 游戏正常启动，无崩溃。打开 `run/logs/latest.log` — 搜索 `[ClientSmoke]`。你应该能看到 mod 构建横幅，但**没有**状态机转换日志（无"INIT → CONFIG_LOAD"、无世界创建、无测试执行）。Smoke mod 存在但处于空闲状态。
7. 正常关闭 Minecraft（点击 X）
8. **检查游戏目录：** `run/` 存在且内容正常（saves、options.txt 等）——**不是** `run/clientsmoke/`（仅 smoke 运行使用）

**预期结果：** `gradlew runClient` 启动到主菜单。日志中无 smoke 测试活动。`run/` 目录正常。

---

## 第 2 步：验证 `runClientSmoke` ——通过场景 → 退出码 0（CORR-04）

**前提条件：** 至少一个 `@ClientSmoke` 测试类存在且能通过。如果不存在真实测试，状态机会处理空测试集的情况（CORR-01）——生成空报告并以退出码 0 退出。同样可以验证通过场景。

1. 打开 **cmd.exe**
2. 进入项目根目录：`cd E:\_ideaProjects\qylEyelib`
3. 清理：`rmdir /s /q run\clientsmoke 2>nul & rmdir /s /q clientsmoke-reports 2>nul`
4. 启动：`gradlew runClientSmoke`
5. 等待完整流程：Minecraft 启动 → 加载世界 → 状态机运行 → 自动退出
6. Minecraft 关闭且 Gradle 完成后，**立即**运行：`echo %ERRORLEVEL%`
7. **验证退出码：** `echo %ERRORLEVEL%` 显示 `0`
8. **验证 Gradle 输出：** Gradle 输出的最后一行应为 `BUILD SUCCESSFUL`
9. **验证报告：** 检查 `clientsmoke-reports/` — 应包含：
   - `report-{yyyyMMdd-HHmmss}.json`（JSON 报告）
   - `junit-{yyyyMMdd-HHmmss}.xml`（JUnit XML 报告）
   - 如果测试有截图：`screenshots/` 子目录
10. **验证游戏目录隔离：** `run/clientsmoke/` 存在（smoke 游戏目录），但 `run/` 未被修改（正常游戏目录不变）

**预期结果：** `BUILD SUCCESSFUL`，`echo %ERRORLEVEL%` → `0`，`clientsmoke-reports/` 中生成报告，`run/` 未被修改。

---

## 第 3 步：验证 `runClientSmoke` ——失败场景 → 退出码 1（CORR-04）

**前提条件：** 需要一个故意失败的 `@ClientSmoke` 测试类。**请创建以下临时文件：**

文件：`eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/FailingTest.java`
```java
package io.github.tt432.clientsmoke;

import io.github.tt432.clientsmoke.annotation.ClientSmoke;

@ClientSmoke(description = "Deliberate failure for exit code verification", priority = 999)
public class FailingTest {
    public FailingTest() {
        throw new RuntimeException("This test deliberately fails to verify exit code 1");
    }
}
```

> ⚠️ **验证完成后请删除此文件。** 不应提交到版本控制。

1. 确保失败测试类存在（见上文）
2. 打开 **cmd.exe**
3. 进入项目根目录：`cd E:\_ideaProjects\qylEyelib`
4. 清理：`rmdir /s /q run\clientsmoke 2>nul & rmdir /s /q clientsmoke-reports 2>nul`
5. 启动：`gradlew runClientSmoke`
6. 等待 Minecraft 加载、执行失败测试并退出
7. Gradle 完成后，**立即**运行：`echo %ERRORLEVEL%`
8. **验证退出码：** `echo %ERRORLEVEL%` 显示 `1`
9. **验证 Gradle 输出：** Gradle 输出的最后一行应为 `BUILD FAILED`
10. **验证 JUnit XML：** 打开 `clientsmoke-reports/junit-*.xml` — 应包含 `<failure message="..." type="java.lang.Exception">` 及堆栈信息
11. **删除 `FailingTest.java` 文件**

**预期结果：** `BUILD FAILED`，`echo %ERRORLEVEL%` → `1`，JUnit XML 包含 `<failure>` 元素。

---

## 第 4 步：验证正常开发流程零回归

1. 启动 `gradlew runClient`（与第 1 步相同）
2. 验证仍然正常工作——无崩溃、无 smoke 干扰
3. 再次启动 `gradlew runClient`（模拟重复开发启动）
4. 检查 `run/logs/latest.log` — 只有 mod 构建横幅，无状态机活动
5. 验证 `run/` 目录干净（无 `clientsmoke-reports/` 泄漏到正常运行目录）

**预期结果：** 重复 `runClient` 启动行为一致。无 smoke 产物泄漏到正常 `run/`。

---

## 报告结果

执行完成后，请按以下格式回复：

```
STEP 1 (runClient idle): [PASS / FAIL] — [任何备注]
STEP 2 (runClientSmoke pass → exit 0): [PASS / FAIL] — [任何备注]
STEP 3 (runClientSmoke fail → exit 1): [PASS / FAIL] — [任何备注]
STEP 4 (no regression): [PASS / FAIL] — [任何备注]
```
