package server;

import java.util.LinkedList;
import java.util.Queue;

public class Lobby {
    private static final Queue<ClientBundle> queue = new LinkedList<>();

    public synchronized static void join(ClientBundle client) {
        queue.add(client);
        if (queue.size() >= 2) {
            ClientBundle c1 = queue.poll();
            ClientBundle c2 = queue.poll();
            new Thread(new MultiplayerSession(c1, c2)).start();
        }
    }
}
