package ca.alexcomeau.texmobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import ca.alexcomeau.texmobile.R;

public class SettingsActivity extends AppCompatActivity {
    private SeekBar seekMusic, seekSound;
    private TextView txtMusic, txtSound;
    private Spinner spnLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        seekMusic = (SeekBar) findViewById(R.id.seekVolMusic);
        seekSound = (SeekBar) findViewById(R.id.seekVolSound);
        txtMusic = (TextView) findViewById(R.id.txtMusic);
        txtSound = (TextView) findViewById(R.id.txtSound);

        // Populate spinner
        spnLevel = (Spinner) findViewById(R.id.spinnerStarts);
        Integer[] levels = new Integer[]{0, 100, 200, 300, 400, 500, 600, 700, 800, 900};
        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_item, levels);
        spnLevel.setAdapter(adapter);

        // Wire up event listeners
        seekMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                txtMusic.setText(String.format(getString(R.string.volMusic), progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        seekSound.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                txtSound.setText(String.format(getString(R.string.volSound), progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // Initial values
        seekMusic.setProgress(getSharedPreferences("volume", 0).getInt("music", 100));
        seekSound.setProgress(getSharedPreferences("volume", 0).getInt("sound", 100));
        txtMusic.setText(String.format(getString(R.string.volMusic), seekMusic.getProgress()));
        txtSound.setText(String.format(getString(R.string.volSound), seekSound.getProgress()));
    }

    public void btnOkClick(View v)
    {
        // Remember the name they enter so they don't have to type it again
        getSharedPreferences("volume", 0).edit().putInt("music", seekMusic.getProgress()).commit();
        getSharedPreferences("volume", 0).edit().putInt("sound", seekSound.getProgress()).commit();

        // Send the selected start level back to the game activity
        Intent output = new Intent();
        output.putExtra("start", spnLevel.getSelectedItemPosition() * 100);
        setResult(RESULT_OK, output);
        finish();
    }
}