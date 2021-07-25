package io.configwise.android.sdk_example.controllers.ar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.parse.boltsinternal.Task;
import io.configwise.android.sdk_example.R;
import io.configwise.android.sdk_example.Utils;
import io.configwise.android.sdk_example.controllers.common.ComponentEntityDiffCallback;
import io.configwise.android.sdk_example.controllers.common.ResizeMaxWidthPicassoTransformation;
import io.configwise.sdk.domain.ComponentEntity;
import io.configwise.sdk.services.ComponentService;
import io.configwise.sdk.services.DownloadingService;

public class TrayCatalogAdapter extends RecyclerView.Adapter<TrayCatalogAdapter.ItemViewHolder> {

    private static final String TAG = TrayCatalogAdapter.class.getSimpleName();

    public static final int ITEM_TYPE_PRODUCT = 1;

    @NonNull
    private final List<ComponentEntity> mOriginalDataSource = new ArrayList<>();

    @NonNull
    private final List<ComponentEntity> mFilteredSortedDataSource = new ArrayList<>();

    private Context mContext;

    private Delegate mDelegate;

    public TrayCatalogAdapter(Context context) {
        mContext = context;
    }

    @Override
    @NonNull
    public TrayCatalogAdapter.ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        View view = LayoutInflater.from(context).inflate(R.layout.listitem_tray_product, parent, false);

        return new TrayCatalogAdapter.ItemViewHolder(view, ITEM_TYPE_PRODUCT);
    }

    @Override
    public void onBindViewHolder(@NonNull TrayCatalogAdapter.ItemViewHolder holder, int position) {
        final ComponentEntity item = getItem(position);

        if (holder.mImageView != null) {
            holder.mImageView.setImageDrawable(null);

            String fileKey = item.getThumbnailFilePath();
            if (fileKey != null) {
                DownloadingService.getInstance().download(fileKey).onSuccess(task -> {
                    File file = task.getResult();
                    if (file != null) {
                        Picasso.get()
                                .load(file)
                                .transform(new ResizeMaxWidthPicassoTransformation(360))
                                .into(holder.mImageView);
                    }
                    return null;
                }, Task.UI_THREAD_EXECUTOR);
            }
        }

        if (holder.mTitleView != null) {
            Utils.runOnUiThread(() -> {
                holder.mTitleView.setText(item.getGenericName());
            });
        }

        if (holder.mDescriptionView != null) {
            ComponentService.getInstance().countVariancesByComponent(item).onSuccess(task -> {
                int numberOfVariances = task.getResult();
                holder.mDescriptionView.setText(numberOfVariances > 0
                        ? mContext.getString(R.string.product_n_variances_available, numberOfVariances + 1)
                        : null
                );
                return null;
            }, Task.UI_THREAD_EXECUTOR);
        }
    }

    @Override
    public int getItemCount() {
        return mFilteredSortedDataSource.size();
    }

    @NonNull
    protected ComponentEntity getItem(int position) {
        return mFilteredSortedDataSource.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return ITEM_TYPE_PRODUCT;
    }

    public void setDelegate(Delegate delegate) {
        mDelegate = delegate;
    }

    @NonNull
    public List<ComponentEntity> getDataSource() {
        return mOriginalDataSource;
    }

    public void setDataSource(List<ComponentEntity> data) {
        Utils.runOnUiThread(() -> {
            final List<ComponentEntity> _data = data != null ? data : new ArrayList<>();

            if (_data.isEmpty()) {
                mOriginalDataSource.clear();
                mFilteredSortedDataSource.clear();
                notifyDataSetChanged();
                return;
            }

            final List<ComponentEntity> filteredSortedData = filter(_data);
            sort(filteredSortedData);

            if (filteredSortedData.isEmpty()) {
                mOriginalDataSource.clear();
                mOriginalDataSource.addAll(_data);
                mFilteredSortedDataSource.clear();
                notifyDataSetChanged();
                return;
            }

            final ComponentEntityDiffCallback diffCallback = new ComponentEntityDiffCallback(mFilteredSortedDataSource, filteredSortedData);
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

            mOriginalDataSource.clear();
            mOriginalDataSource.addAll(_data);

            mFilteredSortedDataSource.clear();
            mFilteredSortedDataSource.addAll(filteredSortedData);

            diffResult.dispatchUpdatesTo(this);
        });
    }

    private void sort(@NonNull List<ComponentEntity> data) {
        Collections.sort(data, (o1, o2) -> {
            Date o1CreatedAt = o1.getCreatedAt();
            Date o2CreatedAt = o2.getCreatedAt();

            return Long.compare(
                    o1CreatedAt != null ? o1CreatedAt.getTime() : 0,
                    o2CreatedAt != null ? o2CreatedAt.getTime() : 0
            );
        });
    }

    @NonNull
    private List<ComponentEntity> filter(@NonNull List<ComponentEntity> data) {
        return data.stream()
                .filter(item -> {
                    if (item == null) {
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @NonNull
        private int mItemType = RecyclerView.INVALID_TYPE;

        @Nullable
        private ImageView mImageView;

        @Nullable
        private TextView mTitleView;

        @Nullable
        private TextView mDescriptionView;

        ItemViewHolder(View itemView, int itemType) {
            super(itemView);

            mItemType = itemType;

            mImageView = itemView.findViewById(R.id.imageView);
            mTitleView = itemView.findViewById(R.id.labelText);
            mDescriptionView = itemView.findViewById(R.id.descriptionText);

            this.itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mDelegate == null) {
                return;
            }
            if (mItemType != ITEM_TYPE_PRODUCT) {
                return;
            }
            int position = getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) {
                return;
            }

            ComponentEntity item = getItem(position);
            Utils.fadeIn(v);
            mDelegate.onProductSelected(item, position, v);
        }
    }


    public interface Delegate {
        void onProductSelected(@NonNull ComponentEntity component, int position, View v);
    }
}
