import java.rmi.RemoteException;
import java.util.LinkedList;


public class ServerObject {

    private enum SState {
        RLT,
        WLT,
        NL,
    };

    private class Invalider implements Runnable {
        private Client_itf _clt;
        private int _id;

        public Invalider(Client_itf clt, int id) {
            _clt = clt;
        }

        public void run() {
            try {
                _clt.invalidate_reader(_id);
            }
            catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private Object _obj;
    private int _id;
    private SState _state;
    private LinkedList<Client_itf> _locks;

    public ServerObject(int id, Object obj) {
        _obj = obj;
        _id = id;
        _state = SState.NL;
        _locks = new LinkedList<Client_itf>();
    }

    public int getId() {
        return _id;
    }

    public Object get_obj() {
        return _obj;
    }

    public synchronized void lock_read(Client_itf c) {
        assert (c != null);

        if (_state == SState.WLT) {
            assert (_locks.size() == 1);
            try {
                if (!_locks.getFirst().equals(c)) {
                    _obj = _locks.getFirst().reduce_lock(_id);
                }
            }
            catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        _state = SState.RLT;

        if (!_locks.contains(c)) {
            _locks.add(c);
        }
    }

    public synchronized void lock_write(Client_itf clt) {
        assert (clt != null);

        if (_state == SState.WLT) {
            assert (_locks.size() == 1);
            assert (!_locks.getFirst().equals(clt));
            try {
                _obj = _locks.getFirst().invalidate_writer(_id);
            }
            catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        else if (_state == SState.RLT) {
            LinkedList<Thread> invaliders = new LinkedList<Thread>();
            for (Client_itf c: _locks) {
                if (!c.equals(clt)) {
                    Thread inv = new Thread(new Invalider(c, _id));
                    inv.start();
                    invaliders.push(inv);
                }
            }
            for (Thread inv: invaliders) {
                try {
                    inv.join();
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        _locks.clear();
        _locks.add(clt);
        _state = SState.WLT;
    }
}
