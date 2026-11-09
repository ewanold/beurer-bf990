"""Check selected-user behavior after one failed disposable-user authorization."""
import json
import sys
from datetime import datetime, timezone
from .app import BlueZ, select_device
from .multi_user_test import PROFILES, verify_user
from .setup_profile import ROOT, Session, check_state, load_profile


def main():
    root=ROOT / 'database'
    paths=[root / 'profile-state.json', *(root / f'multi-user-test-{i}-state.json' for i in (2,3))]
    locks=[p.with_name(p.name+'.lock') for p in paths]
    acquired=[]; backend=session=None
    try:
        for lock in locks: lock.mkdir();acquired.append(lock)
        original,test2,test3=[json.loads(p.read_text()) for p in paths]
        profile1=load_profile(root/'profile-input.json')
        for state,profile,index in ((original,profile1,1),(test2,PROFILES[2],2),(test3,PROFILES[3],3)):
            check_state(state,original['address'],profile)
            if state.get('phase')!='configured' or state.get('user_index')!=index: raise RuntimeError('Saved profile state is not configured')
        from dbus.mainloop.glib import DBusGMainLoop
        DBusGMainLoop(set_as_default=True)
        backend=BlueZ('hci0',35)
        print('Connecting for disposable-user authorization probe; keep the scale awake.',flush=True)
        device=select_device(backend.discover(3),name='BF990')
        if device['connected']:raise RuntimeError('Scale is connected elsewhere')
        check_state(original,device['address'],profile1)
        backend.connect(device['path'])
        path=root/'captures'/datetime.now(timezone.utc).strftime('unauthorized-probe-%Y%m%dT%H%M%S-%fZ.jsonl')
        path.parent.mkdir(parents=True,exist_ok=True)
        with path.open('x') as output:
            def emit(event,**fields):output.write(json.dumps({'event':event,'at':datetime.now(timezone.utc).isoformat(),**fields})+'\n');output.flush()
            session=Session(backend,device,emit)
            if session.read('180a','2a24').rstrip(b'\x00')!=b'BF990':raise RuntimeError('Live model verification failed')
            session.subscribe('181c','2a9f')
            try:
                verify_user(session,test2,PROFILES[2],2)
                wrong=(test3['consent_code']+1)%10000
                try:
                    session.control(2,bytes([3])+wrong.to_bytes(2,'little'))
                    emit('incorrect_code_result',result='accepted')
                    print('Unexpected: incorrect authorization was accepted.',flush=True)
                except RuntimeError as exc:
                    if 'rejected with result' not in str(exc): raise
                    emit('incorrect_code_result',result='rejected',detail=str(exc))
                    print('Incorrect authorization for disposable user 3 was rejected:',str(exc),flush=True)
                selected=session.read('181c','2a9a')
                emit('selected_after_failure',raw_hex=selected.hex())
                print('Selection after failed authorization:', 'user 2' if selected==b'\x02' else 'user 3' if selected==b'\x03' else 'none' if selected==b'\xff' else 'other/unknown',flush=True)
                for field in ('2a85','2a8e','2a8c'):
                    try:
                        value=session.read('181c',field)
                        emit('profile_read_after_failure',field=field,raw_hex=value.hex())
                        print(f'Profile {field} after failed authorization: read succeeded ({len(value)} bytes).',flush=True)
                    except Exception as exc:
                        emit('profile_read_after_failure',field=field,error=type(exc).__name__)
                        print(f'Profile {field} after failed authorization: read failed.',flush=True)
            finally:
                verify_user(session,original,profile1,1)
                print('Original user 1 reauthorized and verified.',flush=True)
        print('Private capture:',path,flush=True)
        return 0
    except Exception as exc:
        print(f'Probe stopped: {exc}',file=sys.stderr,flush=True);return 1
    finally:
        if session:session.close()
        if backend:backend.close()
        for lock in reversed(acquired):lock.rmdir()

if __name__=='__main__':raise SystemExit(main())
