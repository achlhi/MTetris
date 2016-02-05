package ca.alexcomeau.texmobile.activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import ca.alexcomeau.texmobile.db.DatabaseBundler;
import ca.alexcomeau.texmobile.R;

public class MainActivity extends AppCompatActivity {
    private MediaPlayer mp;
    private int startLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bundle the database if it isn't already.
        DatabaseBundler dbBundle = new DatabaseBundler(this);
        dbBundle.bundle("scoresdb");

        // Make the links clickable in the credits
        TextView txtCredits = (TextView) findViewById(R.id.txtCredits);
        txtCredits.setMovementMethod(LinkMovementMethod.getInstance());

        float volume = getSharedPreferences("volume", 0).getInt("music", 100) / 100.0f;
        mp = MediaPlayer.create(this, R.raw.chibi_ninja);
        mp.setVolume(0.7f * volume, 0.7f * volume);

        // setLooping doesn't work with the "AwesomePlayer"
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
        else
        {
            // If this was started from the HighScore activity, keep the song position.
            Intent i = getIntent();
            mp.seekTo(i.getIntExtra("songPosition", 0));
        }

        mp.start();

    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putInt("songPosition", mp.getCurrentPosition());
    }

    @Override
    protected void onDestroy()
    {
        mp.release();
        mp = null;
        super.onDestroy();
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

    public void btnClick(View v)
    {
        Intent intent = new Intent("ca.alexcomeau.texmobile.Game");
        int maxLevel = Integer.parseInt(v.getTag().toString());

        // Don't start then immediately win
        if(startLevel >= maxLevel)
            startLevel = maxLevel - 100;

        intent.putExtra("startLevel", startLevel);
        intent.putExtra("maxLevel", maxLevel);
        finish();
        startActivity(intent);
    }

    public void btnSettingsClick(View v)
    {
        Intent intent = new Intent("ca.alexcomeau.texmobile.Settings");
        startActivityForResult(intent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK)
        {
            float volume = getSharedPreferences("volume", 0).getInt("music", 100) / 100.0f;
            mp.setVolume(0.7f * volume, 0.7f * volume);
            startLevel = data.getIntExtra("start", 0);
        }
    }
}
