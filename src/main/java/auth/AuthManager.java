package auth;

public class AuthManager {
    private final Store store;

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
            return store.login(u, p);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
