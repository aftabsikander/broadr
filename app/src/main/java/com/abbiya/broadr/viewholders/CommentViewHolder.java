package com.abbiya.broadr.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.abbiya.broadr.R;

/**
 * Created by seshachalam on 15/11/14.
 */
public class CommentViewHolder extends RecyclerView.ViewHolder {

    public TextView contentView;
    public TextView commentHappenedAtView;
    public TextView commentStatus;
    public View row;

    public CommentViewHolder(View view) {
        super(view);

        contentView = (TextView) view.findViewById(R.id.comment_content);
        commentHappenedAtView = (TextView) view.findViewById(R.id.comment_happenedAt);
        commentStatus = (TextView) view.findViewById(R.id.textView_commentStatus);
        row = view;
    }

}
