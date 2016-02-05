package ca.alexcomeau.texmobile.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.*;

import java.util.ArrayList;
import java.util.List;

public class ScoreDBManager {
    private final String DATABASE_NAME = "scoresdb";
    private final int DATABASE_VERSION = 1;
    private final int MAX_SCORES = 20;

    private Cursor cursor;
    private SQLiteDatabase db;
    private Context context;
    private DBHelper dbHelper;

    // Constructor
    public ScoreDBManager(Context ctx)
    {
        context = ctx;
        cursor = null;
        db = null;
    }

    // Getters
    public Cursor getCursor() {
        return cursor;
    }

    // Public Methods =============================================================
    public void open() {
        dbHelper = new DBHelper();
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
        db = null;
    }

    public boolean writeScore(String name, int score, String time, String grade)
    {
        if(db == null) return false;

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("score", score);
        values.put("duration", time);
        values.put("grade", grade);
        db.insert("tblScores", null, values);

        db.execSQL("DELETE FROM tblScores WHERE id NOT IN " +
                           "(SELECT id FROM tblScores ORDER BY score DESC, duration ASC LIMIT " +
                           MAX_SCORES + ")");

        return true;
    }

    public int getLowestScore()
    {
        try {
            cursor = null;

            if(db == null) return 0;

            cursor = db.rawQuery("SELECT MIN(score) FROM tblScores", null);

            if(cursor.getCount() == 0) return 0;

            cursor.moveToFirst();
            return cursor.getInt(0);
        }
        catch (Exception e) { return 0;}
    }

    public List<Score> getAllScores()
    {
        List<Score> result = new ArrayList<>();

        try {
            cursor = null;

            if(db == null) return null;

            // Highest score, then lowest time
            cursor = db.rawQuery("SELECT * FROM tblScores ORDER BY score DESC, duration ASC", null);

            while (cursor.moveToNext())
            {
                Score s = new Score();
                s.name = cursor.getString(1);
                s.score = cursor.getInt(2);
                s.time = cursor.getString(3);
                s.grade = cursor.getString(4);
                result.add(s);
            }
        }
        catch (Exception e) { return result; }

        return result;
    }

    // Inner class =================================================================
    private class DBHelper extends SQLiteOpenHelper {

        public DBHelper () { super(context, DATABASE_NAME, null, DATABASE_VERSION); }

        @Override
        public void onCreate(SQLiteDatabase db) {}

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
    }
}

