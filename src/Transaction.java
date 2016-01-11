import java.util.HashMap;
import java.util.Map.Entry;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import java.lang.RuntimeException;


public class Transaction {

    public static final class Fingerprint { private Fingerprint() {} }
    private static final Fingerprint fingerprint = new Fingerprint();

    private static Transaction current = null;
    private HashMap<Integer, Object> _init;


    public Transaction() {
        _init = new HashMap<Integer, Object>();
    }

    public static Transaction getCurrentTransaction() {
        return current;
    }

    // indique si l'appelant est en mode transactionnel
    public boolean isActive() {
        return this == current;
    }

    // demarre une transaction (passe en mode transactionnel)
    public void start() {
        assert (current == null);
        current = this;
    }

    // termine une transaction et passe en mode non transactionnel
    public boolean commit() {
        assert (current != null);
        current = null;
        _init.clear();
        return true;
    }

    // abandonne et annule une transaction (et passe en mode non transactionnel)
    public void abort() {
        assert (current != null);
        for (Entry<Integer, Object> entry: current._init.entrySet()) {
            Client.get_obj(fingerprint, entry.getKey()).obj = entry.getValue();
        }
    }

    public void push(SharedObject so) {
        if (_init.get(so.get_id()) == null) {
            _init.put(so.get_id(), deepcopy(so.obj));
        }
    }

    public Object deepcopy(Object orig) {
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
