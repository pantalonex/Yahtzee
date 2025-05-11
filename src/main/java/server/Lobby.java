package server;
import network.Message;

import java.util.LinkedList;
import java.util.Queue;

public class Lobby {
    private static final Queue<ClientBundle> queue = new LinkedList<>();

    public static synchronized void join(ClientBundle client) {
        queue.add(client);

        if (queue.size() == 1) {
            try {
                client.out().writeObject(
                    new Message(Message.Type.WAIT, "In attesa di un avversario...")
                );
                client.out().flush();
            } catch (Exception e) {
                e.printStackTrace();
                client.closeQuietly();
            }
            return;
        }

        ClientBundle c1 = queue.poll();
        ClientBundle c2 = queue.poll();

        new Thread(new MultiplayerSession(c1, c2)).start();
    }
}
