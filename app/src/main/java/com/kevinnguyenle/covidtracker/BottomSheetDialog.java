package com.kevinnguyenle.covidtracker;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.kevinnguyenle.covidtracker.databinding.FragmentBottomSheetDialogBinding;

/**
 * BottomSheetDialog - Modal containing the COVID-19 statistical data for selected province
 */
public class BottomSheetDialog extends BottomSheetDialogFragment {

    private FragmentBottomSheetDialogBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_bottom_sheet_dialog,
                container, false);

        binding = FragmentBottomSheetDialogBinding.inflate(inflater, container, false);

        binding.title.setText(this.getArguments().getString("province_name"));
        binding.lastUpdated.setText(this.getArguments().getString("date"));
        binding.txtTotalCases.setText(this.getArguments().getString("cases"));
        binding.txtTotalDeaths.setText(this.getArguments().getString("deaths"));
        binding.txtRecoveries.setText(this.getArguments().getString("recoveries"));
        binding.txtVaccinated.setText(this.getArguments().getString("vaccinated"));
        binding.lblPercent.setText(this.getArguments().getString("percentage"));
        binding.lblProgressPcnt.setText(Math.round(this.getArguments().getFloat("percent")) + "%");
        binding.pbProgress.setProgressWithAnimation(this.getArguments().getFloat("percent"), (long) 2500);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}