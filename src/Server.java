import java.rmi.RemoteException;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

import java.net.MalformedURLException;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;


public class Server extends UnicastRemoteObject implements Server_itf {

    static final long serialVersionUID = 20151227171455L;

    private HashMap<Integer, ServerObject> s_objs;
    private HashMap<String, Integer> hm_name;
    private ReentrantLock mutex;
    private int cpt;

    public Server() throws RemoteException {
        hm_name = new HashMap<String, Integer>();
        s_objs = new HashMap<Integer, ServerObject>();
        mutex = new ReentrantLock();
        cpt = -1;
    }

    public int lookup(String name) throws java.rmi.RemoteException {
        mutex.lock();
        Integer id = hm_name.get(name);
        mutex.unlock();

        return (id == null) ? -1 : id;
    }

    public void register(String name, int id) throws java.rmi.RemoteException {
        mutex.lock();
        hm_name.put(name, id);
        mutex.unlock();
    }

    public int create(Object o) throws java.rmi.RemoteException {
        mutex.lock();
        do {
            cpt++;
        } while(s_objs.get(cpt) != null);
        ServerObject s_obj = new ServerObject(cpt, o);
        s_objs.put(cpt, s_obj);
        mutex.unlock();

        return cpt;
    }

    public Object lock_read(int id, Client_itf client) throws java.rmi.RemoteException {
        mutex.lock();
        ServerObject obj = s_objs.get(id);
        mutex.unlock();
        obj.lock_read(client);
        return obj.getObj();
    }

    public Object lock_write(int id, Client_itf client) throws java.rmi.RemoteException {
        mutex.lock();
        ServerObject obj = s_objs.get(id);
        mutex.unlock();
        obj.lock_write(client);
        return obj.getObj();
    }
}
