package io.configwise.android.sdk_example.controllers;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import io.configwise.android.sdk_example.R;
import io.configwise.android.sdk_example.Utils;


public class ConfirmDialogFragment extends DialogFragment {

    @Nullable
    private String mTitle;

    @Nullable
    private String mMessage;

    @Nullable
    private String mPositiveButtonTitle;

    private int mPositiveButtonColorResId = R.color.colorPositive;

    @Nullable
    private String mNegativeButtonTitle;

    private int mNegativeButtonColorResId = R.color.colorNegative;

    @Nullable
    private BaseActivity.ConfirmDialogDelegate mDelegate;

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public void setMessage(String message) {
        this.mMessage = message;
    }

    public void setPositiveButtonTitle(String positiveButtonTitle) {
        this.mPositiveButtonTitle = positiveButtonTitle;
    }

    public void setPositiveButtonColorResId(int positiveButtonColorResId) {
        this.mPositiveButtonColorResId = positiveButtonColorResId;
    }

    public void setNegativeButtonTitle(String negativeButtonTitle) {
        this.mNegativeButtonTitle = negativeButtonTitle;
    }

    public void setNegativeButtonColorResId(int negativeButtonColorResId) {
        this.mNegativeButtonColorResId = negativeButtonColorResId;
    }

    public void setDelegate(BaseActivity.ConfirmDialogDelegate delegate) {
        this.mDelegate = delegate;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_confirm, null);

        TextView titleTextView = dialogView.findViewById(R.id.dialogTitle);
        if (titleTextView != null) {
            titleTextView.setText(mTitle);
        }

        TextView messageTextView = dialogView.findViewById(R.id.dialogMessage);
        if (messageTextView != null) {
            messageTextView.setText(mMessage);
        }

        Button positiveButton = dialogView.findViewById(R.id.dialogPositiveButton);
        if (positiveButton != null) {
            positiveButton.setText(mPositiveButtonTitle);
            positiveButton.setBackgroundTintList(ColorStateList.valueOf(
                    Utils.colorResIdToColor(getContext(), mPositiveButtonColorResId)
            ));
            positiveButton.setOnClickListener(v -> {
                dismiss();
                if (mDelegate != null) {
                    mDelegate.onOk();
                    mDelegate.onClosed();
                }
            });
        }

        Button negativeButton = dialogView.findViewById(R.id.dialogNegativeButton);
        if (negativeButton != null) {
            negativeButton.setText(mNegativeButtonTitle);
            negativeButton.setBackgroundTintList(ColorStateList.valueOf(
                    Utils.colorResIdToColor(getContext(), mNegativeButtonColorResId)
            ));
            negativeButton.setOnClickListener(v -> {
                dismiss();
                if (mDelegate != null) {
                    mDelegate.onCancel();
                    mDelegate.onClosed();
                }
            });
        }

        ImageView closeButton = dialogView.findViewById(R.id.dialogCloseButton);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                dismiss();
                if (mDelegate != null) {
                    mDelegate.onCancel();
                    mDelegate.onClosed();
                }
            });
        }

        return new AlertDialog.Builder(getContext(), R.style.AppTheme_Dialog_Alert)
                .setView(dialogView)
                .setCancelable(false)
                .create();
    }
}
