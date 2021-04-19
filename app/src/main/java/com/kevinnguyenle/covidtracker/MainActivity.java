package com.kevinnguyenle.covidtracker;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.kevinnguyenle.covidtracker.databinding.ActivityMainBinding;

import static com.kevinnguyenle.covidtracker.utility.Utilities.setTransition;

/**
 * ADEV-2007
 * Kevin Nguyen Le
 * COVIDTracker - Android app designed to visualize, track and calculate statistical
 *                data of COVID-19 cases around Canada by province.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseUser user;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTransition(this);

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        user = FirebaseAuth.getInstance().getCurrentUser();
        setContentView(binding.getRoot());

        if (user != null) {
            binding.btnRegister.setVisibility(View.INVISIBLE);
            binding.btnLogin.setVisibility(View.INVISIBLE);
            binding.btnContinue.setText("Go to map");
            binding.btnLogout.setVisibility(View.VISIBLE);
        } else {
            binding.btnRegister.setVisibility(View.VISIBLE);
            binding.btnLogin.setVisibility(View.VISIBLE);
            binding.btnLogout.setVisibility(View.INVISIBLE);
            binding.btnContinue.setText("Skip for now >>");
        }

        binding.btnContinue.setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class),
                                                    ActivityOptions.makeSceneTransitionAnimation(this).toBundle()));

        binding.btnLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class),
                                                 ActivityOptions.makeSceneTransitionAnimation(this,
                                                         Pair.create(binding.AppTitle, "title"),
                                                         Pair.create(binding.btnLogin, "signin")).toBundle()));

        binding.btnRegister.setOnClickListener(v -> startActivity(new Intent(this, SignUpActivity.class),
                                                    ActivityOptions.makeSceneTransitionAnimation(this,
                                                            Pair.create(binding.AppTitle, "title"),
                                                            Pair.create(binding.btnRegister, "create")).toBundle()));

        binding.btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class),
                                                    ActivityOptions.makeSceneTransitionAnimation(this).toBundle()));

        binding.btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                finish();
                startActivity(getIntent(), ActivityOptions.makeSceneTransitionAnimation(this,
                        Pair.create(binding.AppTitle, "title"),
                        Pair.create(binding.imgLogo, "logo"),
                        Pair.create(binding.btnSettings, "settings")).toBundle());
        });
    }
}