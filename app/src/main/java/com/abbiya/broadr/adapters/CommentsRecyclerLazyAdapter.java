package com.abbiya.broadr.adapters;

import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.R;
import com.abbiya.broadr.dao.Comment;
import com.abbiya.broadr.utility.Constants;
import com.abbiya.broadr.utility.LocationUtils;
import com.abbiya.broadr.viewholders.CommentViewHolder;
import com.ocpsoft.pretty.time.PrettyTime;

import java.util.Date;

/**
 * Created by seshachalam on 15/11/14.
 */
public class CommentsRecyclerLazyAdapter extends RecyclerLazyListAdapter<Comment> {

    Comment comment;

    String geoHashViewFontPath = "RobotoTTF/Roboto-Medium.ttf";
    Typeface gtf = Typeface.createFromAsset(BroadrApp.getInstance().getAssets(), geoHashViewFontPath);
    String contentFontPath = "RobotoTTF/Roboto-Regular.ttf";
    Typeface ctf = Typeface.createFromAsset(BroadrApp.getInstance().getAssets(), contentFontPath);
    String messageStatusFontPath = "RobotoTTF/Roboto-Light.ttf";
    Typeface mtf = Typeface.createFromAsset(BroadrApp.getInstance().getAssets(), messageStatusFontPath);

    public CommentsRecyclerLazyAdapter() {

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        comment = getItem(i);

        render((CommentViewHolder) viewHolder, comment);
    }

    @Override
    public CommentViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View commentView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.comment, viewGroup, false);

        return new CommentViewHolder(commentView);
    }

    private void render(CommentViewHolder commentViewHolder, Comment comment) {
        commentViewHolder.contentView.setText(comment.getContent());

        PrettyTime t = new PrettyTime(comment.getHappenedAt());

        commentViewHolder.commentHappenedAtView.setText(t.format(new Date()).replace("from now", BroadrApp.getInstance().getString(R.string.time_ago)));
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
                commentViewHolder.commentStatus.setText(parts[0] + ", " + parts[1] + ", " + parts[2]);
            }
        } else {
            if (geoHash != null && geoHash.trim().length() > 0) {
                String link = Constants.GOOGLE_MAPS_URL + LocationUtils.getLocationAsString(geoHash) + "z";
                commentViewHolder.commentStatus.setText(link);
            } else {
                commentViewHolder.commentStatus.setText(commentStatus);
            }
        }

        //set fonts
        commentViewHolder.commentHappenedAtView.setTypeface(gtf);
        commentViewHolder.contentView.setTypeface(ctf);
        commentViewHolder.commentStatus.setTypeface(mtf);
    }

}
