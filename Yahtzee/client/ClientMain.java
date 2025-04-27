// client/ClientMain.java
package client;

import auth.CryptoUtils;
import network.Message;
import game.ScoreCard;
import game.ScoreCalculator;

import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Arrays;

public class ClientMain {
    private static final String HOST = "127.0.0.1";
    private static final int    PORT = 55555;

    public static void main(String[] args) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
             Socket socket     = new Socket(HOST, PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())) {

            System.out.println("1) Login   2) Registrazione   3) Esci");
            System.out.print("Scegli (1-3): ");
            String choice = br.readLine().trim();
            if ("3".equals(choice)) {
                System.out.println("Uscita... A presto!");
                return;
            }
            if (!"1".equals(choice) && !"2".equals(choice)) {
                System.out.println("Scelta non valida.");
                return;
            }
            boolean isRegister = "2".equals(choice);
            System.out.print("Username: ");
            String user = br.readLine().trim();
            System.out.print("Password: ");
            String pass = br.readLine().trim();

            Message msg = (Message) in.readObject();
            if (msg.type != Message.Type.PUBLIC_KEY)
                throw new IllegalStateException("Expected PUBLIC_KEY");
            byte[] pubBytes = Base64.getDecoder().decode((String) msg.data);
            PublicKey serverPub = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(pubBytes));
            CryptoUtils.initRSA();

            String encCred = CryptoUtils.encryptRSA(user + ":" + pass, serverPub);
            Message.Type authType = isRegister ? Message.Type.REGISTER : Message.Type.LOGIN;
            out.writeObject(new Message(authType, encCred));

            msg = (Message) in.readObject();
            if (msg.type != Message.Type.NEXT_TURN) {
                System.out.println((isRegister ? "Registrazione" : "Login") + " fallito.");
                return;
            }
            System.out.println("Login riuscito. Benvenuto, " + user + "!");

            msg = (Message) in.readObject();
            if (msg.type != Message.Type.AES_KEY)
                throw new IllegalStateException("Expected AES_KEY");
            String[] parts = ((String) msg.data).split(":", 2);
            byte[] keyBytes = Base64.getDecoder().decode(parts[0]);
            byte[] ivBytes  = Base64.getDecoder().decode(parts[1]);
            CryptoUtils.setAesKey(new SecretKeySpec(keyBytes, "AES"));
            CryptoUtils.setIv(new IvParameterSpec(ivBytes));
            out.writeObject(new Message(Message.Type.STATE, "OK"));

 
            System.out.println("1) Singleplayer   2) Multiplayer");
            System.out.print("Scegli modalità (1-2): ");
            boolean isMulti = br.readLine().trim().equals("2");
            String mode = isMulti ? "MULTI" : "SINGLE";
            out.writeObject(new Message(Message.Type.MODE, CryptoUtils.encryptAES(mode)));

            Message first;
            ScoreCard dummy = new ScoreCard();

            while (true) {
                first = (Message) in.readObject();

                if (!(first.type == Message.Type.NEXT_TURN ||
                    first.type == Message.Type.STATE)) {
                    processGameMessage(first, dummy, in, out, br);
                    continue;
                }

                break;
            }


            if (isMulti)
                runMultiplayer(in, out, br, first);
            else
                runSingleplayer(in, out, br, first);

        } catch (Exception e) {
            System.out.println("Errore: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runSingleplayer(ObjectInputStream in, ObjectOutputStream out, BufferedReader br, Message firstMsg) throws Exception {
        ScoreCard card = new ScoreCard();

        processGameMessage(firstMsg, card, in, out, br);
        while (true) {
            Message msg = (Message) in.readObject();
            if (!processGameMessage(msg, card, in, out, br)) return;
        }
    }

    private static void runMultiplayer(ObjectInputStream in, ObjectOutputStream out, BufferedReader br, Message firstMsg) throws Exception {
        runSingleplayer(in, out, br, firstMsg);
    }

    private static boolean processGameMessage(Message msg, ScoreCard card, ObjectInputStream in, ObjectOutputStream out, BufferedReader br) throws Exception {
        switch (msg.type) {
            case NEXT_TURN: {
                int turn = (int) msg.data;
                purgeStdin(br);
                System.out.println("--- Turno " + turn + " ---");
                System.out.println("Premi invio per tirare i dadi...");
                br.readLine();
                out.writeObject(new Message(Message.Type.ROLL, null));
                return true;
            }
            case STATE: {
                Object[] st   = (Object[]) msg.data;
                int[]  dice   = (int[]) st[0];
                int    total  = (int)    st[1];
                int    rollNr = (int)    st[2];
                boolean canRoll = (boolean) st[3];

                System.out.println("Tiro #" + rollNr);
                System.out.println("Dadi: " + Arrays.toString(dice));
                card.displayPossible(dice);
                System.out.println("Punteggio parziale: " + total);

                if (canRoll) {
                    System.out.println("1) Tieni 2) Ritira 3) Salva 4) Esci");
                    String choice = br.readLine().trim();
                    switch (choice) {
                        case "1":
                            System.out.print("Quali (es.1 3): ");
                            String[] idxs = br.readLine().trim().split("\\s+");
                            int[] keep = new int[5];
                            for (String i : idxs) keep[Integer.parseInt(i) - 1] = 1;
                            out.writeObject(new Message(Message.Type.KEEP, keep));
                            out.writeObject(new Message(Message.Type.ROLL, null));
                            break;
                        case "2":
                            out.writeObject(new Message(Message.Type.KEEP, new int[5]));
                            out.writeObject(new Message(Message.Type.ROLL, null));
                            break;
                        case "3":
                            salvaCategoria(card, dice, br, out);
                            break;
                        default:
                            out.writeObject(new Message(Message.Type.OVER, null));
                            return false;
                    }
                } else {
                    System.out.println("1) Salva 2) Esci");
                    if ("1".equals(br.readLine().trim())) {
                        salvaCategoria(card, dice, br, out);
                    } else {
                        out.writeObject(new Message(Message.Type.OVER, null));
                        return false;
                    }
                }
                return true;
            }
            case OVER:
                System.out.println("Gioco finito! Punteggio totale: " + msg.data);
                return false;
                case OPPONENT: {
                    System.out.println("Avversario trovato: " + (String) msg.data);
                    purgeStdin(br);       
                    return true;
                }
            
            case WAIT: {
                System.out.println((String) msg.data);
                purgeStdin(br);       
                return true;
            }
            
            case OPP_ROLL: {
                System.out.println("(Avversario) dadi: " +
                Arrays.toString((int[]) msg.data));
                purgeStdin(br);       
                return true;
            }
            
            case OPP_SAVE: {
                Object[] a = (Object[]) msg.data;
                System.out.println("L'avversario ha salvato la categoria " + a[0] +
                                   ". Punti avversario: " + a[1]);
                purgeStdin(br);       
                return true;
            }                           
            default:
                return true;
        }
    }

    private static void salvaCategoria(ScoreCard card, int[] dice, BufferedReader br, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        System.out.print("Categoria (1-13): ");
        int cat = Integer.parseInt(br.readLine().trim());
        int score = ScoreCalculator.score(dice, card.getCat(cat));
        out.writeObject(new Message(Message.Type.SAVE, cat));
        card.setScore(cat, score);
        System.out.println("Punteggio assegnato: " + score);
        System.out.println("Punteggio totale provvisorio: " + card.total());
        System.out.println("Premi invio per continuare...");
        br.readLine();
    }

    private static void purgeStdin(BufferedReader br) throws IOException {
        while (br.ready()) br.readLine();
    }
}
