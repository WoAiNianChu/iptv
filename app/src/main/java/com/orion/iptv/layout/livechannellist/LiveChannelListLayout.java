package com.orion.iptv.layout.livechannellist;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.MediaItem;
import com.orion.iptv.R;
import com.orion.iptv.bean.ChannelGroup;
import com.orion.iptv.bean.ChannelItem;
import com.orion.iptv.bean.ChannelManager;
import com.orion.iptv.misc.CancelableRunnable;
import com.orion.iptv.recycleradapter.RecyclerAdapter;
import com.orion.iptv.recycleradapter.ViewHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import android.util.Log;

public class LiveChannelListLayout {
    private final String TAG = "LiveChannelListLayout";
    private final View mLayout;
    private final RecyclerView groupList;
    private final View groupListSpacer;
    private final RecyclerAdapter<ViewHolder<ChannelGroup>, ChannelGroup> groupListViewAdapter;
    private final RecyclerAdapter<ViewHolder<ChannelItem>, ChannelItem> channelListViewAdapter;
    private final View epgSpacer;
    private final RecyclerView epgList;

    private ChannelManager channelManager;
    private final Handler mHandler;
    private CancelableRunnable setVisibilityDelayedTask;
    private OnChannelSelectedListener channelSelectedListener;

    private int selectedGroup = 0;
    private int selectedChannel = 0;

    public boolean getIsVisible() {
        return mLayout.getVisibility() == View.VISIBLE;
    }

    public interface OnChannelSelectedListener {
        void onChannelSelected(int groupIndex, int channelIndex);
    }

    public LiveChannelListLayout(AppCompatActivity activity, ChannelManager channelManager) {
        this.mLayout = activity.findViewById(R.id.channelListLayout);
        this.channelManager = channelManager;
        this.mHandler = new Handler(activity.getMainLooper());

        ToggleButton showEpg = mLayout.findViewById(R.id.showEpgButton);
        showEpg.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mHandler.post(() -> {
                        epgSpacer.setVisibility(View.VISIBLE);
                        epgList.setVisibility(View.VISIBLE);
                    });
                } else {
                    mHandler.post(() -> {
                        epgSpacer.setVisibility(View.GONE);
                        epgList.setVisibility(View.GONE);
                    });
                }
            }
        });
        this.epgSpacer = mLayout.findViewById(R.id.channelSpacer3);
        this.epgList = mLayout.findViewById(R.id.channelEpgList);

        RecyclerView channelList = mLayout.findViewById(R.id.channelList);
        channelListViewAdapter = new RecyclerAdapter<>(
                mLayout.getContext(),
                channelManager.getChannels(selectedChannel).orElse(new ArrayList<>()),
                new ChannelListViewHolderFactory(mLayout.getContext(), R.layout.live_channel_list_item)
        );
        channelList.setAdapter(channelListViewAdapter);
        channelListViewAdapter.setOnSelectedListener((position, item) -> {
            Log.i(TAG, String.format(Locale.ENGLISH, "channel item: %d selected", position));
            selectedChannel = position;
            if (channelSelectedListener != null) {
                channelSelectedListener.onChannelSelected(selectedGroup, selectedChannel);
            }
        });
        channelList.addOnItemTouchListener(channelListViewAdapter.new OnItemTouchListener(mLayout.getContext(), channelList));

        groupList = mLayout.findViewById(R.id.channelGroup);
        groupListSpacer = mLayout.findViewById(R.id.channelSpacer1);
        if (channelManager.hasGroup()) {
            mHandler.post(() -> {
                groupList.setVisibility(View.VISIBLE);
                groupListSpacer.setVisibility(View.VISIBLE);
            });
        }
        groupListViewAdapter = new RecyclerAdapter<>(
                mLayout.getContext(),
                channelManager.groups,
                new GroupListViewHolderFactory(mLayout.getContext(), R.layout.live_channel_list_item)
        );
        groupList.setAdapter(groupListViewAdapter);
        groupListViewAdapter.setOnSelectedListener((position, item) -> {
            selectedGroup = position;
            List<ChannelItem> channels = this.channelManager.getChannels(position).orElse(new ArrayList<>());
            Log.i(TAG, String.format("select group: %d, channels: %d", position, channels.size()));
            channelListViewAdapter.setData(channels);
        });
        groupList.addOnItemTouchListener(groupListViewAdapter.new OnItemTouchListener(mLayout.getContext(), groupList));
    }

    public void setVisibleDelayed(boolean isVisible, int delayMillis) {
        if (setVisibilityDelayedTask != null) {
            setVisibilityDelayedTask.cancel();
        }
        setVisibilityDelayedTask = new CancelableRunnable() {
            @Override
            public void callback() {
                setVisible(isVisible);
            }
        };
        delayMillis = Math.max(delayMillis, 1);
        mHandler.postDelayed(setVisibilityDelayedTask, delayMillis);
    }

    private void setVisible(boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.GONE;
        if (visibility != mLayout.getVisibility()) {
            mLayout.setVisibility(visibility);
        }
    }

    public void setOnChannelSelectedListener(OnChannelSelectedListener listener) {
        channelSelectedListener = listener;
    }

    public Optional<List<MediaItem>> getCurrentChannelSources() {
        return channelManager.toMediaItems(selectedGroup, selectedChannel);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(ChannelManager m) {
        selectedGroup = 0;
        selectedChannel = 0;
        channelManager = m;
        groupListViewAdapter.setData(m.groups);
        int visibility = m.hasGroup() ? View.VISIBLE : View.GONE;
        mHandler.post(() -> {
            groupList.setVisibility(visibility);
            groupListSpacer.setVisibility(visibility);
        });
        List<ChannelItem> channels = m.getChannels(selectedGroup).orElse(new ArrayList<>());
        channelListViewAdapter.setData(channels);
    }
}
