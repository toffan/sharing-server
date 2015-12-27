import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Server extends UnicastRemoteObject implements Server_itf {

    private ArrayList<ServerObject> s_objs;
    private HashMap<String, ServerObject> hm_name;
    private ReentrantLock mutex;

    public Server() throws RemoteException {
        hm_name = new HashMap<String, ServerObject>();
        s_objs = new ArrayList<ServerObject>();
        mutex = new ReentrantLock();
    }

    public int lookup(String name) throws java.rmi.RemoteException {
        mutex.lock();
        ServerObject s_obj = hm_name.get(name);
        mutex.unlock();

        if(s_obj == null)
            return -1;
        else
            return s_obj.getId();
    }

    public void register(String name, int id) throws java.rmi.RemoteException {
        mutex.lock();
        hm_name.put(name, s_objs.get(id));
        mutex.unlock();
    }

    public int create(Object o) throws java.rmi.RemoteException {
        mutex.lock();
        int id = s_objs.size();
        ServerObject s_obj = new ServerObject(id, o);
        s_objs.add(s_obj);
        mutex.unlock();

        return id;
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
