package io.configwise.android.sdk_example.controllers;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import io.configwise.android.sdk_example.R;

public class ProgressDialogFragment extends DialogFragment implements BaseActivity.ProgressIndicatorModal {

    @Nullable
    private TextView mProgressIndicatorPercentage;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_progress_indicator, null);

        mProgressIndicatorPercentage = dialogView.findViewById(R.id.progressIndicatorPercentage);

        return new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog_Alert)
                .setView(dialogView)
                .setCancelable(false)
                .create();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void setValue(float value) {
        if (mProgressIndicatorPercentage != null) {
            mProgressIndicatorPercentage.setText(getString(R.string.loading_percentage,
                    Math.round(value * 100) + "%")
            );
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }
}
