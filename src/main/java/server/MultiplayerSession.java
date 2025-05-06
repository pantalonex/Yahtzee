package server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import game.*;
import network.Message;

public class MultiplayerSession implements Runnable {
    private final ClientBundle c1, c2;

    public MultiplayerSession(ClientBundle c1, ClientBundle c2) {
    this.c1 = c1;
    this.c2 = c2;
}

    @Override
    public void run() {
        try {
            ObjectOutputStream out1 = c1.out();
            ObjectInputStream  in1  = c1.in();
            ObjectOutputStream out2 = c2.out();
            ObjectInputStream  in2  = c2.in();

            ScoreCard card1 = new ScoreCard();
            ScoreCard card2 = new ScoreCard();
            DiceRoller dr1 = new DiceRoller();
            DiceRoller dr2 = new DiceRoller();

            out1.writeObject(new Message(Message.Type.OPPONENT, c2.username()));
            out2.writeObject(new Message(Message.Type.OPPONENT, c1.username()));


            for (int turn = 1; turn <= 13; turn++) {
  
                out1.writeObject(new Message(Message.Type.NEXT_TURN, turn));
                out2.writeObject(new Message(Message.Type.WAIT,
                               "Tocca a " + c1.username()));
                playTurn(in1, out1, out2, dr1, card1);

                out2.writeObject(new Message(Message.Type.NEXT_TURN, turn));
                out1.writeObject(new Message(Message.Type.WAIT,
                               "Tocca a " + c2.username()));
                playTurn(in2, out2, out1, dr2, card2);
            }
            


            int score1 = card1.total();
            int score2 = card2.total();
            String result;
            if (score1 > score2) result = "Player 1 wins!";
            else if (score2 > score1) result = "Player 2 wins!";
            else result = "It's a tie!";


            out1.writeObject(new Message(Message.Type.OVER, result));
            out2.writeObject(new Message(Message.Type.OVER, result));

            c1.socket().close();
            c2.socket().close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playTurn(ObjectInputStream in,
        ObjectOutputStream out,
        ObjectOutputStream oppOut,
        DiceRoller dr,
        ScoreCard card) throws Exception {

        int rolls = 0;
        int[] dice = new int[5];
        int[] keep = new int[5];
        while (true) {

            Message req = (Message) in.readObject();
            if (req.type != Message.Type.ROLL) continue;

            dice = (rolls == 0) ? dr.rollAll() : dr.reroll(keep);
            rolls++;
            out.writeObject(new Message(Message.Type.STATE,
            new Object[]{dice.clone(), card.total(), rolls, rolls < 3}));
            oppOut.writeObject(new Message(Message.Type.OPP_ROLL, dice.clone()));

            Message act = (Message) in.readObject();
            if (act.type == Message.Type.KEEP) {
                keep = (int[]) act.data;
            } else if (act.type == Message.Type.SAVE) {
                int idx = (int) act.data;
                int sc  = ScoreCalculator.score(dice, card.getCat(idx));
                card.setScore(idx, sc);
                oppOut.writeObject(new Message(Message.Type.OPP_SAVE,
                new Object[]{idx, card.total()}));
                break;
            } else if (act.type == Message.Type.OVER) {
                return;
            }
        }
    }
}