package sa.kfupm.tictactoe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private EditText phoneNumberEditText, verificationCodeEditText, nameEditText;
    private Button sendOTPButton, verifyCodeButton, resendOTPButton;
    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private DatabaseReference mDatabaseRef;
    private boolean isNewUser = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("users");
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        verificationCodeEditText = findViewById(R.id.verificationCodeEditText);
        nameEditText = findViewById(R.id.nameEditText);
        sendOTPButton = findViewById(R.id.sendOTPButton);
        verifyCodeButton = findViewById(R.id.verifyCodeButton);
        resendOTPButton = findViewById(R.id.resendOTPButton);
        sendOTPButton.setOnClickListener(v -> sendVerificationCode());
        verifyCodeButton.setOnClickListener(v -> verifyVerificationCode());
        resendOTPButton.setOnClickListener(v -> resendVerificationCode());
    }

    private void sendVerificationCode() {
        String phoneNumber = phoneNumberEditText.getText().toString();
        if (phoneNumber.isEmpty()) {
            phoneNumberEditText.setError("Phone number is required");
            phoneNumberEditText.requestFocus();
            return;
        }

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                mCallbacks
        );
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                    String name = nameEditText.getText().toString();
                    signInWithPhoneAuthCredential(phoneAuthCredential, name);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(@NonNull String verificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    Toast.makeText(MainActivity.this, "OTP sent", Toast.LENGTH_SHORT).show();
                    mVerificationId = verificationId;
                    mResendToken = token;

                    verificationCodeEditText.setVisibility(View.VISIBLE);
                    verifyCodeButton.setVisibility(View.VISIBLE);
                    resendOTPButton.setVisibility(View.VISIBLE);

                    checkIfUserExists();
                }
            };

    private void checkIfUserExists() {
        String phoneNumber = phoneNumberEditText.getText().toString();
        mDatabaseRef.child(phoneNumber).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    isNewUser = false;
                    nameEditText.setVisibility(View.GONE);
                } else {
                    isNewUser = true;
                    nameEditText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to check user in database", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyVerificationCode() {
        String code = verificationCodeEditText.getText().toString();
        if (code.isEmpty()) {
            verificationCodeEditText.setError("Please enter verification code");
            verificationCodeEditText.requestFocus();
            return;
        }

        String name = "";
        if (isNewUser) {
            name = nameEditText.getText().toString();
            if (name.isEmpty()) {
                nameEditText.setError("Please enter your name");
                nameEditText.requestFocus();
                return;
            }
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        signInWithPhoneAuthCredential(credential, name);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential, String name) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String phoneNumber = phoneNumberEditText.getText().toString();
                        if (isNewUser) {
                            mDatabaseRef.child(phoneNumber).setValue(name)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            navigateToWelcomeActivity(phoneNumber);
                                        } else {
                                            Toast.makeText(MainActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            navigateToWelcomeActivity(phoneNumber);
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToWelcomeActivity(String phoneNumber) {
        Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
        intent.putExtra("phoneNumber", phoneNumber);
        startActivity(intent);
        finish();
    }

    private void resendVerificationCode() {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumberEditText.getText().toString(),
                60,
                TimeUnit.SECONDS,
                this,
                mCallbacks,
                mResendToken
        );
    }
}