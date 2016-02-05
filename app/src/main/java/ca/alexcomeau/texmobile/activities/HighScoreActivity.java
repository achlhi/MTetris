package ca.alexcomeau.texmobile.activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import java.util.List;

import ca.alexcomeau.texmobile.R;
import ca.alexcomeau.texmobile.db.Score;
import ca.alexcomeau.texmobile.db.ScoreDBManager;

public class HighScoreActivity extends AppCompatActivity {
    private MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scores);

        // Start the tunes
        float volume = getSharedPreferences("volume", 0).getInt("music", 100) / 100.0f;
        mp = MediaPlayer.create(this, R.raw.chibi_ninja);
        mp.setVolume(0.7f * volume, 0.7f * volume);
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mediaplayer)
            {
                mediaplayer.seekTo(0);
                mediaplayer.start();
            }
        });

        // Go back to where the song was, if it had already been playing
        if(savedInstanceState != null)
            mp.seekTo(savedInstanceState.getInt("songPosition"));

        mp.start();

        // Get all the scores
        ScoreDBManager scoreManager = new ScoreDBManager(this);
        scoreManager.open();
        List<Score> scores = scoreManager.getAllScores();
        scoreManager.close();

        TableLayout tl = (TableLayout) findViewById(R.id.tblScores);

        // Add all the scores to the table
        for(Score s : scores)
        {
            TableRow row = new TableRow(this);
            TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT);
            row.setLayoutParams(lp);

            TextView txtName = new TextView(this);
            txtName.setText(s.name);
            txtName.setLayoutParams(new TableRow.LayoutParams(1));

            TextView txtScore = new TextView(this);
            txtScore.setText(Integer.toString(s.score));
            txtScore.setLayoutParams(new TableRow.LayoutParams(2));

            TextView txtTime = new TextView(this);
            txtTime.setText(s.time);
            txtTime.setLayoutParams(new TableRow.LayoutParams(3));

            TextView txtGrade = new TextView(this);
            txtGrade.setText(s.grade);
            txtGrade.setLayoutParams(new TableRow.LayoutParams(4));

            row.addView(txtName);
            row.addView(txtScore);
            row.addView(txtTime);
            row.addView(txtGrade);

            tl.addView(row);
        }
    }

    public void btnBackClick(View v)
    {
        // Back to the main menu.
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("songPosition", mp.getCurrentPosition());
        finish();
        startActivity(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putInt("songPosition", mp.getCurrentPosition());
    }

    @Override
    protected void onPause()
    {
        mp.pause();
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        mp.start();
        super.onResume();
    }

    @Override
    protected void onDestroy()
    {
        mp.release();
        mp = null;
        super.onDestroy();
    }
}
