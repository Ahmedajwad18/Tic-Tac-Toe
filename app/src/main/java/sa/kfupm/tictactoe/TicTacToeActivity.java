package sa.kfupm.tictactoe;

import android.content.Intent; import android.os.Bundle; import android.os.Handler; import android.view.View; import android.widget.Button; import android.widget.TextView; import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot; import com.google.firebase.database.DatabaseError; import com.google.firebase.database.DatabaseReference; import com.google.firebase.database.FirebaseDatabase; import com.google.firebase.database.ValueEventListener;

public class TicTacToeActivity extends AppCompatActivity implements View.OnClickListener { private DatabaseReference refPlayer; private DatabaseReference refCanPlay; private DatabaseReference refWinner; private DatabaseReference refCases; private DatabaseReference refGameStatus;

    private int[] cellValues = {0, 0, 0, 0, 0, 0, 0, 0, 0};
    private Button[] buttonsArray = new Button[9];

    private boolean canPlay = false;
    private int player = 0;
    private boolean finished = false;

    private TextView playerTextView;
    private TextView gameStatusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gui_tictactoe);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        refPlayer = database.getReference("player");
        refCanPlay = database.getReference("canplay");
        refWinner = database.getReference("winner");
        refCases = database.getReference("cells");
        refGameStatus = database.getReference("gameStatus");

        playerTextView = findViewById(R.id.playerTextView);
        gameStatusTextView = findViewById(R.id.gameStatusTextView);

        buttonsArray[0] = findViewById(R.id.button0);
        buttonsArray[1] = findViewById(R.id.button1);
        buttonsArray[2] = findViewById(R.id.button2);
        buttonsArray[3] = findViewById(R.id.button3);
        buttonsArray[4] = findViewById(R.id.button4);
        buttonsArray[5] = findViewById(R.id.button5);
        buttonsArray[6] = findViewById(R.id.button6);
        buttonsArray[7] = findViewById(R.id.button7);
        buttonsArray[8] = findViewById(R.id.button8);

        for (Button button : buttonsArray) {
            button.setOnClickListener(this);
            button.setTextSize(36);
            button.setPadding(0, 0, 0, 0);
        }

        refPlayer.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int p1 = dataSnapshot.child("p1").getValue(Integer.class);
                int p2 = dataSnapshot.child("p2").getValue(Integer.class);

                if (p1 == 0) {
                    refPlayer.child("p1").setValue(1);
                    player = 1;
                    playerTextView.setText("Player 1");
                    refGameStatus.setValue("Waiting for Player 2");
                    refCanPlay.setValue(1); // Player 1 starts
                } else if (p1 == 1 && p2 == 0) {
                    refPlayer.child("p2").setValue(1);
                    player = 2;
                    playerTextView.setText("Player 2");
                    refGameStatus.setValue("Game started");
                } else if (p1 == 1 && p2 == 1) {
                    Toast.makeText(TicTacToeActivity.this, "Game is full", Toast.LENGTH_SHORT).show();
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(TicTacToeActivity.this, WelcomeActivity.class));
                        finish();
                    }, 3000); // 3-second delay
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        refCanPlay.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int currentPlayerTurn = dataSnapshot.getValue(Integer.class);
                canPlay = (currentPlayerTurn == player);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        refWinner.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String winnerS = null;
                int winner = dataSnapshot.getValue(Integer.class);
                if (winner == 1) {
                    winnerS = "Player 1 wins!";
                    finished = true;
                } else if (winner == 2) {
                    winnerS = "Player 2 wins!";
                    finished = true;
                } else if (winner == 0) {
                    winnerS = "its a Tie!";
                    finished = true;
                }

                if (finished) {
                    resetDatabase();
                    Toast.makeText(TicTacToeActivity.this, winnerS, Toast.LENGTH_SHORT).show();
                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(TicTacToeActivity.this, WelcomeActivity.class));
                        finish();
                    }, 3000); // 3-second delay
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        refGameStatus.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String gameStatus = dataSnapshot.getValue(String.class);
                gameStatusTextView.setText(gameStatus);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        refCases.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot caseSnapshot : dataSnapshot.getChildren()) {
                    int index = Integer.parseInt(caseSnapshot.getKey());
                    int value = caseSnapshot.getValue(Integer.class);
                    cellValues[index] = value;
                    updateButtonText(index, value);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        resetDatabase();
    }

    @Override
    public void onClick(View view) {
        if (!canPlay || finished) {
            return;
        }

        int index = -1;
        for (int i = 0; i < buttonsArray.length; i++) {
            if (view == buttonsArray[i]) {
                index = i;
                break;
            }
        }

        if (index == -1 || cellValues[index] != 0) {
            return;
        }

        cellValues[index] = player;
        refCases.child(String.valueOf(index)).setValue(player);
        updateButtonText(index, player);
        checkWinner();

        if (!finished) {
            refCanPlay.setValue(player == 1 ? 2 : 1);
        }
    }

    private void updateButtonText(int index, int value) {
        String text = value == 1 ? "X" : value == 2 ? "O" : "";
        buttonsArray[index].setText(text);
    }

    private void checkWinner() {
        int[][] winningCombinations = {
                {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
                {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
                {0, 4, 8}, {2, 4, 6}
        };

        for (int[] combination : winningCombinations) {
            if (cellValues[combination[0]] == player &&
                    cellValues[combination[1]] == player &&
                    cellValues[combination[2]] == player) {
                refWinner.setValue(player);
                return;
            }
        }

        boolean isTie = true;
        for (int value : cellValues) {
            if (value == 0) {
                isTie = false;
                break;
            }
        }

        if (isTie) {
            refWinner.setValue(0);
        }
    }

    public void resetGame(View view) {
        resetCells();
        refWinner.setValue(-1);
        refCanPlay.setValue(1);
        finished = false;
        refGameStatus.setValue("");

        refPlayer.child("p1").setValue(0);
        refPlayer.child("p2").setValue(0);
        player = 1;
        playerTextView.setText("Player 1");
    }

    private void resetCells() {
        for (int i = 0; i < cellValues.length; i++) {
            cellValues[i] = 0;
            buttonsArray[i].setText("");
            refCases.child(String.valueOf(i)).setValue(0);
        }
    }

    private void resetDatabase() {
        refPlayer.child("p1").setValue(0);
        refPlayer.child("p2").setValue(0);
        refWinner.setValue(-1);
        refCanPlay.setValue(0);
        refGameStatus.setValue(""); // Reset game status
        for (int i = 0; i < 9; i++) {
            refCases.child(String.valueOf(i)).setValue(0);
        }
    }

    public void resetButton(View view) {
        resetCells();
        refCanPlay.setValue(1);
    }
}