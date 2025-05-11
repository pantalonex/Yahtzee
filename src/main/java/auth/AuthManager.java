package auth;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {
    private final Store store;

    private final static Set<String> loggedInUsers = ConcurrentHashMap.newKeySet();
    
    public AuthManager(Store store) {
        this.store = store;
    }

    public boolean register(String u, String p) {
        try {
            return store.register(u, p);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean login(String u, String p) {
        try {
            if (!store.login(u, p)) {
            return false;
            } else if (loggedInUsers.contains(u)) {
                return false;
            } else {
                return loggedInUsers.add(u);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean logout(String u) {
        try {
            if (loggedInUsers.contains(u)) {
                return loggedInUsers.remove(u);
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
