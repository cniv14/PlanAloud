# 正式签名加密备份

`PlanAloud-signing-1.5.2.enc.json` 包含 Android keystore、构建配置和 Android 更新签名私钥，以 scrypt + AES-256-GCM 加密。解密凭据通过原聊天单独交付，没有放到 GitHub；必须保存两者。

克隆本仓库后，在空的 `.signing` 目录恢复：

```sh
python -m pip install cryptography
python tools/signing_backup.py restore --source PlanAloud-signing-1.5.2.enc.json --password-file signing-password.txt --output .signing
python tools/release_keys.py --android-only
```

恢复工具会修正 keystore 路径，不会覆盖现有目录。不要把解密凭据、恢复后的文件或 `.signing` 提交到公开仓库。后续正式 APK 必须沿用这套证书；Windows 更新公钥未旋转。
