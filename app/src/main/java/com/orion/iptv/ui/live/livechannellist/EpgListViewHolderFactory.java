package com.orion.iptv.ui.live.livechannellist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.orion.iptv.bean.EpgProgram;
import com.orion.iptv.recycleradapter.ViewHolder;
import com.orion.iptv.recycleradapter.ViewHolderFactory;

public class EpgListViewHolderFactory implements ViewHolderFactory<ViewHolder<EpgProgram>, EpgProgram> {
    private final Context context;
    private final int layoutId;

    public EpgListViewHolderFactory(Context context, int layoutId) {
        this.context = context;
        this.layoutId = layoutId;
    }

    @Override
    public ViewHolder<EpgProgram> create(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new ListViewHolder<>(v);
    }
}
