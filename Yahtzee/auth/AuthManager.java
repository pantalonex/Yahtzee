package auth;

import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {
    private ConcurrentHashMap<String,String> users = new ConcurrentHashMap<>();

    public synchronized boolean register(String u, String p) {
        return users.putIfAbsent(u,p)==null;
    }
    
    public boolean login(String u, String p) {
        return p.equals(users.get(u));
    }
}
