package server;

import auth.AuthManager;
import auth.CryptoUtils;
import auth.Store;
import network.Message;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;

public class ServerMain {
    private static final int PORT = 55555;

    public static void main(String[] args) throws Exception {
        Store store = new Store();                 
        AuthManager authManager = new AuthManager(store);
        CryptoUtils.initRSA();

        try (ServerSocket ss = new ServerSocket(PORT)) {
            System.out.println("Server in ascolto sulla porta " + PORT);
            while (true) {
                Socket client = ss.accept();
                new ConnectionWorker(client, authManager).start();
            }
        }
    }

    static class ConnectionWorker extends Thread {
        private final Socket socket;
        private final AuthManager auth;

        ConnectionWorker(Socket socket, AuthManager auth) {
            this.socket = socket;
            this.auth   = auth;
        }

        @Override
        public void run() {
            ObjectOutputStream out = null;
            ObjectInputStream  in  = null;
            try {

                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();                       
                in  = new ObjectInputStream(socket.getInputStream());


                String pubB64 = Base64.getEncoder().encodeToString(CryptoUtils.getPublicKey().getEncoded());
                out.writeObject(new Message(Message.Type.PUBLIC_KEY, pubB64));

                Message authMsg  = (Message) in.readObject();
                String  creds    = CryptoUtils.decryptRSA((String) authMsg.data);
                String[] parts   = creds.split(":", 2);
                String user = parts[0];  
                boolean ok = authMsg.type == Message.Type.REGISTER
                           ? auth.register(parts[0], parts[1])
                           : auth.login   (parts[0], parts[1]);

                if (!ok) {
                    out.writeObject(new Message(Message.Type.ERROR, "Auth failed"));
                    socket.close();
                    return;
                }
                out.writeObject(new Message(Message.Type.NEXT_TURN, "Auth OK"));

                CryptoUtils.initAES();
                SecretKey aesKey   = CryptoUtils.getAesKey();
                IvParameterSpec iv = CryptoUtils.getIv();
                String aesPayload = Base64.getEncoder().encodeToString(aesKey.getEncoded()) + ":" +
                                    Base64.getEncoder().encodeToString(iv.getIV());
                out.writeObject(new Message(Message.Type.AES_KEY, aesPayload));

                in.readObject();

                Message modeMsg = (Message) in.readObject();
                String  mode    = CryptoUtils.decryptAES((String) modeMsg.data);

                ClientBundle bundle = new ClientBundle(socket, in, out, user);
                if ("MULTI".equalsIgnoreCase(mode)) {
                    Lobby.join(bundle);
                } else {
                    new Handler(bundle).start();
                }

            } catch (Exception e) {
                e.printStackTrace();
                try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception ignored) {}
            }
        }
    }
}
