# BrResourcesLoader 硬编码前缀导致资源查找失败

## 症状

通过 `@ResourceLoader` 注册的 `BrResourcesLoader` 子类（`BrAttachableLoader`、`BrRenderControllerLoader`、
`BrModelLoader` 等）运行时 `apply` 收到 0 个文件，Manager 为空，资源未加载。

日志显示 `scanDirectory prefix=eyelib/attachables` 而非预期的 `prefix=attachables`。

## 原因

`BrResourcesLoader` 构造函数拼接了 `"eyelib/"` 前缀：

```java
// BrResourcesLoader.java:12
public BrResourcesLoader(String directory, String suffix) {
    super(new GsonBuilder().setLenient().create(), "eyelib/" + directory, suffix);
}
```

`FileToIdConverter` 的搜索前缀变成 `"eyelib/attachables"`，只会匹配
`assets/<namespace>/eyelib/attachables/*.json`，而非 `assets/<namespace>/attachables/*.json`。

因此 mod 资源必须放在 `assets/<modid>/eyelib/<type>/` 路径下
（比如 `assets/test/eyelib/attachables/test_stick.json`）。

## 正确做法

- 本 mod 内部资源放 `assets/eyelib/eyelib/<type>/`——遵守前缀约定
- 测试或外部资源放 `assets/<your-namespace>/eyelib/<type>/`——namespace 随意但第二层必须是 `eyelib/`
- 路径模板：`assets/<namespace>/eyelib/<resource_type>/<filename>.json`
