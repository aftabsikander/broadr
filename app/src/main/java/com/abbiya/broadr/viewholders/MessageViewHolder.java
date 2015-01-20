package com.abbiya.broadr.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.abbiya.broadr.R;

/**
 * Created by seshachalam on 15/11/14.
 */
public class MessageViewHolder extends RecyclerView.ViewHolder {

    public TextView contentView;
    public TextView geoHashView;
    public TextView messageStatus;
    public View row;

    public MessageViewHolder(View view) {
        super(view);

        contentView = (TextView) view.findViewById(R.id.message_content);
        geoHashView = (TextView) view.findViewById(R.id.message_geoHash);
        messageStatus = (TextView) view.findViewById(R.id.textView_messageStatus);
        row = view;
    }

}
