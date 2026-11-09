import unittest
from beu_connect.shared_user_scan import scan
class Fake:
    def control(self,opcode,payload):
        self.index=payload[0]
        if self.index not in (1,2):raise RuntimeError('UDS operation 0x02 rejected with result 0x05')
    def read(self,service,field):return {'2a9a':bytes([self.index]),'2a85':b'1234','2a8e':b'12','2a8c':b'1'}[field]
class Tests(unittest.TestCase):
    def test_inaccessible_indices_are_not_assumed_empty(self):
        events=[];a,b=scan(Fake(),123,lambda name,**kwargs:events.append(name))
        self.assertEqual(a,[1,2]);self.assertEqual(b,list(range(3,13)));self.assertEqual(events.count('index_inaccessible'),10)
