package io.configwise.android.sdk_example.controllers.main;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import bolts.Task;
import io.configwise.android.sdk_example.R;
import io.configwise.android.sdk_example.Utils;
import io.configwise.android.sdk_example.controllers.ToolbarAwareBaseActivity;
import io.configwise.sdk.domain.AppListItemEntity;
import io.configwise.sdk.domain.ComponentEntity;
import io.configwise.sdk.eventbus.AppListItemCreatedEvent;
import io.configwise.sdk.eventbus.AppListItemDeletedEvent;
import io.configwise.sdk.eventbus.AppListItemUpdatedEvent;
import io.configwise.sdk.eventbus.ComponentDeletedEvent;
import io.configwise.sdk.eventbus.ComponentUpdatedEvent;
import io.configwise.sdk.services.AppListItemService;
import io.configwise.sdk.services.ComponentService;
import io.configwise.sdk.services.ModelService;


public class MainActivity extends ToolbarAwareBaseActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    public static final String EXTRA_CATEGORY_LOCATION = MainActivity.class.getName() + ".extra_component";

    private RecyclerView mCatalogView;

    private CatalogAdapter mCatalogAdapter;

    @Nullable
    private AppListItemEntity mCurrentCategoryLocation;

    @Override
    protected int contentViewResId() {
        return R.layout.activity_main;
    }

    @Override
    protected boolean isVerifyIfAuthorized() {
        return true;
    }

    @Override
    protected boolean isBackButtonVisible() {
        return mCurrentCategoryLocation != null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCurrentCategoryLocation = getIntent().getParcelableExtra(EXTRA_CATEGORY_LOCATION);

        mCatalogView = findViewById(R.id.catalogView);
        mCatalogView.setLayoutManager(new LinearLayoutManager(this));
//        mCatalogView.setItemAnimator(new DefaultItemAnimator());

        mCatalogAdapter = new CatalogAdapter(this);
        mCatalogAdapter.setCatalogActionsDelegate(new CatalogAdapter.CatalogActionsDelegate() {
            @Override
            public void onDetailsSelected(@NonNull AppListItemEntity item, int position, View v) {
                if (item.isMainProduct()) {
                    ComponentEntity component = item.getComponent();
                    if (component != null) {
                        Uri uri = component.getProductUri();
                        if (uri != null) {
                            showWebView(uri);
                        }
                    }
                }
            }

            @Override
            public void onArSelected(@NonNull AppListItemEntity productItem, int position, View v) {
                final ComponentEntity component = productItem.getComponent();
                if (component == null) {
                    return;
                }

                if (!component.isAndroidModelFileExist()) {
                    showSimpleDialog(
                            getString(R.string.error),
                            getString(R.string.component_model_no_model),
                            getString(R.string.ok)
                    );
                    return;
                }

                if (component.isAndroidModelFileCached()) {
                    gotoArOrCanvas(component);
                    return;
                }

                showProgressIndicator();
                ComponentService.getInstance().obtainFilesSizeByComponent(component, true).continueWith(task -> {
                    hideProgressIndicator();

                    if (task.isCancelled()) {
                        Log.w(TAG, String.format("Unable to obtain 'total assets size' of '%s' product due canceled.", component.getGenericName()));
                    }
                    if (task.isFaulted()) {
                        Exception e = task.getError();
                        Log.e(TAG, String.format("Unable to obtain 'total assets size' of '%s' product due error.", component.getGenericName()), e);
                    }
                    if (!task.isCompleted()) {
                        Log.w(TAG, String.format("Unable to obtain 'total assets size' of '%s' product due not completed.", component.getGenericName()));
                    }

                    Long bytes = task.getResult();
                    final String totalSize = bytes != null && bytes > 0
                            ? String.format("%.0f MB", (float) bytes / 1024.0f / 1024.0f)
                            : "unknown";

                    showConfirmDialog(
                            getString(R.string.product_confirm_downloading_title),
                            getString(R.string.product_confirm_downloading_message, totalSize),
                            getString(R.string.ok),
                            getString(R.string.cancel),
                            new ConfirmDialogDelegate() {
                                @Override
                                public void onClosed() {
                                }

                                @Override
                                public void onCancel() {
                                }

                                @Override
                                public void onOk() {
                                    final ProgressIndicatorModal progressIndicator = showProgressIndicatorModal();

                                    ComponentService.getInstance().obtainAllVariancesByComponent(component)
                                            .onSuccessTask(task -> {
                                                List<ComponentEntity> components = new ArrayList<>();
                                                components.add(component);
                                                components.addAll(task.getResult());

                                                return ModelService.getInstance().downloadComponentsModels(components, progress -> {
                                                    if (progressIndicator != null) {
                                                        Utils.runOnUiThread(() -> {
                                                            progressIndicator.setValue(progress);
                                                        });
                                                    }
                                                });
                                            })
                                            .continueWith(task -> {
                                                new Handler().postDelayed(() -> {
                                                    hideProgressIndicatorModal(progressIndicator);

                                                    if (task.isCancelled()) {
                                                        showSimpleDialog(
                                                                getString(R.string.error),
                                                                getString(R.string.component_model_loading_canceled),
                                                                getString(R.string.ok)
                                                        );
                                                        return;
                                                    }

                                                    if (task.isFaulted()) {
                                                        Exception e = task.getError();
                                                        Log.e(TAG, "Unable to load '" + component.getGenericName() + "' component of product due error", e);

                                                        showSimpleDialog(
                                                                getString(R.string.error),
                                                                Utils.isRelease()
                                                                        ? getString(R.string.error_something_goes_wrong)
                                                                        : getString(R.string.component_model_loading_error, e.getMessage()),
                                                                getString(R.string.ok)
                                                        );
                                                        return;
                                                    }

                                                    if (!task.isCompleted()) {
                                                        showSimpleDialog(
                                                                getString(R.string.error),
                                                                getString(R.string.component_model_loading_not_completed),
                                                                getString(R.string.ok)
                                                        );
                                                        return;
                                                    }

                                                    gotoArOrCanvas(component);
                                                }, 1000);

                                                return null;
                                            }, Task.UI_THREAD_EXECUTOR);
                                }
                            }
                    );

                    return null;
                }, Task.UI_THREAD_EXECUTOR);
            }

            @Override
            public void onCategorySelected(@NonNull AppListItemEntity navigationItem, int position, View v) {
                gotoCategory(navigationItem);
            }
        });
        mCatalogView.setAdapter(mCatalogAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadData().continueWith(task -> {
            mCatalogAdapter.setDataSource(task.getResult());
            return null;
        });
    }

    @Subscribe
    public void onEventAppListItemCreated(AppListItemCreatedEvent event) {
        AppListItemEntity appListItem = event.appListItem;
        if (mCurrentCategoryLocation == null) {
            if (appListItem.getParent() == null) {
                mCatalogAdapter.addOrUpdateItem(appListItem);
            }
        } else {
            if (mCurrentCategoryLocation.equals(appListItem.getParent())) {
                mCatalogAdapter.addOrUpdateItem(appListItem);
            }
        }
    }

    @Subscribe
    public void onEventAppListItemUpdated(AppListItemUpdatedEvent event) {
        AppListItemEntity appListItem = event.appListItem;
        if (mCurrentCategoryLocation == null) {
            if (appListItem.getParent() == null) {
                mCatalogAdapter.addOrUpdateItem(appListItem);
            } else {
                AppListItemEntity existingAppListItem = mCatalogAdapter.getDataSource().stream()
                        .filter(it -> it.equals(appListItem))
                        .findFirst()
                        .orElse(null);

                if (existingAppListItem != null) {
                    mCatalogAdapter.removeItem(existingAppListItem);
                }
            }
        } else {
            if (mCurrentCategoryLocation.equals(appListItem.getParent())) {
                mCatalogAdapter.addOrUpdateItem(appListItem);
            } else {
                AppListItemEntity existingAppListItem = mCatalogAdapter.getDataSource().stream()
                        .filter(it -> it.equals(appListItem))
                        .findFirst()
                        .orElse(null);

                if (existingAppListItem != null) {
                    mCatalogAdapter.removeItem(existingAppListItem);
                }
            }
        }
    }

    @Subscribe
    public void onEventAppListItemDeleted(AppListItemDeletedEvent event) {
        mCatalogAdapter.removeItem(event.appListItem);
    }

    @Subscribe
    public void onEventComponentUpdated(ComponentUpdatedEvent event) {
        mCatalogAdapter.updateItemsByComponent(event.component, event.component);
    }

    @Subscribe
    public void onEventComponentDeleted(ComponentDeletedEvent event) {
        mCatalogAdapter.updateItemsByComponent(event.component, null);
    }

    @Override
    protected void onPostSignOut() {
        super.onPostSignOut();

        mCatalogAdapter.setDataSource(null);
    }

    private void gotoArOrCanvas(@NonNull ComponentEntity component) {
        if (io.configwise.sdk.Utils.isArCompatible(this)) {
            gotoAr(component);
        } else {
            showSimpleDialog(
                    getString(R.string.sceneform_incompatible_title),
                    getString(R.string.sceneform_incompatible_message)
            );
        }
    }

    @NonNull
    private Task<List<AppListItemEntity>> loadData() {
        showProgressIndicator();
        return AppListItemService.getInstance().obtainAllAppListItemsByCurrentCatalogAndParent(mCurrentCategoryLocation)
                .continueWithTask(task -> {
                    hideProgressIndicator();

                    if (task.isCancelled()) {
                        Log.w(TAG, "Unable to obtain appListItems due cancelled.");
                    }
                    if (task.isFaulted()) {
                        Log.e(TAG, "Unable to obtain appListItems due error.", task.getError());
                    }
                    if (!task.isCompleted()) {
                        Log.w(TAG, "Unable to obtain appListItems due not completed.");
                    }

                    return task;
                });
    }
}
