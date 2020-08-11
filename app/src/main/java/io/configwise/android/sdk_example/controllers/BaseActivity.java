package io.configwise.android.sdk_example.controllers;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import bolts.Task;
import io.configwise.android.sdk_example.R;
import io.configwise.android.sdk_example.Utils;
import io.configwise.android.sdk_example.controllers.ar.ArActivity;
import io.configwise.android.sdk_example.controllers.auth.SignInActivity;
import io.configwise.android.sdk_example.controllers.main.MainActivity;
import io.configwise.sdk.ConfigWiseSDK;
import io.configwise.sdk.domain.AppListItemEntity;
import io.configwise.sdk.domain.ComponentEntity;
import io.configwise.sdk.eventbus.SignOutEvent;
import io.configwise.sdk.eventbus.UnsupportedAppVersionEvent;
import io.configwise.sdk.services.AuthService;


public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = BaseActivity.class.getSimpleName();

    @Nullable
    private ProgressDialog mProgressDialog;

    @Nullable
    private ProgressBar mProgressIndicator;

    private boolean mTouchEventsEnabled = true;

    protected abstract int contentViewResId();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(contentViewResId());

        mProgressIndicator = findViewById(R.id.progressIndicator);
    }

    @Override
    protected void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        verifyIfOnline();

        if (isVerifyIfAuthorized()) {
            verifyIfAuthorized();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void onEventSignOut(SignOutEvent event) {
        onPostSignOut();
        gotoSignIn();
    }

    protected void onPostSignOut() {
    }

    protected void onPostSignIn() {
        ConfigWiseSDK.getInstance().subscribeIfNeeded();
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void onEventUnsupportedAppVersion(UnsupportedAppVersionEvent event) {
        showSimpleDialog(
                getString(R.string.error),
                getString(R.string.unsupported_app_version_message),
                () -> {
                    AuthService.getInstance().signOut();
                }
        );
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mTouchEventsEnabled) {
            return super.dispatchTouchEvent(ev);
        }

        return true;
    }

    protected void enableTouchEvents() {
        mTouchEventsEnabled = true;

    }

    private void verifyIfOnline() {
        if (!isNetworkConnected()) {
            showSimpleDialog(
                    getString(R.string.error),
                    getString(R.string.network_unavailable_message),
                    this::finish
            );
        }
    }

    protected boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    @NonNull
    protected ConnectivityStatus getConnectivityStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (null != networkInfo) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return ConnectivityStatus.WIFI;
            }

            if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                return ConnectivityStatus.MOBILE;
            }
        }

        return ConnectivityStatus.NOT_CONNECTED;
    }

    public enum ConnectivityStatus {
        WIFI,
        MOBILE,
        NOT_CONNECTED
    }

    protected void disableTouchEvents() {
        mTouchEventsEnabled = false;
    }

    protected boolean isVerifyIfAuthorized() {
        return false;
    }

    protected long postDelayedMillisToSignOut() {
        return 0;
    }

    protected boolean isProgressIndicatorAvailableOnVerifyAuthorizationStep() {
        return true;
    }

    private void verifyIfAuthorized() {
        if (isProgressIndicatorAvailableOnVerifyAuthorizationStep()) {
            showProgressIndicator();
        }
        AuthService.getInstance().currentUser().continueWith(task -> {
            if (isProgressIndicatorAvailableOnVerifyAuthorizationStep()) {
                hideProgressIndicator();
            }

            if (task.isCancelled()) {
                showSimpleDialog(
                        getString(R.string.warning),
                        getString(R.string.invocation_canceled),
                        () -> AuthService.getInstance().signOut()
                );
                return null;
            }

            if (task.isFaulted()) {
                Exception e = task.getError();
                showSimpleDialog(
                        getString(R.string.error),
                        Utils.isRelease()
                                ? getString(R.string.error_something_goes_wrong)
                                : e.getMessage(),
                        () -> AuthService.getInstance().signOut()
                );
                Log.e(TAG, "Unable to get current user due error", e);
                return null;
            }

            if (task.getResult() == null) {
                new Handler().postDelayed(AuthService.getInstance()::signOut, postDelayedMillisToSignOut());
                return null;
            }

            onPostSignIn();

            return null;
        }, Task.UI_THREAD_EXECUTOR);
    }

    protected void gotoSignIn() {
        if (this instanceof SignInActivity) {
            return;
        }

        Intent intent = new Intent(this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    protected void gotoMain() {
        if (this instanceof MainActivity) {
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    protected void gotoCategory(@NonNull AppListItemEntity navigationItem) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_CATEGORY_LOCATION, navigationItem);
        startActivity(intent);
    }

    protected void gotoAr(@NonNull ComponentEntity component) {
        if (this instanceof ArActivity) {
            return;
        }

        Intent intent = new Intent(this, ArActivity.class);
        intent.putExtra(ArActivity.EXTRA_COMPONENT, component);
        startActivity(intent);
    }

    protected void showProgressIndicator() {
        Utils.runOnUiThread(() -> {
            if (mProgressIndicator != null) {
                mProgressIndicator.setVisibility(View.VISIBLE);
                return;
            }

            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.show();
        });
    }

    protected void hideProgressIndicator() {
        hideProgressIndicator(View.GONE);
    }

    protected void hideProgressIndicator(int visibility) {
        Utils.runOnUiThread(() -> {
            if (mProgressIndicator != null) {
                mProgressIndicator.setVisibility(visibility);
            }

            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
        });
    }

    @Nullable
    protected ProgressIndicatorModal showProgressIndicatorModal() {
        if (isFinishing()) {
            return null;
        }

        ProgressDialogFragment dialogFragment = new ProgressDialogFragment();
        dialogFragment.setValue(0.0f);
        dialogFragment.setCancelable(false);
        dialogFragment.show(getSupportFragmentManager(), "ProgressDialogFragment");

        return dialogFragment;
    }

    protected void hideProgressIndicatorModal(ProgressIndicatorModal progressIndicatorModal) {
        if (isFinishing()) {
            return;
        }

        if (progressIndicatorModal != null) {
            progressIndicatorModal.setValue(1.0f);
            progressIndicatorModal.dismiss();
        }
    }

    protected void showMessage(@NonNull String message) {
        if (message.trim().isEmpty()) {
            return;
        }

        Utils.runOnUiThread(() -> {
            Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        });
    }

    protected boolean isTablet() {
        return getResources().getBoolean(R.bool.is_tablet);
    }

    protected void showSimpleDialog(
            String title,
            String message
    ) {
        showSimpleDialog(title, message, getString(R.string.ok));
    }

    protected void showSimpleDialog(
            String title,
            String message,
            String buttonTitle
    ) {
        showSimpleDialog(title, message, buttonTitle, R.color.colorPositive, null);
    }

    protected void showSimpleDialog(
            String title,
            String message,
            final SimpleDialogDelegate delegate
    ) {
        showSimpleDialog(title, message, getString(R.string.ok), R.color.colorPositive, delegate);
    }

    protected void showSimpleDialog(
            String title,
            String message,
            String buttonTitle,
            int buttonColorResId,
            final SimpleDialogDelegate delegate
    ) {
        if (isFinishing()) {
            return;
        }

        Utils.runOnUiThread(() -> {
            SimpleDialogFragment dialogFragment = new SimpleDialogFragment();
            dialogFragment.setTitle(title);
            dialogFragment.setMessage(message);

            dialogFragment.setButtonTitle(buttonTitle);
            dialogFragment.setButtonColorResId(buttonColorResId);

            if (delegate != null) {
                dialogFragment.setDelegate(delegate);
            }
            dialogFragment.setCancelable(false);

            dialogFragment.show(getSupportFragmentManager(), "SimpleDialogFragment");
        });
    }

    protected void showConfirmDialog(
            String title,
            String message,
            String positiveButtonTitle,
            String negativeButtonTitle,
            final ConfirmDialogDelegate delegate
    ) {
        showConfirmDialog(
                title,
                message,
                positiveButtonTitle,
                R.color.colorPositive,
                negativeButtonTitle,
                R.color.colorNegative,
                delegate
        );
    }

    protected void showConfirmDialog(
            String title,
            String message,
            String positiveButtonTitle,
            int positiveButtonColorResId,
            String negativeButtonTitle,
            int negativeButtonColorResId,
            final ConfirmDialogDelegate delegate
    ) {
        if (isFinishing()) {
            return;
        }

        Utils.runOnUiThread(() -> {
            ConfirmDialogFragment dialogFragment = new ConfirmDialogFragment();
            dialogFragment.setTitle(title);
            dialogFragment.setMessage(message);

            dialogFragment.setPositiveButtonTitle(positiveButtonTitle);
            dialogFragment.setPositiveButtonColorResId(positiveButtonColorResId);

            dialogFragment.setNegativeButtonTitle(negativeButtonTitle);
            dialogFragment.setNegativeButtonColorResId(negativeButtonColorResId);

            if (delegate != null) {
                dialogFragment.setDelegate(delegate);
            }
            dialogFragment.setCancelable(false);

            dialogFragment.show(getSupportFragmentManager(), "ConfirmDialogFragment");
        });
    }

    protected interface SimpleDialogDelegate {
        void onClosed();
    }

    protected interface ConfirmDialogDelegate extends SimpleDialogDelegate {
        void onOk();
        void onCancel();
    }

    protected interface ProgressIndicatorModal {
        void dismiss();
        void setValue(float value);
    }

    protected void showWebView(@NonNull Uri uri) {
        if (isFinishing()) {
            return;
        }
        Utils.runOnUiThread(() -> {
            try {
                View sheetView = getLayoutInflater().inflate(R.layout.view_myweb, null);

                // Invoke setAcceptCookie(true) before you initialize your webview
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.setAcceptCookie(true);

                WebView webView = sheetView.findViewById(R.id.myWebView);

                WebSettings webSettings = webView.getSettings();
                webSettings.setLoadsImagesAutomatically(true);

                webSettings.setSupportZoom(true);
                webSettings.setBuiltInZoomControls(true);

                // webSettings.setSupportMultipleWindows(true);

                webSettings.setJavaScriptEnabled(true);
                webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

                webSettings.setAllowContentAccess(true);
                webSettings.setAllowFileAccess(true);
                webSettings.setDomStorageEnabled(true);
                webSettings.setAppCacheEnabled(true);
                webSettings.setDatabaseEnabled(true);

                // NOTE [smuravev] If we enable geolocation in webview then also add the following permissions in the AndroidManifest.xml
                //                   <uses-permission android:name="android.Manifest.permission.ACCESS_COARSE_LOCATION" />
                //                   <uses-permission android:name="android.Manifest.permission.ACCESS_FINE_LOCATION" />
                //                 Also, see more details what to do extra in the JavaDoc of setGeolocationEnabled() method.
                // webSettings.setGeolocationEnabled(true);

                // Trick to accept cokies, see:
                // https://stackoverflow.com/questions/33998688/webview-cannot-accept-cookies
                cookieManager.setAcceptThirdPartyCookies(webView, true);
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        return false;
                    }
                });

                webView.loadUrl(uri.toString());

                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
                bottomSheetDialog.setContentView(sheetView);
                bottomSheetDialog.setCanceledOnTouchOutside(false);
                bottomSheetDialog.setCancelable(true);
                bottomSheetDialog.show();
            } catch (Exception e) {
                Log.e(TAG, "Unable to open web view due error", e);
            }
        });
    }
}
