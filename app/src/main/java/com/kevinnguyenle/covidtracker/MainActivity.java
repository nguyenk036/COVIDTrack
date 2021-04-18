package com.kevinnguyenle.covidtracker;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;

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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTransition(this);

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
    }
}