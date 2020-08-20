package io.configwise.android.sdk_example.controllers.ar;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentContainerView;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.ar.core.Anchor;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;

import bolts.Task;
import io.configwise.android.sdk_example.R;
import io.configwise.android.sdk_example.Utils;
import io.configwise.android.sdk_example.controllers.ToolbarAwareBaseActivity;
import io.configwise.sdk.ar.ArFragment;
import io.configwise.sdk.ar.BaseArFragment;
import io.configwise.sdk.ar.ModelNode;
import io.configwise.sdk.domain.ComponentEntity;
import io.configwise.sdk.services.ComponentService;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class ArActivity extends ToolbarAwareBaseActivity {

    private static final String TAG = ArActivity.class.getSimpleName();

    public static final String EXTRA_COMPONENT = ArActivity.class.getName() + ".extra_component";

    @NonNull
    private ArFragment mArFragment;

    private FragmentContainerView mArFragmentContainerView;

    private View mPlaneDiscoveryHelpMessage;

    private View mHelpMessageContainer;

    private TextView mHelpMessage;

    private View mProductToolbar;

    private ImageButton mProductAddButton;

    private ImageButton mProductDeleteButton;

    private ImageButton mProductConfirmButton;

    private ImageButton mProductInfoButton;

    @Nullable
    private ComponentEntity mInitialComponent;

    private boolean mTrayCatalogVisible = false;

    private View mTrayCatalogParentView;

    private View mTrayCatalogContainer;

    private ImageButton mTrayCatalogCloseButton;

    private RecyclerView mTrayCatalogCollectionView;

    private TrayCatalogAdapter mTrayCatalogAdapter;

    @Override
    protected int contentViewResId() {
        return R.layout.activity_ar;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!io.configwise.sdk.Utils.isArCompatible(this)) {
            showMessage(getString(R.string.sceneform_incompatible_message));
            finish();
            return;
        }


        mInitialComponent = getIntent().getParcelableExtra(EXTRA_COMPONENT);
        if (mInitialComponent == null) {
            showMessage(getString(R.string.product_is_null));
            finish();
            return;
        }
        if (!mInitialComponent.isVisible()) {
            showMessage(getString(R.string.product_unsupported));
            finish();
            return;
        }

        mProductToolbar = findViewById(R.id.productToolbar);
        mProductAddButton = findViewById(R.id.productAddButton);
        mProductDeleteButton = findViewById(R.id.productDeleteButton);
        mProductConfirmButton = findViewById(R.id.productConfirmButton);
        mProductInfoButton = findViewById(R.id.productInfoButton);

        mPlaneDiscoveryHelpMessage = findViewById(R.id.arPlaneDiscoveryHelpMessage);
        mHelpMessageContainer = findViewById(R.id.arHelpMessageContainer);
        mHelpMessage = findViewById(R.id.arHelpMessage);

        // Setup tray catalog view
        mTrayCatalogParentView = findViewById(R.id.trayCatalogParent);
        mTrayCatalogContainer = findViewById(R.id.trayCatalogContainer);
        mTrayCatalogCloseButton = findViewById(R.id.trayCatalogCloseButton);
        mTrayCatalogCollectionView = findViewById(R.id.trayCatalogCollectionView);
        mTrayCatalogAdapter = new TrayCatalogAdapter(this);
        mTrayCatalogAdapter.setDelegate((component, position, v) -> {
            mArFragment.addModel(
                    component,
                    null,
                    null,
                    null,
                    null,
                    true
            );
            hideTrayCatalog();
        });
        mTrayCatalogCollectionView.setAdapter(mTrayCatalogAdapter);

        // Setup AR fragment
        mArFragmentContainerView = findViewById(R.id.arFragmentContainerView);
        if (mArFragmentContainerView != null) {
            mArFragment = (ArFragment) getSupportFragmentManager().findFragmentByTag(
                    (String) mArFragmentContainerView.getTag()
            );
            if (mArFragment == null) {
                throw new RuntimeException("Unable to find ArFragment with 'arFragment_tag' tag.");
            }

            // Let's init our AR fragment
            mArFragment.setSelectionVisualizerType(BaseArFragment.SelectionVisualizerType.JUMPING);
            mArFragment.setDelegate(new ArFragmentDelegate());
        }

        // Finally, let's obtain catalog - to show it in the '+' (add product to scene) dialog.
        obtainCatalog();
    }

    public void onClickProductDeleteButton(View view) {
        ModelNode selectedModel = mArFragment.getSelectedModel();
        if (selectedModel == null) {
            return;
        }

        mArFragment.removeModel(selectedModel);
    }

    public void onClickProductAddButton(View view) {
        showTrayCatalog();
        obtainCatalog();
    }

    public void onClickProductConfirmButton(View view) {
        ModelNode selectedModel = mArFragment.getSelectedModel();
        if (selectedModel != null) {
            selectedModel.deselect();
        }
    }

    public void onClickProductInfoButton(View view) {
        ModelNode selectedModel = mArFragment.getSelectedModel();
        if (selectedModel == null) {
            return;
        }

        ComponentEntity component = selectedModel.getComponent();
        Uri uri = component.getProductUri();
        if (uri != null) {
            showWebView(uri);
        }
    }

    public void onClickTrayCatalogCloseButton(View view) {
        hideTrayCatalog();
    }

    private void obtainCatalog() {
        showProgressIndicator();
        ComponentService.getInstance().obtainAllComponentsByCurrentCatalog()
                .onSuccess(task -> {
                    hideProgressIndicator();
                    mTrayCatalogAdapter.setDataSource(task.getResult());
                    return null;
                }, Task.UI_THREAD_EXECUTOR);
    }

    private void refreshUI() {
        ModelNode selectedModel = mArFragment.getSelectedModel();

        if (selectedModel != null) {
            ComponentEntity component = selectedModel.getComponent();

            Utils.runOnUiThread(() -> {
                mProductAddButton.setVisibility(View.GONE);
                mProductDeleteButton.setVisibility(View.VISIBLE);
                mProductConfirmButton.setVisibility(View.VISIBLE);
                mProductInfoButton.setVisibility(component.isProductUriExist() ? View.VISIBLE : View.GONE);
            });
        } else {
            Utils.runOnUiThread(() -> {
                mProductAddButton.setVisibility(View.VISIBLE);
                mProductDeleteButton.setVisibility(View.GONE);
                mProductConfirmButton.setVisibility(View.GONE);
                mProductInfoButton.setVisibility(View.GONE);
            });
        }
    }

    private void showHelpMessage(String message) {
        showHelpMessage(message, -1);
    }

    private void showHelpMessage(String message, int hideAfterMillis) {
        hidePlaneDiscoveryHelpMessage();
        Utils.runOnUiThread(() -> {
            mHelpMessage.setText(message);
            mHelpMessageContainer.setVisibility(View.VISIBLE);
        });

        if (hideAfterMillis > 0) {
            new Handler().postDelayed(() -> hideHelpMessage(), hideAfterMillis);
        }
    }

    private void hideHelpMessage() {
        Utils.runOnUiThread(() -> {
            mHelpMessageContainer.setVisibility(View.GONE);
            mHelpMessage.setText(null);
        });
    }

    private void showPlaneDiscoveryHelpMessage() {
        showPlaneDiscoveryHelpMessage(-1);
    }

    private void showPlaneDiscoveryHelpMessage(int hideAfterMillis) {
        hideHelpMessage();

        Utils.runOnUiThread(() -> {
            mPlaneDiscoveryHelpMessage.setVisibility(View.VISIBLE);
        });

        if (hideAfterMillis > 0) {
            new Handler().postDelayed(() -> hidePlaneDiscoveryHelpMessage(), hideAfterMillis);
        }
    }

    private void hidePlaneDiscoveryHelpMessage() {
        Utils.runOnUiThread(() -> {
            mPlaneDiscoveryHelpMessage.setVisibility(View.GONE);
        });
    }

    private void showTrayCatalog() {
        if (mTrayCatalogVisible) {
            return;
        }

        hideHelpMessage();
        hidePlaneDiscoveryHelpMessage();
        hideProductToolbar();

        Utils.runOnUiThread(() -> {
            mTrayCatalogCloseButton.setVisibility(View.VISIBLE);
            int targetHeight = (int) (mTrayCatalogParentView.getHeight() * 0.8);
            Utils.expandViewVertical(mTrayCatalogContainer, 500, targetHeight, () -> {
                mTrayCatalogVisible = true;
            });
        });
    }

    private void hideTrayCatalog() {
        if (!mTrayCatalogVisible) {
            return;
        }

        Utils.runOnUiThread(() -> {
            Utils.collapseViewVertical(mTrayCatalogContainer, 500, 1, () -> {
                mTrayCatalogCloseButton.setVisibility(View.GONE);
                mTrayCatalogVisible = false;
                showProductToolbar();
            });
        });
    }

    private void showProductToolbar() {
        Utils.runOnUiThread(() -> {
            mProductToolbar.setVisibility(View.VISIBLE);
        });
    }

    private void hideProductToolbar() {
        Utils.runOnUiThread(() -> {
            mProductToolbar.setVisibility(View.GONE);
        });
    }

    private class ArFragmentDelegate implements ArFragment.Delegate {

        @Override
        public void onPlaneDetected(@NonNull Plane plane, @NonNull Anchor anchor) {
            if (mInitialComponent == null) {
                return;
            }

            mArFragment.disablePlaneDiscoveryInstruction();

            // Attach a node to the anchor with the scene as the parent
            final AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(mArFragment.getArSceneView().getScene());

            mArFragment.addModel(
                    mInitialComponent,
                    anchorNode,
                    null,
                    null,
                    null,
                    true
            );

            mInitialComponent = null;
        }

        @Override
        public void onPlaneDiscoveryInstructionShown() {
            showHelpMessage(getString(R.string.ar_scan_environment_help_message));
            new Handler().postDelayed(() -> {
                if (mInitialComponent != null) {
                    showPlaneDiscoveryHelpMessage();
                }
            }, 5000);
        }

        @Override
        public void onPlaneDiscoveryInstructionHidden() {
            hidePlaneDiscoveryHelpMessage();
        }

        @Override
        public void onModelAdded(@NonNull ModelNode model) {
        }

        @Override
        public void onModelDeleted(@NonNull ModelNode model) {
        }

        @Override
        public void onModelSelected(@NonNull ModelNode model) {
            refreshUI();

            showHelpMessage(
                    getString(R.string.ar_place_product_help_message),
                    5000
            );
        }

        @Override
        public void onModelDeselected(@NonNull ModelNode model) {
            refreshUI();
        }

        public void onModelLoadingStarted(@NonNull ModelNode model) {
            showProgressIndicator();
        }

        public void onModelLoadingFinished(
                @NonNull ModelNode model,
                @Nullable Exception e,
                boolean cancelled,
                boolean completed
        ) {
            hideProgressIndicator();

            if (e != null) {
                Log.e(TAG, "Unable to load model due error", e);
                showSimpleDialog(
                        getString(R.string.error),
                        Utils.isRelease()
                                ? getString(R.string.error_something_goes_wrong)
                                : getString(R.string.component_model_loading_error, e.getMessage())
                );
                return;
            }

            if (cancelled) {
                showSimpleDialog(
                        getString(R.string.error),
                        getString(R.string.component_model_loading_canceled)
                );
                return;
            }

            if (!completed) {
                showSimpleDialog(
                        getString(R.string.error),
                        getString(R.string.component_model_loading_not_completed)
                );
                return;
            }
        }
    }
}
