"""Encrypt signing material for off-machine backup. Never upload the password file."""
import argparse
import base64
import io
import json
import os
from pathlib import Path
import secrets
import zipfile
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.scrypt import Scrypt

FILES = ('android.json', 'dayflow.jks', 'updates-private.pem')

def derive(password, salt):
    return Scrypt(salt=salt, length=32, n=32768, r=8, p=1).derive(password)

def create(source, destination, password_file):
    source, destination, password_file = map(Path, (source, destination, password_file))
    if destination.exists() or password_file.exists():
        raise ValueError('Refusing to replace an existing backup or password file')
    for name in FILES:
        if not (source / name).is_file():
            raise ValueError('Missing signing file: ' + name)
    archive = io.BytesIO()
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as output:
        for name in FILES:
            output.write(source / name, name)
    password = secrets.token_urlsafe(32).encode('ascii')
    salt, nonce = os.urandom(16), os.urandom(12)
    header = {'schema': 1, 'cipher': 'AES-256-GCM', 'kdf': 'scrypt-32768-8-1',
              'salt': base64.b64encode(salt).decode(), 'nonce': base64.b64encode(nonce).decode()}
    authenticated = json.dumps(header, sort_keys=True, separators=(',', ':')).encode()
    ciphertext = AESGCM(derive(password, salt)).encrypt(nonce, archive.getvalue(), authenticated)
    destination.parent.mkdir(parents=True, exist_ok=True)
    password_file.parent.mkdir(parents=True, exist_ok=True)
    with password_file.open('xb') as output:
        os.chmod(password_file, 0o600)
        output.write(password + b'\n')
    envelope = dict(header, ciphertext=base64.b64encode(ciphertext).decode())
    with destination.open('x', encoding='utf-8') as output:
        json.dump(envelope, output)

def restore(backup, destination, password_file):
    destination = Path(destination)
    if destination.exists() and any(destination.iterdir()):
        raise ValueError('Restore destination must be empty')
    envelope = json.loads(Path(backup).read_text())
    if envelope.get('schema') != 1 or envelope.get('cipher') != 'AES-256-GCM' or envelope.get('kdf') != 'scrypt-32768-8-1':
        raise ValueError('Unsupported backup format')
    ciphertext = base64.b64decode(envelope.pop('ciphertext'), validate=True)
    authenticated = json.dumps(envelope, sort_keys=True, separators=(',', ':')).encode()
    salt, nonce = base64.b64decode(envelope['salt'], validate=True), base64.b64decode(envelope['nonce'], validate=True)
    if len(salt) != 16 or len(nonce) != 12:
        raise ValueError('Invalid backup parameters')
    password = Path(password_file).read_bytes().strip()
    plaintext = AESGCM(derive(password, salt)).decrypt(nonce, ciphertext, authenticated)
    with zipfile.ZipFile(io.BytesIO(plaintext)) as archive:
        if sorted(archive.namelist()) != sorted(FILES):
            raise ValueError('Invalid signing archive paths')
        contents = {name: archive.read(name) for name in FILES}
    destination.mkdir(parents=True, exist_ok=True)
    os.chmod(destination, 0o700)
    for name, content in contents.items():
        target = destination / name
        with target.open('xb') as output:
            os.chmod(target, 0o600)
            output.write(content)
    # Gradle's keystore path must point to this restored checkout, not the original machine.
    config = json.loads((destination / 'android.json').read_text())
    config['storeFile'] = str((destination / 'dayflow.jks').resolve())
    (destination / 'android.json').write_text(json.dumps(config))

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['create', 'restore'])
    parser.add_argument('--source', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--password-file', type=Path, required=True)
    args = parser.parse_args()
    if args.source is None:
        parser.error('--source is required')
    try:
        if args.action == 'create': create(args.source, args.output, args.password_file)
        else: restore(args.source, args.output, args.password_file)
    except Exception as error:
        raise SystemExit('Signing backup operation failed (' + type(error).__name__ + '); no secrets were printed.')
    print('Encrypted backup ready.' if args.action == 'create' else 'Signing material restored locally.')

if __name__ == '__main__': main()
