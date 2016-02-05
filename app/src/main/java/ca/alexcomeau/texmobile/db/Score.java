package ca.alexcomeau.texmobile.db;

// Data class
public class Score {
    public String name;
    public int score;
    public String time;
    public String grade;

    public Score() {}

    public Score(String n, int s, String t, String g)
    {
        name = n;
        score = s;
        time = t;
        grade = g;
    }
}
