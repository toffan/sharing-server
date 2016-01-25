import java.rmi.RemoteException;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

import java.net.MalformedURLException;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class Server extends UnicastRemoteObject implements Server_itf {

    static final long serialVersionUID = 20151227171455L;

    private HashMap<Integer, ServerObject> s_objs;
    private HashMap<String, Integer> hm_name;
    private ReentrantReadWriteLock mutex;
    private int cpt;

    public Server() throws RemoteException {
        hm_name = new HashMap<String, Integer>();
        s_objs = new HashMap<Integer, ServerObject>();
        mutex = new ReentrantReadWriteLock();
        cpt = -1;
    }

    public int lookup(String name) throws java.rmi.RemoteException {
        mutex.readLock().lock();
        Integer id = hm_name.get(name);
        mutex.readLock().unlock();

        return (id == null) ? -1 : id;
    }

    public void register(String name, int id) throws java.rmi.RemoteException {
        mutex.writeLock().lock();
        hm_name.put(name, id);
        mutex.writeLock().unlock();
    }

    public int create(Object o) throws java.rmi.RemoteException {
        mutex.writeLock().lock();
        do {
            cpt++;
        } while(s_objs.get(cpt) != null);
        ServerObject s_obj = new ServerObject(cpt, o);
        s_objs.put(cpt, s_obj);
        mutex.writeLock().unlock();

        return cpt;
    }

    public Object lock_read(int id, Client_itf client) throws java.rmi.RemoteException {
        mutex.readLock().lock();
        ServerObject obj = s_objs.get(id);
        mutex.readLock().unlock();
        obj.lock_read(client);
        return obj.get_obj();
    }

    public Object lock_write(int id, Client_itf client) throws java.rmi.RemoteException {
        mutex.readLock().lock();
        ServerObject obj = s_objs.get(id);
        mutex.readLock().unlock();
        obj.lock_write(client);
        return obj.get_obj();
    }

    public static void main(String[] args) {
        Server srv;
        System.out.println("Starting server...");

        try {
            System.out.println("Creating registry...");
            LocateRegistry.createRegistry(8000);

            System.out.println("Binding server...");
            srv = new Server();
            Naming.rebind("//localhost:8000/sharing-server", srv);
        }
        catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Server started !");
    }
}
