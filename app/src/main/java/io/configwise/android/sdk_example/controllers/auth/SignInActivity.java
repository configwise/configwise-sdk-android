package io.configwise.android.sdk_example.controllers.auth;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.widget.Button;

import bolts.Task;
import io.configwise.android.sdk_example.R;
import io.configwise.android.sdk_example.controllers.ToolbarAwareBaseActivity;
import io.configwise.sdk.ConfigWiseSDK;
import io.configwise.sdk.services.AuthService;
import io.configwise.sdk.services.UnsupportedAppVersionService;

public class SignInActivity extends ToolbarAwareBaseActivity {

    private static final String TAG = SignInActivity.class.getSimpleName();

    private Button mSignInButton;

    @Override
    protected int contentViewResId() {
        return R.layout.activity_signin;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSignInButton = findViewById(R.id.signInButton);
        if (mSignInButton != null) {
            mSignInButton.setOnClickListener(v -> {
                doSignIn();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ConfigWiseSDK.getInstance().isB2C()) {
            doSignIn();
        }
    }

    private void doSignIn() {
        showProgressIndicator();
        disableForm();
        UnsupportedAppVersionService.getInstance().isUnsupported().onSuccessTask(task -> {
            final boolean isUnsupported = task.getResult();
            if (isUnsupported) {
                return Task.forError(new RuntimeException(getString(R.string.unsupported_app_version_message)));
            }

            return AuthService.getInstance().signIn(
                    ConfigWiseSDK.getInstance().getCompanyAuthToken(),
                    ConfigWiseSDK.getInstance().getCompanyAuthToken()
            );
        }).continueWith(task -> {
            hideProgressIndicator();
            enableForm();

            if (task.isCancelled()) {
                String message = "Unable to sign in due invocation task is canceled.";
                Log.e(TAG, message);
                showSimpleDialog(getString(R.string.error), getString(R.string.invocation_canceled));
                return null;
            }
            if (task.isFaulted()) {
                Exception e = task.getError();
                Log.e(TAG, "Unable to sign in due error", e);
                showSimpleDialog(
                        getString(R.string.error),
                        ConfigWiseSDK.getInstance().isB2B()
                                ? e.getMessage()
                                : getString(R.string.unauthorized_message)
                );
                return null;
            }

            gotoMain();

            return null;
        }, Task.UI_THREAD_EXECUTOR);
    }

    private void enableForm() {
        mSignInButton.setEnabled(true);
    }

    private void disableForm() {
        mSignInButton.setEnabled(false);
    }
}
