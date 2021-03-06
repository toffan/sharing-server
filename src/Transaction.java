import java.util.HashMap;
import java.util.Map.Entry;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import java.lang.RuntimeException;


public class Transaction {

    // Required for friendship with Client
    public static final class Fingerprint { private Fingerprint() {} }
    private static final Fingerprint fingerprint = new Fingerprint();

    private static Transaction current = null;
    private HashMap<Integer, Object> _init;
    private HashMap<Integer, Boolean> _locked;
    private boolean _active;


    public Transaction() {
        _init = new HashMap<Integer, Object>();
        _locked = new HashMap<Integer, Boolean>();
        _active = false;
    }

    public static Transaction getCurrentTransaction() {
        return current;
    }

    // indique si l'appelant est en mode transactionnel
    public boolean isActive() {
        return _active;
    }

    // demarre une transaction (passe en mode transactionnel)
    public void start() {
        assert (current == null);
        assert (_active == false);
        current = this;
        _active = true;
    }

    // termine une transaction et passe en mode non transactionnel
    public boolean commit() {
        assert (current != null);
        assert (_active == true);
        _active = false;
        for (Entry<Integer, Boolean> entry: _locked.entrySet()) {
            Client.get_obj(fingerprint, entry.getKey()).unlock();
        }
        _init.clear();
        _locked.clear();
        current = null;
        return true;
    }

    // abandonne et annule une transaction (et passe en mode non transactionnel)
    public void abort() {
        assert (current != null);
        assert (_active == true);
        _active = false;
        for (Entry<Integer, Object> entry: current._init.entrySet()) {
            Client.get_obj(fingerprint, entry.getKey()).obj = entry.getValue();
        }
        _init.clear();
        _locked.clear();
        current = null;
    }

    public void push(SharedObject.Fingerprint fingerprint, SharedObject so) {
        if (_init.get(so.get_id()) == null) {
            _init.put(so.get_id(), deepcopy(so.obj));
        }
    }

    public void lock(SharedObject.Fingerprint fingerprint, SharedObject so) {
        assert (_locked.get(so.get_id()) != true);
        _locked.put(so.get_id(), true);
    }

    public void unlock(SharedObject.Fingerprint fingerprint, SharedObject so) {
        assert (_locked.get(so.get_id()) == true);
        _locked.put(so.get_id(), false);
    }

    public Boolean locked(SharedObject so) {
        return _locked.get(so.get_id());
    }


    public static Object deepcopy(Object orig) {
        Object copy = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(bos);
            os.writeObject(orig);
            os.close();

            ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
            copy = is.readObject();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return copy;
    }
}
