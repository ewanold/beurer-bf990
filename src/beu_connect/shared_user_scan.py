"""Try the owner's shared code on indices 1–12; failed consent is inconclusive."""
import json
import sys
from datetime import datetime,timezone
from .app import BlueZ,select_device
from .multi_user_test import verify_user
from .setup_profile import ROOT,Session,check_state,load_profile


def scan(session,code,emit):
    accessible=[];inaccessible=[]
    for index in range(1,13):
        try:session.control(2,bytes([index])+code.to_bytes(2,'little'))
        except RuntimeError as exc:
            if 'rejected with result' not in str(exc):raise
            emit('index_inaccessible',index=index,detail=str(exc));inaccessible.append(index)
            print(f'Index {index}: not accessible with shared code.',flush=True);continue
        if session.read('181c','2a9a')!=bytes([index]):raise RuntimeError('Selected index differs from consent request')
        birth=session.read('181c','2a85');height=session.read('181c','2a8e');sex=session.read('181c','2a8c')
        emit('index_accessible',index=index,birth_hex=birth.hex(),height_hex=height.hex(),sex_hex=sex.hex())
        accessible.append(index);print(f'Index {index}: authorized and profile read.',flush=True)
    return accessible,inaccessible


def main():
    root=ROOT/'database';path=root/'profile-state.json';lock=path.with_name(path.name+'.lock');acquired=False;backend=session=None
    try:
        lock.mkdir();acquired=True
        original=json.loads(path.read_text());profile=load_profile(root/'profile-input.json');check_state(original,original['address'],profile)
        if original.get('phase')!='configured' or original.get('user_index')!=1:raise RuntimeError('Original user 1 not configured')
        from dbus.mainloop.glib import DBusGMainLoop
        DBusGMainLoop(set_as_default=True)
        backend=BlueZ('hci0',35);print('Connecting to scan indices 1–12; keep the scale awake.',flush=True)
        device=select_device(backend.discover(3),name='BF990')
        if device['connected']:raise RuntimeError('Scale is connected elsewhere')
        check_state(original,device['address'],profile);backend.connect(device['path'])
        capture=root/'captures'/datetime.now(timezone.utc).strftime('shared-user-scan-%Y%m%dT%H%M%S-%fZ.jsonl');capture.parent.mkdir(parents=True,exist_ok=True)
        with capture.open('x') as out:
            def emit(event,**fields):out.write(json.dumps({'event':event,'at':datetime.now(timezone.utc).isoformat(),**fields})+'\n');out.flush()
            session=Session(backend,device,emit)
            if session.read('180a','2a24').rstrip(b'\x00')!=b'BF990':raise RuntimeError('Live model verification failed')
            session.subscribe('181c','2a9f')
            try:scan(session,original['consent_code'],emit)
            finally:verify_user(session,original,profile,1);print('Original user 1 verified again.',flush=True)
        print('Private capture:',capture,flush=True);return 0
    except Exception as exc:print('Scan stopped:',str(exc),file=sys.stderr,flush=True);return 1
    finally:
        if session:session.close()
        if backend:backend.close()
        if acquired:lock.rmdir()

if __name__=='__main__':raise SystemExit(main())
