#!/usr/bin/env python3
"""Run DayFlow commands on an explicitly authorized Android device through ADB."""
import argparse
import base64
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import uuid


BASE = "content://app.planaloud.commands"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", nargs="?", default="help")
    parser.add_argument("args", nargs="?", default="{}", help="JSON object")
    parser.add_argument("--args-file", type=Path, help="Read command arguments from a UTF-8 JSON file")
    parser.add_argument("--input", type=Path, help="Upload a file for an import or image command")
    parser.add_argument("--output", type=Path, help="Destination for backup.export, notes.export or record.export")
    parser.add_argument("--serial", help="ADB device serial")
    parser.add_argument("--user", type=int, default=0, help="Android user ID (default 0)")
    parser.add_argument("--adb", help="Path to adb executable")
    options = parser.parse_args()
    args_text = (sys.stdin.read() if str(options.args_file) == "-" else options.args_file.read_text(encoding="utf-8-sig")) if options.args_file else options.args
    args = json.loads(args_text)
    private_request = options.command in {"sync.login", "sync.connect-taskflow"}
    if private_request and not options.args_file:
        parser.error("use --args-file or --args-file - for credentials")
    if not isinstance(args, dict):
        parser.error("args must be a JSON object")
    sdk = os.getenv("ANDROID_HOME") or os.getenv("ANDROID_SDK_ROOT")
    installed = Path(sdk) / "platform-tools" / ("adb.exe" if os.name == "nt" else "adb") if sdk else None
    adb = options.adb or shutil.which("adb") or (str(installed) if installed and installed.is_file() else None)
    if not adb:
        parser.error("ADB not found; install Android platform-tools or pass --adb")
    prefix = [adb] + (["-s", options.serial] if options.serial else [])

    def shell(*arguments, **kwargs):
        return subprocess.run(prefix + ["shell", "-T", *arguments], check=True, **kwargs)

    def content(verb, uri, *arguments, **kwargs):
        return shell("content", verb, "--user", str(options.user), "--uri", uri, *arguments, **kwargs)

    staged = []

    def upload(stream):
        token = str(uuid.uuid4())
        uri = f"{BASE}/input/{token}"
        staged.append(uri)
        content("write", uri, stdin=stream)
        return token

    try:
        exports = {"backup.export": "backup", "notes.export": "notes", "record.export": "record"}
        if options.command in exports:
            if options.output is None:
                parser.error("this command needs --output")
            target = exports[options.command]
            if target == "record":
                target += "/" + str(uuid.UUID(args["id"]))
            path = options.output.resolve()
            temporary = None
            try:
                with tempfile.NamedTemporaryFile(dir=path.parent, prefix=".dayflow-", delete=False) as file:
                    temporary = Path(file.name)
                    proc = content("read", f"{BASE}/{target}", stdout=file, stderr=subprocess.PIPE)
                    if proc.stderr.strip():
                        raise RuntimeError(proc.stderr.decode("utf-8", errors="replace").strip())
                with temporary.open("rb") as file:
                    signature = file.read(4)
                if not signature.startswith(b"PK" if target == "notes" else b"{"):
                    raise RuntimeError("未收到有效的文件；请检查 ADB 指令是否开启。")
                temporary.replace(path)
                print(json.dumps({"ok": True, "file": str(path)}, ensure_ascii=False))
            finally:
                if temporary is not None:
                    temporary.unlink(missing_ok=True)
            return 0
        if options.input:
            with options.input.open("rb") as file:
                args["file"] = upload(file)
        payload = json.dumps({"command": options.command, "args": args}, ensure_ascii=False).encode("utf-8")
        encoded = base64.urlsafe_b64encode(payload).decode("ascii")
        if private_request or len(encoded) > 18000:
            with tempfile.TemporaryFile() as file:
                file.write(payload)
                file.seek(0)
                encoded = "@" + upload(file)
        raw = content("call", BASE, "--method", "run", "--arg", encoded, capture_output=True)
        reply = re.search(rb"reply=([A-Za-z0-9_-]+)", raw.stdout)
        if not reply:
            raise RuntimeError((raw.stderr or raw.stdout).decode("utf-8", errors="replace").strip() or "未收到日栖回复；请在指令台开启 ADB 指令。")
        token = reply.group(1)
        result = json.loads(base64.urlsafe_b64decode(token + b"=" * (-len(token) % 4)))
        if result.get("ok") and options.command == "screen.open":
            ticket = str(uuid.UUID(result["result"]["ticket"]))
            shell("am", "start", "--user", str(options.user), "-n", "app.planaloud/.MainActivity", "--es", "dayflow_screen_ticket", ticket, capture_output=True)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return 0 if result.get("ok") else 1
    finally:
        for uri in staged:
            subprocess.run(prefix + ["shell", "-T", "content", "delete", "--user", str(options.user), "--uri", uri], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (RuntimeError, OSError, ValueError, KeyError, subprocess.CalledProcessError) as error:
        print(json.dumps({"ok": False, "error": str(error)}, ensure_ascii=False))
        raise SystemExit(1)
