import java.io.Serializable;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;


public class SharedObject implements Serializable, SharedObject_itf {

    static final long serialVersionUID = 20160111161806L;

    // Required for friendship with Transaction
    public static final class Fingerprint { private Fingerprint() {} }
    private static final Fingerprint fingerprint = new Fingerprint();

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

        Transaction tr = Transaction.getCurrentTransaction();
        if (tr == null || !tr.isActive() || tr.locked(this) == null) {
            assert(_state != CState_t.RLT);
            assert(_state != CState_t.WLT);
            assert(_state != CState_t.RLT_WLC);

            if (_state == CState_t.NL) {
                // Ask for a lock
                obj = Client.lock_read(_id);
            }
            _state = (_state == CState_t.WLC) ? CState_t.RLT_WLC : CState_t.RLT;
        }
        if (tr != null) {
            tr.lock(fingerprint, this);
        }
        _mutex.unlock();
    }

    // invoked by the user program on the client node
    public void lock_write() {
        _mutex.lock();

        Transaction tr = Transaction.getCurrentTransaction();
        if (tr == null || !tr.isActive() || tr.locked(this) == null) {
            // Not in transaction or not locked
            assert(_state != CState_t.RLT);
            assert(_state != CState_t.WLT);
            assert(_state != CState_t.RLT_WLC);

            if (_state == CState_t.NL) {
                // Ask for a lock
                obj = Client.lock_write(_id);
            }
            _state = CState_t.WLT;
        }
        else  if (tr != null) {
            tr.push(fingerprint, this);
            tr.lock(fingerprint, this);
        }

        _mutex.unlock();
    }

    // invoked by the user program on the client node
    public synchronized void unlock() {
        assert(_state != CState_t.NL);
        assert(_state != CState_t.RLC);
        assert(_state != CState_t.WLC);

        Transaction tr = Transaction.getCurrentTransaction();
        if (tr != null && tr.isActive()) {
            // It's not a real unlock because we are in the middle of a
            // transaction
            tr.unlock(fingerprint, this);
        }
        else {
            if (_state == CState_t.RLT) {
                _state = CState_t.RLC;
            }
            else { // _state == WLT || _state == CState_t.RLT_WLC
                _state = CState_t.WLC;
            }
        }

        _unlock.signal();
    }


    // callback invoked remotely by the server
    public synchronized Object reduce_lock() {
        assert(_state != CState_t.NL);
        assert(_state != CState_t.RLC);
        assert(_state != CState_t.RLT);

        while(_state == CState_t.WLT) {
            _unlock.awaitUninterruptibly();
        }

        assert(_state == CState_t.RLT_WLC || _state == CState_t.WLC);

        _state = (_state == CState_t.RLT_WLC) ? CState_t.RLT : CState_t.RLC;

        return obj;
    }

    // callback invoked remotely by the server
    public synchronized void invalidate_reader() {
        assert(_state != CState_t.NL);
        assert(_state != CState_t.WLT);
        assert(_state != CState_t.WLC);
        assert(_state != CState_t.RLT_WLC);

        while (_state == CState_t.RLT) {
            _unlock.awaitUninterruptibly();
        }

        assert(_state == CState_t.RLC);

        _state = CState_t.NL;
    }

    public synchronized Object invalidate_writer() {
        assert(_state != CState_t.NL);
        assert(_state != CState_t.RLT);
        assert(_state != CState_t.RLC);

        while (_state == CState_t.WLT || _state == CState_t.RLT_WLC) {
            _unlock.awaitUninterruptibly();
        }

        assert(_state == CState_t.WLC);

        _state = CState_t.NL;

        return obj;
    }

    public int get_id() {
        return _id;
    }
}
