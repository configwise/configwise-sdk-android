package io.configwise.android.sdk_example.controllers.main;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

import io.configwise.android.sdk_example.MyApplication;
import io.configwise.android.sdk_example.R;
import io.configwise.android.sdk_example.controllers.BaseActivity;

public class SplashActivity extends BaseActivity {

    private View mPoweredByContainer;

    @Override
    protected int contentViewResId() {
        return R.layout.activity_splash;
    }

    @Override
    protected boolean isVerifyIfAuthorized() {
        return true;
    }

    @Override
    protected long postDelayedMillisToSignOut() {
        return 1000;
    }

    @Override
    protected boolean isProgressIndicatorAvailableOnVerifyAuthorizationStep() {
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        showProgressIndicator();
    }

    @Override
    protected void onStop() {
        hideProgressIndicator(View.INVISIBLE);
        super.onStop();
    }

    @Override
    protected void onPostSignIn() {
        super.onPostSignIn();
        new Handler(Looper.getMainLooper()).postDelayed(this::gotoMain, 1000);
    }
}
