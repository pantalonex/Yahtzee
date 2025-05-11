package client;

import auth.CryptoUtils;
import network.Message;
import game.ScoreCard;

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
        // Creazione di BufferedReader per input utente e connessione al server
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        boolean authenticated = false;
        String user;
        String pass;

        // Ciclo autenticazione
        while (!authenticated) {
            try {
                // Menu di login
                System.out.println("1) Login   2) Registrazione   3) Esci");
                System.out.print("Scegli (1-3): ");
                String choice = br.readLine().trim();

                if ("3".equals(choice)) {
                    System.out.println("Uscita... A presto!");
                    return;
                }
                if (!"1".equals(choice) && !"2".equals(choice)) {
                    System.out.println("Scelta non valida.");
                    continue;
                }
                boolean isRegister = "2".equals(choice);

                // Richiesta credenziali all'utente
                System.out.print("Username: ");
                user = br.readLine().trim();
                System.out.print("Password: ");
                pass = br.readLine().trim();
                try (Socket socket = new Socket(HOST, PORT);
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())) {

                    // Attesa chiave RSA inviata dal server
                    Message msg = (Message) in.readObject();
                    if (msg.type != Message.Type.PUBLIC_KEY)
                        throw new IllegalStateException("Expected PUBLIC_KEY");
                    // Decodifica Base64 dei byte della chiave
                    byte[] pubBytes = Base64.getDecoder().decode((String) msg.data);
                    PublicKey serverPub = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(pubBytes));
                    
                    // Inizializza il modulo RSA
                    CryptoUtils.initRSA();

                    // Cripta le credenziali con la chiave pubblica del server
                    String encCred = CryptoUtils.encryptRSA(user + ":" + pass, serverPub);
                    Message.Type authType = isRegister ? Message.Type.REGISTER : Message.Type.LOGIN;
                    out.writeObject(new Message(authType, encCred));
                    out.flush();

                    // Risposta dell'autenticazione (NEXT_TURN se autenticato)
                    msg = (Message) in.readObject();
                    if (msg.type != Message.Type.NEXT_TURN) {
                        System.out.println((isRegister ? "Registrazione" : "Login") + " fallito.");
                        continue;
                    }
                    System.out.println("Login riuscito. Benvenuto, " + user + "!");

                    authenticated = true;

                    // Riceve chiave AES e IV per cifratura simmetrica
                    msg = (Message) in.readObject();
                    if (msg.type != Message.Type.AES_KEY)
                        throw new IllegalStateException("Expected AES_KEY");
                    String[] parts    = ((String) msg.data).split(":", 2);
                    byte[] keyBytes   = Base64.getDecoder().decode(parts[0]);
                    byte[] ivBytes    = Base64.getDecoder().decode(parts[1]);
                    // Configura AES
                    CryptoUtils crypto = new CryptoUtils(keyBytes, ivBytes);
                    // Conferma al server di aver ricevuto lo stato
                    out.writeObject(new Message(Message.Type.STATE, "OK"));
                    out.flush();

                    // Selezione modalità di gioco
                    System.out.println("1) Singleplayer   2) Multiplayer");
                    System.out.print("Scegli modalità (1-2): ");
                    boolean isMulti = br.readLine().trim().equals("2");
                    String mode = isMulti ? "MULTI" : "SINGLE";
                    out.writeObject(new Message(Message.Type.MODE, crypto.encryptAES(mode)));
                    out.flush();

                    // Attende il primo messaggio utile per iniziare
                    Message first;
                    ScoreCard dummy = new ScoreCard();

                    while (true) {
                        first = (Message) in.readObject();

                        if (!(first.type == Message.Type.NEXT_TURN ||
                            first.type == Message.Type.STATE)) {
                            processGameMessage(first, dummy, in, out, br, user);
                            continue;
                        }

                        break;
                    }

                    // Avvia la sessione di gioco
                    if (isMulti)
                        runMultiplayer(in, out, br, first, user);
                    else
                        runSingleplayer(in, out, br, first, user);

                } catch (EOFException eof) {
                    System.out.println("Login fallito. Riprova.\n");
                    
                }
            } catch (Exception e) {
            System.out.println("Errore durante l'autenticazione: " + e.getMessage());
            e.printStackTrace();
            continue;
            }
        }
    }

    // Singleplayer
    private static void runSingleplayer(ObjectInputStream in, ObjectOutputStream out, BufferedReader br, Message firstMsg, String user) throws Exception {
        ScoreCard card = new ScoreCard(); // Carta dei punteggi del giocatore
        
        processGameMessage(firstMsg, card, in, out, br, user); // Processa il primo messaggio ricevuto
        while (true) { // Ciclo principale finché il gioco non termina
            Message msg = (Message) in.readObject();
            if (!processGameMessage(msg, card, in, out, br, user)) return;
        }
    }

    // Multiplayer (Riutilizzo Singleplayer)
    private static void runMultiplayer(ObjectInputStream in, ObjectOutputStream out, BufferedReader br, Message firstMsg, String user) throws Exception {
        runSingleplayer(in, out, br, firstMsg, user);
    }

    // Gestione messaggi dal server
    private static boolean processGameMessage(Message msg, ScoreCard card, ObjectInputStream in, ObjectOutputStream out, BufferedReader br, String user) throws Exception {
        switch (msg.type) {
             // Inizio turno
            case NEXT_TURN: {
                int turn = (int) msg.data;
                purgeStdin(br);
                //br.readLine();
                System.out.println("--- Turno " + turn + " ---");
                System.out.println("Premi invio per tirare i dadi...");
                br.readLine();
                out.writeObject(new Message(Message.Type.ROLL, null));
                out.flush();
                return true;
            }
            // Stato del gioco
            case STATE: {
                Object[] st   = (Object[]) msg.data;
                int[]  dice   = (int[]) st[0];
                int    total  = (int)    st[1];
                int    rollNr = (int)    st[2];
                boolean canRoll = (boolean) st[3];

                purgeStdin(br);
                System.out.println("Tiro #" + rollNr);
                System.out.println("Dadi: " + Arrays.toString(dice));
                card.displayPossible(dice);
                System.out.println("Punteggio parziale: " + total);
                boolean check = true;
                while(check){
                    if (canRoll) {
                        System.out.println("1) Tieni 2) Ritira 3) Salva 4) Esci");
                        String choice = br.readLine().trim();
                        switch (choice) {
                            case "1": // Scelta dei dadi da tenere
                                System.out.print("Quali (es.1 3): ");
                                String[] idxs = br.readLine().trim().split("\\s+");
                                int[] keep = new int[5];
                                for (String i : idxs) keep[Integer.parseInt(i) - 1] = 1;
                                out.writeObject(new Message(Message.Type.KEEP, keep));
                                out.flush();
                                out.writeObject(new Message(Message.Type.ROLL, null));
                                out.flush();
                                check = false;
                                break;
                            case "2": // Ritiro di tutti i dadi
                                out.writeObject(new Message(Message.Type.KEEP, new int[5]));
                                out.flush();
                                out.writeObject(new Message(Message.Type.ROLL, null));
                                out.flush();
                                check = false;
                                break;
                            case "3": // Salvataggio dei punteggi
                                salvaCategoria(card, dice, br, out, in);
                                check = false;
                                break;
                            case "4":
                                out.writeObject(new Message(Message.Type.OVER, user));
                                out.flush();
                                check = false;
                                System.out.println("Uscita... A presto!");
                                return false;
                        }
                    } else { // Se terzo tiro
                        purgeStdin(br);
                        System.out.println("1) Salva 2) Esci");
                        if ("1".equals(br.readLine().trim())) {
                            salvaCategoria(card, dice, br, out, in);
                            check = false;
                            break;
                        } else if ("2".equals(br.readLine().trim())) {
                            out.writeObject(new Message(Message.Type.OVER, user));
                            out.flush();
                            check = false;
                            System.out.println("Uscita... A presto!");
                            return false;
                        }
                    }
                }
                check = true;    
                return true;
            }
            case OVER: //Fine partita
                System.out.println("Gioco finito! Punteggio totale: " + msg.data);
                out.writeObject(new Message(Message.Type.OVER, user));
                out.flush();
                return false;
            case OPPONENT: { // Giocatore avversario
                System.out.println("Avversario trovato: " + (String) msg.data);
                purgeStdin(br);       
                return true;
            }
            
            case WAIT: { // Attesa di un giocatore
                System.out.println(msg.data);
                purgeStdin(br);       
                return true;
            }
            
            case OPP_ROLL: { //Tiri dell'avversario
                System.out.println("(Avversario) dadi: " +
                Arrays.toString((int[]) msg.data));
                purgeStdin(br);       
                return true;
            }
            
            case OPP_SAVE: { // Dadi salvati dall'avversario
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

    // Salvataggio del punteggio
    private static void salvaCategoria(ScoreCard card, int[] dice, BufferedReader br, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException {
        while (true) {
            try {
                System.out.print("Categoria (1-13): ");
                int cat = Integer.parseInt(br.readLine().trim());
                if (cat < 1 || cat > 13) {
                    System.err.println("Numero non valido. Inserisci un valore tra 1 e 13.");
                    continue;
                }
                out.writeObject(new Message(Message.Type.SAVE, cat));
                out.flush();

                while (true) {
                    Message resp = (Message) in.readObject();
                    
                    if (resp.type == Message.Type.SAVE) {
                        int score = (int) resp.data;
                        card.setScore(cat, score);
                        System.out.println("Punteggio assegnato: " + score);
                        System.out.println("Punteggio totale provvisorio: " + card.total());
                        System.out.println("Premi invio per continuare...");
                        br.readLine();
                        return;
                    } else {
                        System.err.println("Errore: " + resp.data);
                        break;
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("Input non valido. Inserisci un numero intero da 1 a 13.");
            }
        }
    }

    // Pulizia buffer input
    private static void purgeStdin(BufferedReader br) throws IOException {
        try {
            while (br.ready()) {
                br.readLine();
            }
            while (System.in.available() > 0) {
                System.in.read();
            }
        } catch (IOException e) {
        }
    }
}
