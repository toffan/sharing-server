import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.*;
import java.net.*;
import java.util.*;

public class Server extends UnicastRemoteObject implements Server_itf {

    private ArrayList<ServerObject> s_objs;
    private HashMap<String, ServerObject> hm_name;

    public Server() throws RemoteException {
        hm_name = new HashMap<String, ServerObject>();
        s_objs = new ArrayList<ServerObject>();
    }

    public int lookup(String name) throws java.rmi.RemoteException {
        ServerObject s_obj = hm_name.get(name);

        if(s_obj == null)
            return -1;
        else
            return s_obj.getId();
    }

    public void register(String name, int id) throws java.rmi.RemoteException {
        hm_name.put(name, s_objs.get(id));
    }

    public int create(Object o) throws java.rmi.RemoteException {
        int id = s_objs.size();
        ServerObject s_obj = new ServerObject(id, o);
        s_objs.add(s_obj);

        return id;
    }

    public Object lock_read(int id, Client_itf client) throws java.rmi.RemoteException {
        ServerObject obj = s_objs.get(id);
        obj.lock_read(client);
        return obj.getObj();
    }

    public Object lock_write(int id, Client_itf client) throws java.rmi.RemoteException {
        ServerObject obj = s_objs.get(id);
        obj.lock_write(client);
        return obj.getObj();
    }
}
