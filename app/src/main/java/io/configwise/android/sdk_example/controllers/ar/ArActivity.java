package io.configwise.android.sdk_example.controllers.ar;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentContainerView;

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
import io.configwise.sdk.ar.TransformableNode;
import io.configwise.sdk.domain.ComponentEntity;
import io.configwise.sdk.services.ModelService;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class ArActivity extends ToolbarAwareBaseActivity {

    private static final String TAG = ArActivity.class.getSimpleName();

    public static final String EXTRA_COMPONENT = ArActivity.class.getName() + ".extra_component";

    private ArFragment mArFragment;

    private FragmentContainerView mArFragmentContainerView;

    private View mPlaneDiscoveryHelpMessage;

    private View mHelpMessageContainer;

    private TextView mHelpMessage;

    private ImageButton mProductAddButton;

    private ImageButton mProductDeleteButton;

    private ImageButton mProductConfirmButton;

    private ImageButton mProductInfoButton;

    @Nullable
    private ComponentEntity mComponent;

    @Nullable
    private TransformableNode mModelNode;

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


        mComponent = getIntent().getParcelableExtra(EXTRA_COMPONENT);
        if (mComponent == null) {
            showMessage(getString(R.string.product_is_null));
            finish();
            return;
        }
        if (!mComponent.isVisible()) {
            showMessage(getString(R.string.product_unsupported));
            finish();
            return;
        }

        mProductAddButton = findViewById(R.id.productAddButton);
        mProductAddButton.setVisibility(View.GONE);

        mProductDeleteButton = findViewById(R.id.productDeleteButton);
        mProductDeleteButton.setVisibility(View.GONE);

        mProductConfirmButton = findViewById(R.id.productDeleteButton);
        mProductConfirmButton.setVisibility(View.GONE);

        mProductInfoButton = findViewById(R.id.productInfoButton);
        mProductInfoButton.setVisibility(mComponent.isProductUriExist() ? View.VISIBLE : View.GONE);

        mPlaneDiscoveryHelpMessage = findViewById(R.id.arPlaneDiscoveryHelpMessage);
        mHelpMessageContainer = findViewById(R.id.arHelpMessageContainer);
        mHelpMessage = findViewById(R.id.arHelpMessage);

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

            mArFragment.setDelegate(new ArFragment.Delegate() {

                @Override
                public void onPlaneDetected(@NonNull Plane plane, @NonNull Anchor anchor) {
                    if (mComponent == null) {
                        return;
                    }

                    if (mModelNode != null) {
                        return;
                    }

                    // Attach a node to the anchor with the scene as the parent
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(mArFragment.getArSceneView().getScene());

                    // Create a new TransformableNode that will carry our object
                    mModelNode = new TransformableNode(mArFragment.getTransformationSystem());
                    mModelNode.setFloating(mComponent.isFloating());
                    mModelNode.setDelegate(new TransformableNode.Delegate() {
                        @Override
                        public void onSelected() {
                            showHelpMessage(
                                    getString(R.string.ar_place_product_help_message),
                                    5000
                            );
                            mProductAddButton.setVisibility(View.GONE);
                            mProductConfirmButton.setVisibility(View.VISIBLE);
                            mProductDeleteButton.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onDeselected() {
                            mProductAddButton.setVisibility(View.VISIBLE);
                            mProductConfirmButton.setVisibility(View.GONE);
                            mProductDeleteButton.setVisibility(View.GONE);
                        }
                    });
                    mModelNode.setParent(anchorNode);

                    mArFragment.hidePlaneDiscoveryInstruction();
                    mArFragment.setShowPlaneDiscoveryOnResume(false);

                    loadModel(mComponent, mModelNode);
                }

                @Override
                public void onPlaneDiscoveryInstructionShown() {
                    showHelpMessage(getString(R.string.ar_scan_environment_help_message));
                    new Handler().postDelayed(() -> {
                        if (mModelNode == null) {
                            showPlaneDiscoveryHelpMessage();
                        }
                    }, 5000);
                }

                @Override
                public void onPlaneDiscoveryInstructionHidden() {
                    hidePlaneDiscoveryHelpMessage();
                }
            });
        }
    }

    private void loadModel(@NonNull ComponentEntity component, @NonNull TransformableNode modelNode) {
        showProgressIndicator();
        ModelService.getInstance().loadComponentModel(this, component)
                .continueWith(task -> {
                    hideProgressIndicator();

                    if (task.isCancelled()) {
                        showSimpleDialog(
                                getString(R.string.error),
                                getString(R.string.component_model_loading_canceled)
                        );
                        return null;
                    }
                    if (task.isFaulted()) {
                        Exception e = task.getError();
                        Log.e(TAG, "Unable to load model due error", e);
                        showSimpleDialog(
                                getString(R.string.error),
                                Utils.isRelease()
                                        ? getString(R.string.error_something_goes_wrong)
                                        : getString(R.string.component_model_loading_error, e.getMessage())
                        );
                        return null;
                    }
                    if (!task.isCompleted()) {
                        showSimpleDialog(
                                getString(R.string.error),
                                getString(R.string.component_model_loading_not_completed)
                        );
                        return null;
                    }

                    modelNode.setRenderable(task.getResult());
                    modelNode.select();

                    return null;
                }, Task.UI_THREAD_EXECUTOR);
    }

    public void onClickProductDeleteButton(View view) {
        if (mModelNode == null) {
            return;
        }
        mModelNode.deselect();
        mModelNode.setParent(null);
        mModelNode = null;
        mComponent = null;

        // TODO [smuravev] Implement ArActivity.onClickProductDeleteButton() through invocation of necessary function in the ArFragment
    }

    public void onClickProductAddButton(View view) {
        // TODO [smuravev] Implement ArActivity.onClickProductAddButton()
    }

    public void onClickProductConfirmButton(View view) {
        if (mModelNode != null) {
            mModelNode.deselect();
        }

        // TODO [smuravev] Implement ArActivity.onClickProductConfirmButton()
    }

    public void onClickProductInfoButton(View view) {
        if (mComponent == null) {
            return;
        }

        Uri uri = mComponent.getProductUri();
        if (uri != null) {
            showWebView(uri);
        }
    }

    private void showHelpMessage(String message) {
        showHelpMessage(message, -1);
    }

    private void showHelpMessage(String message, int hideAfterMillis) {
        hidePlaneDiscoveryHelpMessage();
        mHelpMessage.setText(message);
        mHelpMessageContainer.setVisibility(View.VISIBLE);

        if (hideAfterMillis > 0) {
            new Handler().postDelayed(() -> hideHelpMessage(), hideAfterMillis);
        }
    }

    private void hideHelpMessage() {
        mHelpMessageContainer.setVisibility(View.GONE);
        mHelpMessage.setText(null);
    }

    private void showPlaneDiscoveryHelpMessage() {
        showPlaneDiscoveryHelpMessage(-1);
    }

    private void showPlaneDiscoveryHelpMessage(int hideAfterMillis) {
        hideHelpMessage();
        mPlaneDiscoveryHelpMessage.setVisibility(View.VISIBLE);

        if (hideAfterMillis > 0) {
            new Handler().postDelayed(() -> hidePlaneDiscoveryHelpMessage(), hideAfterMillis);
        }
    }

    private void hidePlaneDiscoveryHelpMessage() {
        mPlaneDiscoveryHelpMessage.setVisibility(View.GONE);
    }
}
