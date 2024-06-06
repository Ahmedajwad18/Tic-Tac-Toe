package sa.kfupm.tictactoe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class WelcomeActivity extends AppCompatActivity {

    private TextView welcomeTextView;
    private Button playButton;
    private DatabaseReference mDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        mDatabaseRef = FirebaseDatabase.getInstance().getReference("users");

        welcomeTextView = findViewById(R.id.welcomeTextView);
        playButton = findViewById(R.id.playButton);

        String phoneNumber = getIntent().getStringExtra("phoneNumber");
        mDatabaseRef.child(phoneNumber).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.getValue(String.class);
                    welcomeTextView.setText("Welcome " + name + "!");
                } else {
                    welcomeTextView.setText("Welcome!");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(WelcomeActivity.this, "Failed to retrieve user data", Toast.LENGTH_SHORT).show();
            }
        });

        playButton.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, TicTacToeActivity.class);
            startActivity(intent);
        });
    }
}