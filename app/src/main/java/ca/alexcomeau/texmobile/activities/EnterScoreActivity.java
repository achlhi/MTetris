package ca.alexcomeau.texmobile.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import ca.alexcomeau.texmobile.R;

public class EnterScoreActivity extends AppCompatActivity {
    private EditText txtName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_score);
        txtName = (EditText) findViewById(R.id.txtName);
        txtName.setText(getSharedPreferences("lastName", 0).getString("lastName", ""));
    }

    public void btnEnterClick(View v)
    {
        // Remember the name they enter so they don't have to type it again
        String name = txtName.getText().toString();
        getSharedPreferences("lastName", 0).edit().putString("lastName", name).commit();

        // Send the entered name back to the game activity
        Intent output = new Intent();
        output.putExtra("name", name);
        setResult(RESULT_OK, output);
        finish();
    }
}
