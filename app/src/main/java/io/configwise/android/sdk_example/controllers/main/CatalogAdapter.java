package io.configwise.android.sdk_example.controllers.main;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import bolts.Task;
import io.configwise.android.sdk_example.R;
import io.configwise.android.sdk_example.Utils;
import io.configwise.android.sdk_example.controllers.common.AppListItemEntityDiffCallback;
import io.configwise.android.sdk_example.controllers.common.ResizeMaxWidthPicassoTransformation;
import io.configwise.sdk.domain.AppListItemEntity;
import io.configwise.sdk.domain.AppListItemType;
import io.configwise.sdk.domain.ComponentEntity;
import io.configwise.sdk.services.ComponentService;
import io.configwise.sdk.services.DownloadingService;

import static java.util.stream.Collectors.toList;

public class CatalogAdapter extends RecyclerView.Adapter<CatalogAdapter.ItemViewHolder> {

    private static final String TAG = CatalogAdapter.class.getSimpleName();

    public static final int ITEM_TYPE_PRODUCT = 1;

    public static final int ITEM_TYPE_CATEGORY = 2;

    public static final int ITEM_TYPE_BANNER = 3;

    @NonNull
    private final List<AppListItemEntity> mOriginalDataSource = new ArrayList<>();

    @NonNull
    private final List<AppListItemEntity> mFilteredSortedDataSource = new ArrayList<>();

    private Context mContext;

    @Nullable
    private CatalogActionsDelegate mCatalogActionsDelegate;


    public CatalogAdapter(Context context) {
        mContext = context;
    }

    @Override
    @NonNull
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        switch (viewType) {
            case ITEM_TYPE_PRODUCT:
                return new ItemViewHolder(
                        LayoutInflater.from(context).inflate(R.layout.listitem_product, parent, false),
                        ITEM_TYPE_PRODUCT
                );

            case ITEM_TYPE_CATEGORY:
                return new ItemViewHolder(
                        LayoutInflater.from(context).inflate(R.layout.listitem_category, parent, false),
                        ITEM_TYPE_CATEGORY
                );

            default: // ITEM_TYPE_BANNER and others
                View view = LayoutInflater.from(context).inflate(R.layout.listitem_banner, parent, false);
                return new ItemViewHolder(view, ITEM_TYPE_BANNER);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        final AppListItemEntity item = getItem(position);

        if (holder.mLabelView != null) {
            holder.mLabelView.setVisibility(View.GONE);

            String label = item.getLabel();
            holder.mLabelView.setText(label);

            if (!label.isEmpty()) {
                holder.mLabelView.setVisibility(View.VISIBLE);
            }

            if (item.isMainProduct()) {
                final ComponentEntity component = item.getComponent();
                if (component == null) {
                    return;
                }

                ComponentService.getInstance().countVariancesByComponent(component).onSuccess(task -> {
                    int numberOfVariances = task.getResult();
                    if (numberOfVariances > 0) {
                        holder.mLabelView.setText(label + " (" + numberOfVariances + 1 + ")");
                    } else {
                        holder.mLabelView.setText(label);
                    }
                    return null;
                }, Task.UI_THREAD_EXECUTOR);
            }
        }

        if (holder.mImageView != null) {
            Uri uri = item.getImageUri();
            if (uri != null) {
                DownloadingService.getInstance().download(uri).onSuccess(task -> {
                    File file = task.getResult();
                    if (file != null) {
                        final int maxWidth = 1080;
                        Picasso.get()
                                .load(file)
                                .transform(new ResizeMaxWidthPicassoTransformation(maxWidth))
                                .into(holder.mImageView);
                    }
                    return null;
                }, Task.UI_THREAD_EXECUTOR);
            }
        }

        if (holder.mDescriptionView != null) {
            holder.mDescriptionView.setVisibility(View.GONE);

            String description = item.getDescription();
            holder.mDescriptionView.setText(description);

            if (!description.isEmpty()) {
                holder.mDescriptionView.setVisibility(View.VISIBLE);
            }
        }

        if (holder.mDetailsButton != null) {
            holder.mDetailsButton.setVisibility(View.GONE);

            if (item.isMainProduct()) {
                ComponentEntity component = item.getComponent();
                if (component != null) {
                    Uri uri = component.getProductUri();
                    if (uri != null) {
                        holder.mDetailsButton.setVisibility(View.VISIBLE);
                    }
                }
            } else if (item.isNavigationItem()) {
                holder.mDetailsButton.setVisibility(View.VISIBLE);
            }
        }

        if (holder.mArButton != null) {
            holder.mArButton.setVisibility(View.GONE);

            if (item.isMainProduct()) {
                ComponentEntity component = item.getComponent();
                if (component != null && component.isAndroidModelFileExist()) {
                    holder.mArButton.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return mFilteredSortedDataSource.size();
    }

    @NonNull
    protected AppListItemEntity getItem(int position) {
        return mFilteredSortedDataSource.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        AppListItemEntity item = getItem(position);

        if (item.getType() == AppListItemType.MAIN_PRODUCT) {
            return ITEM_TYPE_PRODUCT;
        } else if (item.getType() == AppListItemType.NAVIGATION_ITEM) {
            return ITEM_TYPE_CATEGORY;
        } else {
            return ITEM_TYPE_BANNER;
        }
    }

    public void setCatalogActionsDelegate(CatalogActionsDelegate delegate) {
        mCatalogActionsDelegate = delegate;
    }

    @NonNull
    public List<AppListItemEntity> getDataSource() {
        return mOriginalDataSource;
    }

    public void setDataSource(List<AppListItemEntity> data) {
        Utils.runOnUiThread(() -> {
            final List<AppListItemEntity> _data = data != null ? data : new ArrayList<>();

            if (_data.isEmpty()) {
                mOriginalDataSource.clear();
                mFilteredSortedDataSource.clear();
                notifyDataSetChanged();
                return;
            }

            final List<AppListItemEntity> filteredSortedData = filter(_data);
            sort(filteredSortedData);

            if (filteredSortedData.isEmpty()) {
                mOriginalDataSource.clear();
                mOriginalDataSource.addAll(_data);
                mFilteredSortedDataSource.clear();
                notifyDataSetChanged();
                return;
            }

            final AppListItemEntityDiffCallback diffCallback = new AppListItemEntityDiffCallback(mFilteredSortedDataSource, filteredSortedData);
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

            mOriginalDataSource.clear();
            mOriginalDataSource.addAll(_data);

            mFilteredSortedDataSource.clear();
            mFilteredSortedDataSource.addAll(filteredSortedData);

            diffResult.dispatchUpdatesTo(this);
        });
    }

    public void addOrUpdateItem(@NonNull AppListItemEntity item) {
        final List<AppListItemEntity> newData = getDataSource().stream()
                .filter(it -> !it.equals(item))
                .collect(toList());

        newData.add(item);

        setDataSource(newData);
    }

    public void removeItem(@NonNull AppListItemEntity item) {
        final List<AppListItemEntity> newData = getDataSource().stream()
                .filter(it -> !it.equals(item))
                .collect(toList());

        setDataSource(newData);
    }

    public void updateItemsByComponent(@NonNull ComponentEntity component, @Nullable ComponentEntity newValue) {
        final List<AppListItemEntity> newData = new ArrayList<>();

        for (AppListItemEntity it : getDataSource()) {
            AppListItemEntity clone = it;
            if (component.equals(it.getComponent())) {
                clone = AppListItemEntity.spawn(it);
                clone.setComponent(newValue);
            }
            newData.add(clone);
        }

        setDataSource(newData);
    }

    private void sort(@NonNull List<AppListItemEntity> data) {
        Collections.sort(data, (o1, o2) -> Integer.compare(o1.getIndex(), o2.getIndex()));
    }

    @NonNull
    private List<AppListItemEntity> filter(@NonNull List<AppListItemEntity> data) {
        return data.stream()
                .filter(item -> {
                    if (item == null) {
                        return false;
                    }
                    if (!item.isEnabled()) {
                        return false;
                    }

                    if (item.isOverlayImage()) {
                        return item.isImageExist()
                                || !item.getLabel().isEmpty()
                                || !item.getDescription().isEmpty();
                    }
                    else if (item.isNavigationItem()) {
                        return !item.getLabel().isEmpty()
                                || !item.getDescription().isEmpty();
                    }
                    else if (item.isMainProduct()) {
                        final ComponentEntity component = item.getComponent();
                        if (component == null) {
                            return false;
                        }

                        return component.isVisible();
                    }

                    return false;
                })
                .collect(toList());
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {

        @Nullable
        private TextView mLabelView;

        @Nullable
        private TextView mDescriptionView;

        @Nullable
        private ImageView mImageView;

        @Nullable
        private ImageButton mArButton;

        @Nullable
        private ImageButton mDetailsButton;

        ItemViewHolder(View itemView, int itemType) {
            super(itemView);

            mImageView = itemView.findViewById(R.id.imageView);
            mLabelView = itemView.findViewById(R.id.labelText);
            mDescriptionView = itemView.findViewById(R.id.descriptionText);
            mDetailsButton = itemView.findViewById(R.id.detailsButton);

            if (itemType == ITEM_TYPE_PRODUCT) {
                mArButton = itemView.findViewById(R.id.arButton);
            }

            if (mArButton != null) {
                mArButton.setOnClickListener(v -> {
                    if (mCatalogActionsDelegate == null) {
                        return;
                    }
                    int position = getAdapterPosition();
                    if (position == RecyclerView.NO_POSITION) {
                        return;
                    }
                    AppListItemEntity item = getItem(position);
                    if (item.isMainProduct()) {
                        final ComponentEntity component = item.getComponent();
                        if (component != null) {
                            Utils.fadeIn(v);
                            mCatalogActionsDelegate.onArSelected(item, position, v);
                        }
                    }
                });
            }

            if (mDetailsButton != null) {
                mDetailsButton.setOnClickListener(v -> {
                    if (mCatalogActionsDelegate == null) {
                        return;
                    }
                    int position = getAdapterPosition();
                    if (position == RecyclerView.NO_POSITION) {
                        return;
                    }
                    AppListItemEntity item = getItem(position);
                    if (item.isNavigationItem()) {
                        Utils.fadeIn(v);
                        mCatalogActionsDelegate.onCategorySelected(item, position, v);
                    } else {
                        Utils.fadeIn(v);
                        mCatalogActionsDelegate.onDetailsSelected(item, position, v);
                    }
                });
            }
        }
    }

    public interface CatalogActionsDelegate {

        void onDetailsSelected(@NonNull AppListItemEntity bannerItem, int position, View v);

        void onArSelected(@NonNull AppListItemEntity productItem, int position, View v);

        void onCategorySelected(@NonNull AppListItemEntity navigationItem, int position, View v);
    }
}
