package game;

import java.util.*;

public class ScoreCalculator {
    public static int score(int[] d, String cat){
        int[] f = new int[7];
        for(int v: d) f[v]++;
        int sum = Arrays.stream(d).sum();

        switch(cat){
            case "Ones":            return f[1]*1;
            case "Twos":            return f[2]*2;
            case "Threes":          return f[3]*3;
            case "Fours":           return f[4]*4;
            case "Fives":           return f[5]*5;
            case "Sixes":           return f[6]*6;
            case "Three of a Kind": return Arrays.stream(f).anyMatch(c->c>=3)?sum:0;
            case "Four of a Kind":  return Arrays.stream(f).anyMatch(c->c>=4)?sum:0;
            case "Full House":      return has(f,3)&&has(f,2)?25:0;
            case "Small Straight":  return straight(f,4)?30:0;
            case "Large Straight":  return straight(f,5)?40:0;
            case "Yahtzee":         return Arrays.stream(f).anyMatch(c->c==5)?50:0;
            case "Chance":          return sum;
            default: return 0;
        }
    }

    private static boolean has(int[] f, int n){
        for(int c: f) if(c==n) return true;
        return false;
    }

    private static boolean straight(int[] f, int len){
        int cnt = 0;
        for(int i=1; i<=6; i++){
            if(f[i]>0){
                if(++cnt >= len) return true;
            } else cnt = 0;
        }
        return false;
    }
}
