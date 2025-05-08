package game;

import java.util.*;
public class ScoreCard {
    private final Map<String,Integer> scores = new LinkedHashMap<>();
    private final String[] cats = {"Ones","Twos","Threes","Fours","Fives","Sixes",
        "Three of a Kind","Four of a Kind","Full House","Small Straight","Large Straight","Yahtzee","Chance"};
    
    public ScoreCard(){ for(String c: cats) scores.put(c, null); }

    public boolean isEmpty(int idx){ return scores.get(cats[idx-1])==null; }

    public void setScore(int idx, int sc){ scores.put(cats[idx-1], sc); }

    public String getCat(int idx){ return cats[idx-1]; }

    public int total(){ return scores.values().stream().filter(Objects::nonNull).mapToInt(i->i).sum(); }
    
    public void displayPossible(int[] dice) {
        System.out.println("Categorie disponibili e punteggi attuali:");
        System.out.printf("%-3s %-20s %10s %10s%n", "#", "Categoria", "Attuale", "Massimo");
        for (int i = 1; i <= cats.length; i++) {
            if (isEmpty(i)) {
                String cat = cats[i-1];
                int current = ScoreCalculator.score(dice, cat);
                int maxPossible;
                if (i >= 1 && i <= 6) {
                    maxPossible = i * 5;
                } else {
                    switch (cat) {
                        case "Three of a Kind": maxPossible = 30; break;
                        case "Four of a Kind":  maxPossible = 30; break;
                        case "Full House":      maxPossible = 25; break;
                        case "Small Straight":  maxPossible = 30; break;
                        case "Large Straight":  maxPossible = 40; break;
                        case "Yahtzee":         maxPossible = 50; break;
                        case "Chance":          maxPossible = 30; break;
                        default: maxPossible = 0;
                    }
                }
                System.out.printf("%-3d %-20s %10d %10d%n", i, cat, current, maxPossible);
            }
        }
    }
}