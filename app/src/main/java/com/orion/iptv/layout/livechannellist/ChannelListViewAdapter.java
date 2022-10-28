package com.orion.iptv.layout.livechannellist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;
import com.orion.iptv.R;
import com.orion.iptv.bean.ChannelItem;

import java.util.List;
import java.util.Optional;

public class ChannelListViewAdapter extends RecyclerView.Adapter<ChannelListViewAdapter.ViewHolder> {
    private final String TAG = "ChannelListViewAdapter";
    private final Context context;
    private List<ChannelItem> channels;
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
        private final TextView index;
        private final TextView desc;

        public ViewHolder(View v) {
            super(v);
            item = v.findViewById(R.id.live_channel_list_item);
            index = v.findViewById(R.id.list_item_index);
            desc = v.findViewById(R.id.list_item_desc);
        }

        public Optional<ItemDetailsLookup.ItemDetails<Long>> getItemDetails() {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                return Optional.of(new ItemDetailsLookup.ItemDetails<>() {
                    @Override
                    public int getPosition() {
                        return position;
                    }

                    @Override
                    public Long getSelectionKey() {
                        return (long) position;
                    }

                    @Override
                    public boolean inSelectionHotspot(@NonNull MotionEvent e) {
                        return true;
                    }
                });
            } else {
                return Optional.empty();
            }
        }

        public void setActivated(boolean isActivated) {
            item.setActivated(isActivated);
        }

        public void setContent(final int position, ChannelItem channel) {
            index.setText(String.valueOf(channel.number));
            desc.setText(channel.name);
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
                return holder.getItemDetails().orElse(null);
            }
            return null;
        }
    }

    public ChannelListViewAdapter(Context context, List<ChannelItem> channels) {
        this.context = context;
        this.channels = channels;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.live_channel_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        boolean selected = tracker.isSelected((long)position);
        holder.setActivated(selected);
        holder.setContent(position, channels.get(position));
    }

    @Override
    public int getItemCount() {
        return channels.size();
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

    public void setData(List<ChannelItem> channels) {
        this.channels = channels;
    }
}
