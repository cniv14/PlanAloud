# Android 1.5.2 Debug

此包使用独立 Debug 签名（与上次 1.5.1 预览包相同），不属于正式发行，也不能覆盖正式签名安装。完整操作变化见正式版说明。

# 序言 Android 1.5.2

## 导航与首次使用

- 普通切页保持底栏和菜单项实例，只更新选中项；仅在实际改变显示栏目时重建菜单，修复高亮丢失和闪烁。
- 左右滑动遵循当前可见栏目，跳过隐藏的笔记和收集箱；今天左滑先进入一周预览。纵向滚动、输入控件、横向表格、底部按钮和系统返回边缘不触发切页。多指或取消手势不会提交切换。
- 新安装显示三步引导：本地／云端使用选择、基本操作、开始使用。每一步顶部都有固定的「跳过」。本地模式无需账号；云端在登录服务器成功后才开启同步，也可稍后配置。
- 引导支持旋转恢复。已有安装不会强制弹出，可从设置或 `/guide` 重新打开。重看时跳过不改变原同步配置。

## 语音与体积

- 完全移除应用内本地识别实现、Whisper 原生桥接、NDK/CMake 构建、模型及引擎下载／导入入口、对应发布脚本和资源。升级清理旧模型与引擎缓存，不删除其他文件。
- 保留语音插件入口、`settings_only` 参数及 `voice_text` 返回协议。默认 DeepSeek 官方 ASR；使用官方 DeepSeek 地址保存 AI 配置时，一起保存语音配置和密钥。非官方网关密钥不会发送给 DeepSeek。
- 识别文字先回填输入框；录音临时文件在结束后删除。保留系统语音和百炼接口作为可选服务。
- 正式包启用 R8 代码优化和资源裁剪。Debug 包另行发布；测试依赖不包含在安装包中。保留语音插件反射访问的宿主接口，避免裁剪破坏插件兼容性。

## 签名变化与备份

原 1.5.0 签名私钥未在 GitHub 分支、历史或发布附件中找到。经维护者要求，1.5.2 使用新的正式签名证书；因此无法覆盖旧正式签名或 Debug 签名安装。请先导出待办、课表和笔记，换装后导入；账号、API Key 等设备配置需要重新填写。今后继续使用本次证书签名，可正常覆盖升级。

Android 更新清单和模块使用新的 Android 更新公钥；Windows 的更新公钥与既有签名清单保持不变。首次迁移需手动安装，不发布声称可供旧签名覆盖安装的更新。

私钥以 `PlanAloud-signing-1.5.2.enc.json` 加密备份发布，内含 Android keystore、构建配置和 Android 更新签名私钥。采用随机独立口令、scrypt 和 AES-256-GCM；解密凭据不上传 GitHub，必须另存到安全位置。

在新电脑恢复：

```sh
python -m pip install cryptography
python tools/signing_backup.py restore --source PlanAloud-signing-1.5.2.enc.json --password-file signing-password.txt --output .signing
python tools/release_keys.py --android-only
```

目标 `.signing` 目录必须为空。恢复工具自动修正 keystore 路径。请同时保存加密备份和单独交付的解密凭据；缺少其中之一不能恢复。

## 构建与验证

使用 JDK17 或更高版本、SDK36、Gradle9.6.1。无需本地语音模型、NDK 或 CMake。

```sh
cd android
gradle :app:assembleRelease :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestJavaWithJavac :app:lintDebug
```

正式版从被 Git 忽略的 `.signing/android.json` 或 `DAYFLOW_KEYSTORE` 环境变量读取签名；没有正式签名时只产生未签名构建，不以 Debug 密钥替代正式包。`tools/signing_backup.py` 用于加密备份与恢复，不会打印密钥或口令。

回归覆盖可见栏目组合、底栏状态与菜单实例、实际触摸滑动及滚动冲突、引导跳过／旋转／云端选择、旧模型清理。Android UI 测试基于 Robolectric，尚未在真实手机上复测；在线语音仍需使用实际服务密钥验证。

本次验证：正式版／Debug 构建、14 项 Android UI 与手势回归、3 项加密备份测试、Lint、APK 签名、四个模块及更新清单签名均已通过。正式 APK 约 5.0 MiB，相比上一预览包约缩小 58%。
