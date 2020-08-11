package io.configwise.android.sdk_example.controllers;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuInflater;

import io.configwise.android.sdk_example.R;

public abstract class ToolbarAwareBaseActivity extends BaseActivity {

    @Nullable
    protected Toolbar mToolbar;

    @Nullable
    protected ActionBar mActionBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mToolbar = findViewById(R.id.toolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            mToolbar.setNavigationOnClickListener(view -> onBackPressed());
        }

        mActionBar = getSupportActionBar();
        setToolbarTitle(null);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(isBackButtonVisible() && getSupportParentActivityIntent() != null);
//            mActionBar.setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_toolbar, menu);

        return true;
    }

    public void setToolbarTitle(@Nullable String title) {
        title = title != null ? title.trim() : null;

        if (mActionBar != null) {
            mActionBar.setTitle(title);
        }
    }

    protected boolean isBackButtonVisible() {
        return true;
    }
}
