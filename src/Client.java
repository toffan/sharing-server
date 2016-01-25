import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

import java.net.MalformedURLException;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;


public class Client extends UnicastRemoteObject implements Client_itf {

    static final long serialVersionUID = 20160111151730L;

    private static Server_itf server;
    private static HashMap<Integer, SharedObject> objects;
    private static ReentrantLock mutex;
    private static Client shame;

    public Client() throws RemoteException {
        super();
    }

    public static SharedObject get_obj(Transaction.Fingerprint f, Integer i) {
        return objects.get(i);
    }


    ///////////////////////////////////////////////////
    //         Interface to be used by applications
    ///////////////////////////////////////////////////

    // initialization of the client layer
    public static void init() {
        try {
            server = (Server_itf) Naming.lookup("//localhost:8000/sharing-server");
        }
        catch (NotBoundException e) {
            throw new RuntimeException(e);
        }
        catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        try {
            shame = new Client();
        }
        catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        objects = new HashMap<Integer, SharedObject>();
        mutex = new ReentrantLock();
    }

    // lookup in the name server
    public static SharedObject lookup(String name) {
        int id = -1;
        try {
            id = server.lookup(name);
        }
        catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        if (id == -1) {
            return null;
        }

        assert(id != -1);

        mutex.lock();
        SharedObject so = null;
        if (objects.get(id) != null) {
            so = objects.get(id);
        }
        else {
            so = new SharedObject(id);
            objects.put(id, so);
        }
        mutex.unlock();

        return so;
    }

    // binding in the name server
    public static void register(String name, SharedObject_itf soi) {
        SharedObject so = (SharedObject) soi;
        try {
            server.register(name, so.get_id());
        }
        catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    // creation of a shared object
    public static SharedObject create(Object o) {
        int id = -1;
        try {
            id = server.create(o);
        }
        catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        SharedObject so = new SharedObject(id);

        mutex.lock();
        objects.put(id, so);
        mutex.unlock();

        return so;
    }

    /////////////////////////////////////////////////////////////
    //    Interface to be used by the consistency protocol
    ////////////////////////////////////////////////////////////

    // request a read lock from the server
    public static Object lock_read(int id) {
        try {
            return server.lock_read(id, shame);
        }
        catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    // request a write lock from the server
    public static Object lock_write(int id) {
        try {
            return server.lock_write(id, shame);
        }
        catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    // receive a lock reduction request from the server
    public Object reduce_lock(int id) throws java.rmi.RemoteException {
        return objects.get(id).reduce_lock();
    }

    // receive a reader invalidation request from the server
    public void invalidate_reader(int id) throws java.rmi.RemoteException {
        objects.get(id).invalidate_reader();
    }

    // receive a writer invalidation request from the server
    public Object invalidate_writer(int id) throws java.rmi.RemoteException {
        return objects.get(id).invalidate_writer();
    }
}
