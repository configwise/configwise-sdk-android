package io.configwise.android.sdk_example.controllers.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import java.util.List;
import java.util.Objects;

import io.configwise.sdk.domain.AppListItemEntity;

public class AppListItemEntityDiffCallback extends DiffUtil.Callback {

    @NonNull
    private final List<AppListItemEntity> mOldDataSource;

    @NonNull
    private final List<AppListItemEntity> mNewDataSource;

    public AppListItemEntityDiffCallback(@NonNull List<AppListItemEntity> oldDataSource, @NonNull List<AppListItemEntity> newDataSource) {
        this.mOldDataSource = oldDataSource;
        this.mNewDataSource = newDataSource;
    }

    @Override
    public int getOldListSize() {
        return mOldDataSource.size();
    }

    @Override
    public int getNewListSize() {
        return mNewDataSource.size();
    }

    @Override
    public boolean areItemsTheSame(int oldPosition, int newPosition) {
        final AppListItemEntity oldItem = mOldDataSource.get(oldPosition);
        final AppListItemEntity newItem = mNewDataSource.get(newPosition);

        return Objects.equals(oldItem, newItem);
    }

    @Override
    public boolean areContentsTheSame(int oldPosition, int newPosition) {
        final AppListItemEntity oldItem = mOldDataSource.get(oldPosition);
        final AppListItemEntity newItem = mNewDataSource.get(newPosition);

        return oldItem.getType() == newItem.getType()
                && oldItem.isEnabled() == newItem.isEnabled()
                && oldItem.isShowPreview() == newItem.isShowPreview()
                && oldItem.getLabel().equals(newItem.getLabel())
                && oldItem.getDescription().equals(newItem.getDescription())
                && Objects.equals(oldItem.getImagePath(), newItem.getImagePath())
                && Objects.equals(oldItem.getColorOfText(), newItem.getColorOfText())
                ;
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldPosition, int newPosition) {
        // NOTE [smuravev] Implement method if you're going to use ItemAnimator
        return super.getChangePayload(oldPosition, newPosition);
    }
}
