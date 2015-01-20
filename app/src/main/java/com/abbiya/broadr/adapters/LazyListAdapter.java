package com.abbiya.broadr.adapters;

import android.widget.BaseAdapter;

import de.greenrobot.dao.query.LazyList;

/**
 * Created by seshachalam on 29/8/14.
 */
abstract public class LazyListAdapter<T> extends BaseAdapter {
    LazyList<T> lazyList = null;

    public LazyListAdapter() {
    }

    public LazyListAdapter(LazyList<T> initialList) {
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
    public int getCount() {
        return lazyList == null ? 0 : lazyList.size();
    }

    @Override
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
