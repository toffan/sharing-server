import java.io.Serializable;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;


public class SharedObject implements Serializable, SharedObject_itf {

    static final long serialVersionUID = 20151227171325L;

    public static enum CState_t {
        NL, // no local lock
        RLC, // read lock cached
        WLC, // write lock cached
        RLT, // read lock taken
        WLT, // write lock taken
        RLT_WLC, // read lock taken and write lock cached
    }

    private int _id;

    // Ugly but compulsory...
    public Object obj;

    private CState_t _state;
    private ReentrantLock _mutex;
    private Condition _unlock;

    public SharedObject(int id) {
        _id = id;

        _state = CState_t.NL;
        _mutex = new ReentrantLock();
        _unlock = _mutex.newCondition();

        obj = null;
    }

    // invoked by the user program on the client node
    public void lock_read() {
        _mutex.lock();

        assert(_state != CState_t.RLT);
        assert(_state != CState_t.WLT);
        assert(_state != CState_t.RLT_WLC);

        if (_state == CState_t.NL) {
            // Ask for a lock
            obj = Client.lock_read(_id);
        }

        _state = (_state == CState_t.WLC) ? CState_t.RLT_WLC : CState_t.RLT;

        _mutex.unlock();
    }

    // invoked by the user program on the client node
    public void lock_write() {
        _mutex.lock();

        assert(_state != CState_t.RLT);
        assert(_state != CState_t.WLT);
        assert(_state != CState_t.RLT_WLC);


        if (_state == CState_t.NL) {
            // Ask for a lock
            obj = Client.lock_write(_id);
        }

        _state = CState_t.WLT;

        _mutex.unlock();
    }

    // invoked by the user program on the client node
    public synchronized void unlock() {
        _mutex.lock();
        assert(_state != CState_t.NL);
        assert(_state != CState_t.RLC);
        assert(_state != CState_t.WLC);

        if (_state == CState_t.RLT) {
            _state = CState_t.RLC;
        }
        else { // _state == WLT || _state == CState_t.RLT_WLC
            _state = CState_t.WLC;
        }

        _unlock.signal();
        _mutex.unlock();

    }


    // callback invoked remotely by the server
    public synchronized Object reduce_lock() {
        _mutex.lock();
        assert(_state != CState_t.NL);
        assert(_state != CState_t.RLC);
        assert(_state != CState_t.RLT);

        while(_state == CState_t.WLT) {
            _unlock.awaitUninterruptibly();
        }

        assert(_state == CState_t.RLT_WLC || _state == CState_t.WLC);

        _state = (_state == CState_t.RLT_WLC) ? CState_t.RLT : CState_t.RLC;
        _mutex.unlock();

        return obj;
    }

    // callback invoked remotely by the server
    public synchronized void invalidate_reader() {
        _mutex.lock();
        assert(_state != CState_t.NL);
        assert(_state != CState_t.WLT);
        assert(_state != CState_t.WLC);
        assert(_state != CState_t.RLT_WLC);

        while (_state == CState_t.RLT) {
            _unlock.awaitUninterruptibly();
        }

        assert(_state == CState_t.RLC);
        _mutex.unlock();

        _state = CState_t.NL;
    }

    public synchronized Object invalidate_writer() {
        _mutex.lock();
        assert(_state != CState_t.NL);
        assert(_state != CState_t.RLT);
        assert(_state != CState_t.RLC);

        while (_state == CState_t.WLT || _state == CState_t.RLT_WLC) {
            _unlock.awaitUninterruptibly();
        }

        assert(_state == CState_t.WLC);

        _state = CState_t.NL;
        _mutex.unlock();

        return obj;
    }

    public int get_id() {
        return _id;
    }
}
