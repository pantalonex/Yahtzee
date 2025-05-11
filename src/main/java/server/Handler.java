package server;

import game.*;
import network.Message;
import auth.AuthManager;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


public class Handler extends Thread {

    private final ObjectInputStream  in;
    private final ObjectOutputStream out;

    private final boolean isMulti;
    private ObjectOutputStream opponentOut;

    private final ScoreCard  card = new ScoreCard();
    private final DiceRoller dice = new DiceRoller();

    public Handler(ClientBundle bundle) {
        this(bundle, false, null);
    }

    public Handler(ClientBundle bundle,
                   boolean isMulti,
                   ObjectOutputStream opponentOut) {
        this.in = bundle.in();
        this.out = bundle.out();
        this.isMulti = isMulti;
        this.opponentOut = opponentOut;
    }

    public void setOpponent(ObjectOutputStream opp) { this.opponentOut = opp; }

    @Override
    public void run() {
        try {
            for (int turn = 1; turn <= 13; turn++) {

                send(out, Message.Type.NEXT_TURN, turn);
                if (isMulti && opponentOut != null)
                    send(opponentOut, Message.Type.STATE,
                    "Turno " + turn + " dell'avversario"
                );

                int rolls = 0;
                int[] keep = new int[5];
                int[] face = new int[5];

                boolean save = false;
                while (true) {
                    Message req = (Message) in.readObject();

                    switch (req.type) {

                        case OVER -> { 
                            String user = (String) req.data;
                            AuthManager.logout(user);
                            notifyGameOver(false); 
                            return; 
                        }

                        case ROLL -> {
                            face  = (rolls == 0) ? dice.rollAll()
                                                 : dice.reroll(keep);
                            rolls++;

                            Object[] state = { face, card.total(), rolls,
                                               rolls < 3 };
                            send(out, Message.Type.STATE, state);
                            if (isMulti && opponentOut != null)
                                send(opponentOut, Message.Type.STATE,
                                     "Avversario lancia: " +
                                     java.util.Arrays.toString(face));
                        }

                        case KEEP -> keep = (int[]) req.data;

                        case SAVE -> {
                            int idx   = (int) req.data;
                            if (!card.isEmpty(idx)) {
                                send(out, Message.Type.ERROR,
                                    "Hai già salvato " + card.getCat(idx) + ", riprova");
                            } else {
                                int score = ScoreCalculator.score(face, card.getCat(idx));
                                card.setScore(idx, score);
                                send(out, Message.Type.SAVE, score);

                                if (isMulti && opponentOut != null)
                                    send(opponentOut, Message.Type.STATE,
                                        "Avversario salva in cat " + idx + " per " + score + " punti");
                                save = true;
                                break;
                            }
                        }

                        default -> { }
                    }
                    if (save) break;
                }
            }

            notifyGameOver(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void send(ObjectOutputStream o, Message.Type t, Object data)
            throws Exception {
        o.writeObject(new Message(t, data));
        o.flush();
    }

    private void notifyGameOver(boolean normalEnd) throws Exception {
        int total = card.total();
        send(out, Message.Type.OVER, total);
        if (isMulti && opponentOut != null)
            send(opponentOut, Message.Type.OVER,
                normalEnd ? "L'avversario ha chiuso con " + total
                    : "L'avversario si è arreso"
        );
    }
}
