package com.abbiya.broadr.adapters;

import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.R;
import com.abbiya.broadr.dao.Message;
import com.abbiya.broadr.utility.Constants;
import com.abbiya.broadr.utility.LocationUtils;
import com.abbiya.broadr.viewholders.MessageViewHolder;
import com.ocpsoft.pretty.time.PrettyTime;

import java.util.Date;

/**
 * Created by seshachalam on 15/11/14.
 */
public class MessagesRecyclerLazyAdapter extends RecyclerLazyListAdapter<Message> {

    boolean kmOrMi;
    Message message;

    String messageStatusFontPath = "RobotoTTF/Roboto-Medium.ttf";
    Typeface mtf = Typeface.createFromAsset(BroadrApp.getInstance().getAssets(), messageStatusFontPath);
    String contentFontPath = "RobotoTTF/Roboto-Regular.ttf";
    Typeface ctf = Typeface.createFromAsset(BroadrApp.getInstance().getAssets(), contentFontPath);
    String geoHashViewFontPath = "RobotoTTF/Roboto-Light.ttf";
    Typeface gtf = Typeface.createFromAsset(BroadrApp.getInstance().getAssets(), geoHashViewFontPath);

    public MessagesRecyclerLazyAdapter(boolean kmOrMi) {
        this.kmOrMi = kmOrMi;
    }

    public void setKmOrMi(boolean kmOrMi) {
        this.kmOrMi = kmOrMi;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        message = getItem(i);
        MessageViewHolder messageViewHolder = (MessageViewHolder) viewHolder;

        render(messageViewHolder, message);
    }

    @Override
    public MessageViewHolder onCreateViewHolder(final ViewGroup viewGroup, int i) {
        final View messageView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.message, viewGroup, false);

        MessageViewHolder messageViewHolder = new MessageViewHolder(messageView);

        return messageViewHolder;
    }

    private void render(MessageViewHolder messageViewHolder, Message message) {

        messageViewHolder.contentView.setText(message.getContent());

        String from = null;
        String address = message.getAddress();
        if (address != null && address.trim().length() > 0) {
            String[] parts = address.split(",");
            if (parts.length >= 3) {
                messageViewHolder.geoHashView.setText(parts[0] + ", " + parts[1] + ", " + parts[2]);
            }
        } else {
            String geoHash = message.getGeoHash();

            if (geoHash != null && geoHash.trim().length() > 0) {
                String link = Constants.GOOGLE_MAPS_URL + LocationUtils.getLocationAsString(geoHash) + "z";
                messageViewHolder.geoHashView.setText(link);
            } else {
                messageViewHolder.geoHashView.setText(LocationUtils.getPrettyDistance(from, message.getGeoHash(), kmOrMi));
            }
        }
        String messageStatus = "";
        Integer status = message.getStatus();
        if (status == Constants.SENDING) {
            messageStatus = "Sending";
        } else if (status == Constants.SENT) {
            messageStatus = "Sent";
        } else if (status == Constants.RECEIVED) {
            messageStatus = "Received";
        } else if (status == Constants.DELIVERED) {
            messageStatus = "Delivered";
        } else {
            messageStatus = "Buggy";
        }

        PrettyTime t = new PrettyTime(message.getHappenedAt());
        messageViewHolder.messageStatus.setText(t.format(new Date()).replace("from now", BroadrApp.getInstance().getString(R.string.time_ago)));

        //set fonts
        messageViewHolder.messageStatus.setTypeface(mtf);
        messageViewHolder.contentView.setTypeface(ctf);
        messageViewHolder.geoHashView.setTypeface(gtf);

        //messageViewHolder.setTag(message);
    }
}
