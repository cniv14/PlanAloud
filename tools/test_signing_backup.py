"""Exercise encrypted signing backups with disposable test keys, never production files."""
import json
from pathlib import Path
import tempfile
import unittest
from cryptography.exceptions import InvalidTag
from signing_backup import create, restore, FILES

class SigningBackupTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name)
        self.source=self.root/'source';self.source.mkdir()
        for name in FILES:(self.source/name).write_bytes(b'test-private-material:'+name.encode())
        (self.source/'android.json').write_text(json.dumps({'storeFile':'/old/dayflow.jks','storePassword':'test-password'}))
        self.backup=self.root/'keys.enc.json';self.password=self.root/'password.txt'
    def test_round_trip_and_path_relocation(self):
        create(self.source,self.backup,self.password);destination=self.root/'restored';restore(self.backup,destination,self.password)
        for name in ['dayflow.jks','updates-private.pem']:self.assertEqual((self.source/name).read_bytes(),(destination/name).read_bytes())
        self.assertEqual(str((destination/'dayflow.jks').resolve()),json.loads((destination/'android.json').read_text())['storeFile'])
        self.assertNotIn('test-private-material',self.backup.read_text());self.assertNotIn('test-password',self.backup.read_text())
    def test_wrong_password_writes_nothing(self):
        create(self.source,self.backup,self.password);self.password.write_text('wrong');destination=self.root/'restored'
        with self.assertRaises(InvalidTag):restore(self.backup,destination,self.password)
        self.assertFalse(destination.exists())
    def test_tampering_is_rejected_and_backups_are_not_overwritten(self):
        create(self.source,self.backup,self.password)
        with self.assertRaises(ValueError):create(self.source,self.backup,self.password)
        envelope=json.loads(self.backup.read_text());text=envelope['ciphertext'];envelope['ciphertext']=('A' if text[0]!='A' else 'B')+text[1:];self.backup.write_text(json.dumps(envelope))
        with self.assertRaises(InvalidTag):restore(self.backup,self.root/'bad',self.password)

if __name__=='__main__':unittest.main()
