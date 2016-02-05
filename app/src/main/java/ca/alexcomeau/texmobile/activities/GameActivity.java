package ca.alexcomeau.texmobile.activities;

import android.content.Intent;
import android.graphics.drawable.NinePatchDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import ca.alexcomeau.texmobile.game.GameManager;
import ca.alexcomeau.texmobile.game.GameView;
import ca.alexcomeau.texmobile.R;
import ca.alexcomeau.texmobile.db.ScoreDBManager;
import ca.alexcomeau.texmobile.game.Block;

public class GameActivity extends AppCompatActivity{
    private GameView gameView;
    private List<String> input;
    private MediaPlayer mp;
    private SoundPool sp;
    private int[] soundEffects;
    private float volume;
    private ImageView imgNext;
    private TextView txtScore;
    private TextView txtLevel;
    private Hashtable<Block.Shape, NinePatchDrawable> nextPieces;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        input = new ArrayList<>();
        gameView = (GameView) findViewById(R.id.svBoard);
        txtScore = (TextView) findViewById(R.id.txtScore);
        txtLevel = (TextView) findViewById(R.id.txtLevel);
        imgNext = (ImageView) findViewById(R.id.imgNext);

        volume = getSharedPreferences("volume", 0).getInt("music", 100) / 100.0f;
        mp = MediaPlayer.create(this, R.raw.all_of_us);
        mp.setVolume(0.5f * volume, 0.5f * volume);
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mediaplayer)
            {
                mediaplayer.seekTo(0);
                mediaplayer.start();
            }
        });

        // This was deprecated in API 21, but target is 19...
        sp = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
        soundEffects = new int[2];
        soundEffects[0] = sp.load(this, R.raw.piece_lock, 1);
        soundEffects[1] = sp.load(this, R.raw.line_clear, 1);
        volume = getSharedPreferences("volume", 0).getInt("sound", 100) / 100.0f;

        // Get all the drawables
        nextPieces = new Hashtable<>();
        nextPieces.put(Block.Shape.I, (NinePatchDrawable) ContextCompat.getDrawable(this, R.drawable.next_piece_i));
        nextPieces.put(Block.Shape.J, (NinePatchDrawable) ContextCompat.getDrawable(this, R.drawable.next_piece_j));
        nextPieces.put(Block.Shape.L, (NinePatchDrawable) ContextCompat.getDrawable(this, R.drawable.next_piece_l));
        nextPieces.put(Block.Shape.O, (NinePatchDrawable) ContextCompat.getDrawable(this, R.drawable.next_piece_o));
        nextPieces.put(Block.Shape.S, (NinePatchDrawable) ContextCompat.getDrawable(this, R.drawable.next_piece_s));
        nextPieces.put(Block.Shape.T, (NinePatchDrawable) ContextCompat.getDrawable(this, R.drawable.next_piece_t));
        nextPieces.put(Block.Shape.Z, (NinePatchDrawable) ContextCompat.getDrawable(this, R.drawable.next_piece_z));

        // Wire up all the buttons
        List<ImageButton> btns = new ArrayList<>();
        btns.add((ImageButton) findViewById(R.id.btnDown2));
        btns.add((ImageButton) findViewById(R.id.btnDown));
        btns.add((ImageButton) findViewById(R.id.btnMoveLeft));
        btns.add((ImageButton) findViewById(R.id.btnMoveRight));
        btns.add((ImageButton) findViewById(R.id.btnRotateLeft));
        btns.add((ImageButton) findViewById(R.id.btnRotateRight));

        for(ImageButton b : btns)
            b.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event)
                {
                    switch(event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                        {
                            v.setPressed(true);
                            String tag = v.getTag().toString();
                            if(!input.contains(tag))
                                input.add(tag);

                            if (gameView.getGame().getGameOver() != null)
                                gameOver();

                            return true;
                        }
                        case MotionEvent.ACTION_UP:
                        {
                            v.setPressed(false);
                            input.remove(v.getTag().toString());
                            return true;
                        }
                    }
                    return false;
                }
            });

        if(savedInstanceState == null)
        {
            Intent intent = getIntent();
            gameView.setupGame(intent.getIntExtra("startLevel", 0), intent.getIntExtra("maxLevel", 999), this);
        }
        else
        {
            gameView.setupGame((GameManager) savedInstanceState.getParcelable("game"), this);
            mp.seekTo(savedInstanceState.getInt("songPosition"));
        }

        mp.start();
    }

    public void playSound(int i) { sp.play(soundEffects[i], volume, volume, 1, 0, 1.0f); }

    private void gameOver()
    {
        // Check if it's a new high score
        ScoreDBManager scores = new ScoreDBManager(this);
        scores.open();
        int lowestScore = scores.getLowestScore();
        scores.close();

        if(gameView.getGame().getScore() > lowestScore)
        {
            Intent intent = new Intent("ca.alexcomeau.texmobile.EnterScore");
            startActivityForResult(intent, 1);
        }
        else
        {
            Intent intent = new Intent("ca.alexcomeau.texmobile.HighScores");
            finish();
            startActivity(intent);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK)
        {
            GameManager game = gameView.getGame();
            // Convert the frames to minutes and seconds
            String time;
            int seconds = game.getFrames() / 30; // 30 frames per second

            if(Integer.toString(seconds % 60).length() == 1)
                time = (seconds / 60) + ":0" + (seconds % 60);
            else
                time = (seconds / 60) + ":" + (seconds % 60);

            // Write the high score to the database
            ScoreDBManager scores = new ScoreDBManager(this);
            scores.open();
            scores.writeScore(data.getStringExtra("name"), game.getScore(), time, game.getGrade());
            scores.close();

            Intent intent = new Intent("ca.alexcomeau.texmobile.HighScores");
            finish();
            startActivity(intent);
        }
    }

    public List<String> getInput() { return input; }

    public void setNextPiece(Block.Shape shape) { imgNext.setBackground(nextPieces.get(shape)); }
    public void setScore(int score) { txtScore.setText(String.format(getString(R.string.score), score)); }
    public void setLevel(int curr, int last) { txtLevel.setText(String.format(getString(R.string.level), curr, last)); }

    @Override
    protected void onPause()
    {
        mp.pause();
        gameView.stop();
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        mp.start();
        gameView.start();
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        outState.putParcelable("game", gameView.getGame());
        outState.putInt("songPosition", mp.getCurrentPosition());
    }

    @Override
    protected void onDestroy()
    {
        mp.release();
        sp.release();
        mp = null;
        sp = null;
        super.onDestroy();
    }
}
