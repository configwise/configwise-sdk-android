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

public class SimpleDialogFragment extends DialogFragment {

    @Nullable
    private String mTitle;

    @Nullable
    private String mMessage;

    @Nullable
    private String mButtonTitle;

    private int mButtonColorResId = R.color.colorPositive;

    @Nullable
    private BaseActivity.SimpleDialogDelegate mDelegate;

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public void setMessage(String message) {
        this.mMessage = message;
    }

    public void setButtonTitle(String buttonTitle) {
        this.mButtonTitle = buttonTitle;
    }

    public void setButtonColorResId(int buttonColorResId) {
        this.mButtonColorResId = buttonColorResId;
    }

    public void setDelegate(BaseActivity.SimpleDialogDelegate delegate) {
        this.mDelegate = delegate;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_simple, null);

        TextView titleTextView = dialogView.findViewById(R.id.dialogTitle);
        if (titleTextView != null) {
            titleTextView.setText(mTitle);
        }

        TextView messageTextView = dialogView.findViewById(R.id.dialogMessage);
        if (messageTextView != null) {
            messageTextView.setText(mMessage);
        }

        Button button = dialogView.findViewById(R.id.dialogButton);
        if (button != null) {
            button.setText(mButtonTitle);
            button.setBackgroundTintList(ColorStateList.valueOf(
                    Utils.colorResIdToColor(getContext(), mButtonColorResId)
            ));
            button.setOnClickListener(v -> {
                dismiss();
                if (mDelegate != null) {
                    mDelegate.onClosed();
                }
            });
        }

        ImageView closeButton = dialogView.findViewById(R.id.dialogCloseButton);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                dismiss();
                if (mDelegate != null) {
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
