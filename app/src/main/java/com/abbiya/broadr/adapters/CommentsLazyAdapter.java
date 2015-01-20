package com.abbiya.broadr.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.R;
import com.abbiya.broadr.dao.Comment;
import com.abbiya.broadr.utility.Constants;
import com.ocpsoft.pretty.time.PrettyTime;

import java.util.Date;

/**
 * Created by seshachalam on 8/11/14.
 */
public class CommentsLazyAdapter extends LazyListAdapter<Comment> {

    private final LayoutInflater layoutInflater;

    public CommentsLazyAdapter(LayoutInflater layoutInflater) {
        this.layoutInflater = layoutInflater;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if (view == null) {
            view = layoutInflater.inflate(R.layout.comment, viewGroup, false);
            holder = new ViewHolder(view);
        } else {
            holder = ViewHolder.getFromView(view);
        }

        Comment comment = getItem(i);
        holder.render(comment);

        return view;
    }

    private static class ViewHolder {
        TextView contentView;
        TextView commentHappenedAtView;
        TextView commentStatus;

        public ViewHolder(View view) {
            contentView = (TextView) view.findViewById(R.id.comment_content);
            commentHappenedAtView = (TextView) view.findViewById(R.id.comment_happenedAt);
            commentStatus = (TextView) view.findViewById(R.id.textView_commentStatus);

            view.setTag(this);
        }

        public static ViewHolder getFromView(View view) {
            Object tag = view.getTag();
            if (tag instanceof ViewHolder) {
                return (ViewHolder) tag;
            } else {
                return new ViewHolder(view);
            }
        }

        public void render(Comment comment) {
            contentView.setText(comment.getContent());

            PrettyTime t = new PrettyTime(comment.getHappenedAt());

            commentHappenedAtView.setText(t.format(new Date()).replace("from now", BroadrApp.getInstance().getString(R.string.time_ago)));
            String commentStatus;
            Integer status = comment.getStatus();
            if (status == Constants.SENDING) {
                commentStatus = "Sending";
            } else if (status == Constants.SENT) {
                commentStatus = "Sent";
            } else if (status == Constants.RECEIVED) {
                commentStatus = "Received";
            } else if (status == Constants.DELIVERED) {
                commentStatus = "Delivered";
            } else {
                commentStatus = "Buggy";
            }
            String address = comment.getAddress();
            String geoHash = comment.getGeoHash();
            if (address != null && address.trim().length() > 0) {
                String[] parts = address.split(",");
                if (parts.length >= 3) {
                    this.commentStatus.setText(parts[0] + ", " + parts[1] + ", " + parts[2]);
                }
            } else {
                if (geoHash != null && geoHash.trim().length() > 0) {
                    //String link = BroadrApp.getInstance().getString(R.string.location_geohash_org) + geoHash;
                    this.commentStatus.setText("");
                } else {
                    this.commentStatus.setText(commentStatus);
                }
            }
        }
    }

}
