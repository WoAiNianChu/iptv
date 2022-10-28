package com.orion.iptv.layout.livechannellist;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import com.orion.iptv.R;
import com.orion.iptv.bean.ChannelGroup;

import java.util.List;
import java.util.Locale;

public class GroupListViewAdapter extends RecyclerView.Adapter<GroupListViewAdapter.ViewHolder> {
    private final String TAG = "GroupListViewAdapter";
    private final Context context;
    private List<ChannelGroup> groups;
    private SelectionTracker<Long> tracker;
    private OnSelectedListener onSelected;

    public interface OnSelectedListener {
        void onSelected(int position);
    }

    public class KeyProvider extends ItemKeyProvider<Long> {
        public KeyProvider(int scope){
            super(scope);
        }

        @Override
        public Long getKey (int position) {
            return (long)position;
        }

        @Override
        public int getPosition(@NonNull Long key) {
            return key.intValue();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final View item;
        private final TextView desc;

        public ViewHolder(View v) {
            super(v);
            item = v.findViewById(R.id.live_channel_list_item);
            desc = item.findViewById(R.id.list_item_desc);
            item.findViewById(R.id.list_item_index).setVisibility(View.GONE);
        }

        public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
            int position = getBindingAdapterPosition();
            return new ItemDetailsLookup.ItemDetails<>() {
                @Override
                public int getPosition() {
                    return position;
                }

                @Override
                public Long getSelectionKey() {
                    return (long)position;
                }

                @Override
                public boolean inSelectionHotspot(@NonNull MotionEvent e) {
                    return true;
                }
            };
        }

        public void setActivated(boolean isActivated) {
            item.setActivated(isActivated);
        }

        public void setContent(final int position, ChannelGroup group) {
            desc.setText(group.name);
        }
    }

    public class ItemLookup extends ItemDetailsLookup<Long> {
        private final RecyclerView view;
        public ItemLookup(RecyclerView view) {
            this.view = view;
        }

        @Nullable
        @Override
        public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
            View v = view.findChildViewUnder(e.getX(), e.getY());
            if (v != null) {
                Log.i(TAG, String.format("got item at (%.2f,%.2f)", e.getX(), e.getY()));
                ViewHolder holder = (ViewHolder) view.getChildViewHolder(v);
                return holder.getItemDetails();
            }
            return null;
        }
    }

    public GroupListViewAdapter(Context context, List<ChannelGroup> groups) {
        this.context = context;
        this.groups = groups;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.live_channel_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Log.i(TAG, String.format(Locale.ENGLISH, "item %d bound", position));
        holder.setActivated(tracker.isSelected((long)position));
        holder.setContent(position, groups.get(position));
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    public void setTracker(SelectionTracker<Long> tracker) {
        tracker.addObserver(new SelectionTracker.SelectionObserver<Long>() {
            @Override
            public void onItemStateChanged(@NonNull Long key, boolean selected) {
                super.onItemStateChanged(key, selected);
                if (!selected || onSelected == null) {
                    return;
                }
                onSelected.onSelected(key.intValue());
            }
        });
        this.tracker = tracker;
    }

    public void setOnSelectedListener(OnSelectedListener listener) {
        onSelected = listener;
    }

    public void setData(List<ChannelGroup> groups) {
        this.groups = groups;
    }
}
