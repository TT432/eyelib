# 非 Mod 库必须用 additionalRuntimeClasspath 包裹

## 现象
用 `implementation 'group:artifact:version'` 添加的非 Minecraft/Forge 第三方库，编译通过但在运行时 ClassNotFoundException。

## 原因
Forge 的运行时 classpath 机制与普通 Gradle 项目不同。`implementation` 仅将库加入编译 classpath，但 Forge 在运行时需要通过 `additionalRuntimeClasspath` 才能让类加载器可见这些库。

## 正确做法
```groovy
additionalRuntimeClasspath(implementation('group:artifact:version'))
```

缺了里层的 `implementation()` 只能上运行时 classpath，编译阶段会报包不存在。

## 参考
- `build.gradle:189` — `jdk-classfile-backport`
- `build.gradle:190` — `h2`
