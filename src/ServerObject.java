import java.rmi.RemoteException;
import java.util.LinkedList;


public class ServerObject {

    private enum SState {
        RLT,
        WLT,
        NL,
    };

    private class Invalider implements Runnable {
        private Client_itf _c;
        private int _id;

        public Invalider(Client_itf c, int id) {
            _c = c;
        }

        public void run() {
            try {
                _c.invalidate_reader(_id);
            }
            catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }



    private Object obj;
    private int id;
    private SState s_state;
    private LinkedList<Client_itf> c_locks;

    public ServerObject(int id, Object obj) {
        this.obj = obj;
        this.id = id;
        this.s_state = SState.NL;
        this.c_locks = new LinkedList<Client_itf>();
    }

    public int getId() {
        return id;
    }

    public Object getObj() {
        return this.obj;
    }

    public synchronized void lock_read(Client_itf c) {
        assert (c != null);

        if (s_state == SState.WLT) {
            assert (c_locks.size() == 1);
            try {
                if (!c_locks.getFirst().equals(c)) {
                    this.obj = c_locks.getFirst().reduce_lock(id);
                }
            }
            catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }

        s_state = SState.RLT;

        if (!c_locks.contains(c)) {
            c_locks.add(c);
        }
    }

    public synchronized void lock_write(Client_itf clt) {
        assert (clt != null);

        if (s_state == SState.WLT) {
            assert (c_locks.size() == 1);
            assert (!c_locks.getFirst().equals(clt));
            try {
                this.obj = c_locks.getFirst().invalidate_writer(id);
            }
            catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        else if (s_state == SState.RLT) {
            LinkedList<Thread> invaliders = new LinkedList<Thread>();
            for (Client_itf c: c_locks) {
                if (!c.equals(clt)) {
                    Thread inv = new Thread(new Invalider(c, id));
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

        c_locks.clear();
        c_locks.add(clt);
        s_state = SState.WLT;
    }
}
