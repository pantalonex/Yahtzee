package server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public record ClientBundle(
        Socket socket,
        ObjectInputStream   in,
        ObjectOutputStream  out,
        String username) {

    public void closeQuietly() {
        try { 
            out.close(); 
        } catch (Exception ignore) {}
        try { 
            in.close();  
        } catch (Exception ignore) {}
        try { 
            socket.close(); 
        } catch (Exception ignore) {}
    }
}
