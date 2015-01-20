package com.abbiya.broadr.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.abbiya.broadr.R;
import com.abbiya.broadr.dao.Message;
import com.abbiya.broadr.utility.Constants;
import com.abbiya.broadr.utility.LocationUtils;

/**
 * Created by seshachalam on 8/11/14.
 */
public class MessagesLazyAdapter extends LazyListAdapter<Message> {

    private final LayoutInflater layoutInflater;

    public MessagesLazyAdapter(LayoutInflater layoutInflater) {
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
            view = layoutInflater.inflate(R.layout.message, viewGroup, false);
            holder = new ViewHolder(view);
        } else {
            holder = ViewHolder.getFromView(view);
        }

        Message message = getItem(i);
        holder.render(message);

        return view;
    }

    private static class ViewHolder {
        TextView contentView;
        TextView geoHashView;
        TextView messageStatus;
        View row;

        public ViewHolder(View view) {
            contentView = (TextView) view.findViewById(R.id.message_content);
            geoHashView = (TextView) view.findViewById(R.id.message_geoHash);
            messageStatus = (TextView) view.findViewById(R.id.textView_messageStatus);
            row = view;
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

        public void render(Message message) {
            contentView.setText(message.getContent());

            String from = null;
            String address = message.getAddress();
            if (address != null && address.trim().length() > 0) {
                String[] parts = address.split(",");
                if (parts.length >= 3) {
                    geoHashView.setText(parts[0] + ", " + parts[1] + ", " + parts[2]);
                }
            } else {
                String geoHash = message.getGeoHash();

                if (geoHash != null && geoHash.trim().length() > 0) {
                    String link = "http://geohash.org/" + geoHash;
                    geoHashView.setText(link);
                } else {
                    geoHashView.setText(LocationUtils.getPrettyDistance(from, message.getGeoHash(), true));
                }
            }
            String messageStatus = "";
            Integer status = message.getStatus();
            if (status == Constants.SENDING) {
                //row.setBackgroundColor(Color.LTGRAY);
                messageStatus = "Sending";
            } else if (status == Constants.SENT) {
                //row.setBackgroundColor(Color.parseColor("#f97272"));
                messageStatus = "Sent";
            } else if (status == Constants.RECEIVED) {
                //row.setBackgroundColor(Color.parseColor("#93effc"));
                messageStatus = "Received";
            } else if (status == Constants.DELIVERED) {
                //row.setBackgroundColor(Color.parseColor("#ebfe7f"));
                messageStatus = "Delivered";
            } else {
                messageStatus = "Buggy";
            }
            this.messageStatus.setText(messageStatus);
        }
    }

}
