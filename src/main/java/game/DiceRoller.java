package game;

public class DiceRoller {
    private final Dice[] dice = new Dice[5];
    
    public DiceRoller() { 
        for(int i=0; i<5; i++) dice[i] = new Dice(); 
    }

    public int[] rollAll() {
        for(Dice d: dice) d.roll();
        return values();
    }

    public int[] reroll(int[] keep) {
        for(int i=0; i<5; i++)
            if(keep[i]==0) dice[i].roll();
        return values();
    }

    private int[] values(){
        int[] v = new int[5];
        for(int i=0; i<5; i++) v[i] = dice[i].getValue();
        return v;
    }
}
