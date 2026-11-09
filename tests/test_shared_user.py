import unittest
from beu_connect.shared_user_test import delete_disposable
from beu_connect.multi_user_test import PROFILES
from beu_connect.setup_profile import profile_payloads

class Fake:
    def __init__(self):self.fields=profile_payloads(PROFILES[2],2);self.fields['2a9a']=b'\x02';self.ops=[]
    def read(self,service,field):return self.fields[field]
    def control(self,opcode,payload):
        self.ops.append(opcode)
        if opcode==3:self.fields['2a9a']=b'\xff'

class DeleteTests(unittest.TestCase):
    def test_persists_intent_before_deletion_and_result(self):
        state={'phase':'configured','user_index':2,'consent_code':123};saved=[];fake=Fake()
        delete_disposable(fake,state,PROFILES[2],2,lambda s:saved.append(dict(s)))
        self.assertEqual(fake.ops,[2,3]);self.assertEqual([s['phase'] for s in saved],['deletion_pending','deletion_acknowledged'])
    def test_unresolved_deletion_never_retries(self):
        fake=Fake()
        with self.assertRaises(RuntimeError):delete_disposable(fake,{'phase':'deletion_pending','user_index':2},PROFILES[2],2,lambda s:None)
        self.assertEqual(fake.ops,[])
    def test_refuses_to_delete_if_profile_differs(self):
        fake=Fake();fake.fields['2a85']=b'bad!'
        with self.assertRaises(RuntimeError):delete_disposable(fake,{'phase':'configured','user_index':2,'consent_code':123},PROFILES[2],2,lambda s:None)
        self.assertEqual(fake.ops,[2])
