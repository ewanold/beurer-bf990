"""Replace disposable users with one using the existing household code."""
import json
import sys
from datetime import datetime,timezone
from .app import BlueZ,select_device
from .multi_user_test import PROFILES,verify_user
from .setup_profile import ROOT,Session,check_state,configure,fingerprint,load_profile,save_state


def delete_disposable(session,state,profile,index,persist):
    if state.get('phase')=='deletion_acknowledged':return
    if state.get('phase')!='configured' or state.get('user_index')!=index:
        raise RuntimeError(f'User {index} state is not safe for deletion; inspect recovery state')
    verify_user(session,state,profile,index)
    if session.read('181c','2a9a')!=bytes([index]):raise RuntimeError('Selected user changed; refusing deletion')
    state['phase']='deletion_pending';persist(state)
    session.control(3,b'')
    state['phase']='deletion_acknowledged';persist(state)
    if session.read('181c','2a9a')!=b'\xff':raise RuntimeError('Deletion acknowledged but selection did not clear')
    print(f'Deletion of disposable user {index} acknowledged.',flush=True)


def main():
    root=ROOT/'database';original_path=root/'profile-state.json';paths={i:root/f'multi-user-test-{i}-state.json' for i in (2,3)}
    shared_path=root/'multi-user-test-2-shared-state.json';locks=[p.with_name(p.name+'.lock') for p in [original_path,*paths.values(),shared_path]]
    acquired=[]; backend=session=None
    try:
        for lock in locks:lock.mkdir();acquired.append(lock)
        original=json.loads(original_path.read_text());profile1=load_profile(root/'profile-input.json')
        check_state(original,original['address'],profile1)
        if original.get('phase')!='configured' or original.get('user_index')!=1:raise RuntimeError('Original user 1 state is not configured')
        states={i:json.loads(path.read_text()) for i,path in paths.items()}
        for i,state in states.items():
            check_state(state,original['address'],PROFILES[i])
            if state.get('phase') not in ('configured','deletion_acknowledged'):raise RuntimeError(f'User {i} deletion outcome unresolved')
        if shared_path.exists():
            shared=json.loads(shared_path.read_text());check_state(shared,original['address'],PROFILES[2])
            if shared.get('consent_code')!=original['consent_code']:raise RuntimeError('Shared state does not match original user code')
        else:
            shared={'schema':1,'address':original['address'],'phase':'prepared','consent_code':original['consent_code'],'profile_fingerprint':fingerprint(PROFILES[2])}
            save_state(shared_path,shared)
        from dbus.mainloop.glib import DBusGMainLoop
        DBusGMainLoop(set_as_default=True)
        capture=root/'captures'/datetime.now(timezone.utc).strftime('shared-user-test-%Y%m%dT%H%M%S-%fZ.jsonl');capture.parent.mkdir(parents=True,exist_ok=True)
        with capture.open('x') as output:
            def emit(event,**fields):output.write(json.dumps({'event':event,'at':datetime.now(timezone.utc).isoformat(),**fields})+'\n');output.flush()
            for stage in ('delete','register'):
                backend=BlueZ('hci0',35);session=None
                try:
                    print(f'Connecting for {stage} stage; keep the scale awake.',flush=True)
                    device=select_device(backend.discover(3),name='BF990')
                    if device['connected']:raise RuntimeError('Scale is connected elsewhere')
                    check_state(original,device['address'],profile1);backend.connect(device['path']);session=Session(backend,device,emit)
                    if session.read('180a','2a24').rstrip(b'\x00')!=b'BF990':raise RuntimeError('Live model verification failed')
                    session.subscribe('181c','2a9f')
                    if stage=='delete':
                        verify_user(session,original,profile1,1)
                        try:
                            for i in (3,2):delete_disposable(session,states[i],PROFILES[i],i,lambda s,path=paths[i]:save_state(path,s))
                        finally:verify_user(session,original,profile1,1)
                    else:
                        configure(session,PROFILES[2],shared,lambda s:save_state(shared_path,s),emit,expected_index=2)
                        verify_user(session,shared,PROFILES[2],2)
                        try:
                            session.control(2,bytes([3])+original['consent_code'].to_bytes(2,'little'))
                            print('Index 3 accepted the shared code unexpectedly; inspect before making assumptions.',flush=True)
                            emit('deleted_index_auth',result='accepted')
                        except RuntimeError as exc:
                            if 'rejected with result' not in str(exc):raise
                            emit('deleted_index_auth',result='rejected',detail=str(exc))
                            print('Authorization of deleted index 3 was rejected:',str(exc),flush=True)
                        verify_user(session,original,profile1,1)
                        print('User 2 verified with the shared code; user 1 restored.',flush=True)
                finally:
                    if session:session.close();session=None
                    backend.close();backend=None
        print('Private capture:',capture,flush=True);return 0
    except Exception as exc:
        print(f'Test stopped: {exc}. Preserve all saved states; do not retry unresolved deletion or registration.',file=sys.stderr,flush=True);return 1
    finally:
        if session:session.close()
        if backend:backend.close()
        for lock in reversed(acquired):lock.rmdir()

if __name__=='__main__':raise SystemExit(main())
