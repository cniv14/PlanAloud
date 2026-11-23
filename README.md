# Android 1.5.10 未签名构建

此 APK 尚未签名，不能直接安装。可覆盖现有正式版的安装包必须使用原正式证书签名。

源码：[fix/widget-edges-schedule](https://github.com/cniv14/PlanAloud/tree/fix/widget-edges-schedule)，提交 `9f3f8e9df5fb9b5eb74ca30be110df7ab0f550df`。

- 修复近期日程卡片和组件外框的位图拉伸模糊，Android 12 及以上使用原生圆角，Android 8–11 保留回退。
- 今天／明天双日组件恢复固定深色底、文字和侧边色条，不跟随应用或单个事件的卡片图片、纹理和动画。
- 移除课表专用流程，使用日期、时间和普通重复日程；旧课程、提醒与同步记录保留。

验证：Debug／未签名 Release 构建、Lint（0 错误）、仪器测试源码编译和 7 项回归全部通过；APK ZIP 完整性检查通过，版本代码 160。未进行实体手机复测。

`SHA256SUMS` 与 `build-info.json` 记录本包哈希和源码版本。

正式签名的加密备份位于 `builds/android-1.5.2` 分支，恢复需要原先单独保存的解密凭据。当前环境尚缺该凭据。
