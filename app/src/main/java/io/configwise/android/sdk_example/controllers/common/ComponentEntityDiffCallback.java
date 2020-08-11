package io.configwise.android.sdk_example.controllers.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import java.util.List;
import java.util.Objects;

import io.configwise.sdk.domain.ComponentEntity;

public class ComponentEntityDiffCallback extends DiffUtil.Callback {

    @NonNull
    private final List<ComponentEntity> mOldDataSource;

    @NonNull
    private final List<ComponentEntity> mNewDataSource;

    public ComponentEntityDiffCallback(@NonNull List<ComponentEntity> oldDataSource, @NonNull List<ComponentEntity> newDataSource) {
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
        final ComponentEntity oldItem = mOldDataSource.get(oldPosition);
        final ComponentEntity newItem = mNewDataSource.get(newPosition);

        return Objects.equals(oldItem, newItem);
    }

    @Override
    public boolean areContentsTheSame(int oldPosition, int newPosition) {
        final ComponentEntity oldItem = mOldDataSource.get(oldPosition);
        final ComponentEntity newItem = mNewDataSource.get(newPosition);

        return Objects.equals(oldItem.getUpdatedAt(), newItem.getUpdatedAt());
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldPosition, int newPosition) {
        // NOTE [smuravev] Implement method if you're going to use ItemAnimator
        return super.getChangePayload(oldPosition, newPosition);
    }
}