package com.abbiya.broadr.adapters;

import android.support.v7.widget.RecyclerView;

import de.greenrobot.dao.query.LazyList;

/**
 * Created by seshachalam on 15/11/14.
 */
abstract public class RecyclerLazyListAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    LazyList<T> lazyList = null;

    public RecyclerLazyListAdapter() {
    }

    public RecyclerLazyListAdapter(LazyList<T> initialList) {
        lazyList = initialList;
    }

    public void replaceLazyList(LazyList<T> newList) {
        if (lazyList != null) {
            lazyList.close();
        }
        lazyList = newList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return lazyList == null ? 0 : lazyList.size();
    }

    public T getItem(int i) {
        return lazyList == null ? null : lazyList.get(i);
    }

    public void close() {
        if (lazyList != null) {
            lazyList.close();
            lazyList = null;
        }
    }
}
