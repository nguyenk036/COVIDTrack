package com.kevinnguyenle.covidtracker;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.kevinnguyenle.covidtracker.databinding.ActivitySignUpBinding;

import static com.kevinnguyenle.covidtracker.utility.Utilities.setTransition;

/**
 * SignUpActivity - Register for a COVIDTracker account to be stored on a FirebaseDB
 */
public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private FirebaseAuth mAuth;
    private String email;
    private String password;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTransition(this);

        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        signUp();
    }

    // Handles the account validation process
    private void signUp() {
        binding.btnRegisterAcc.setOnClickListener(v -> {

            try {
                email = binding.etEmail.getText().toString().trim();
                password = binding.etPassword.getText().toString().trim();

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    saveUserToDatabase();
                                    startActivity(new Intent(SignUpActivity.this, MapActivity.class), ActivityOptions.makeSceneTransitionAnimation(SignUpActivity.this).toBundle());
                                } else {
                                    Toast.makeText(SignUpActivity.this, "Please enter a valid email and password",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                Toast.makeText(SignUpActivity.this, "Please enter a valid email and password",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Handles the saving to FirebaseDB process
    private void saveUserToDatabase() {
        try {
            // Get the currently signed in users userID
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            // Reference the databases user model
            DatabaseReference mbase = FirebaseDatabase.getInstance().getReference("users");
            // Add the userID to the databases users
            mbase.child(user.getUid()).setValue(user.getEmail());

        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}