package game;

public class Dice {
    private int value;
    public void roll() { value = 1 + (int)(Math.random() * 6); }
    public int getValue() { return value; }
}
