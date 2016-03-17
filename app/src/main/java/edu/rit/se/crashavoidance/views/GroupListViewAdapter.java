package edu.rit.se.crashavoidance.views;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import edu.rit.se.crashavoidance.R;

/**
 * Created by Chris on 3/16/2016.
 */
public class GroupListViewAdapter extends BaseAdapter{
    private List<String> peers;
    private Activity context;

    public GroupListViewAdapter(Activity context, List<String> peers){
        this.context = context;
        this.peers = peers;
    }
    @Override
    public int getCount() {
        return peers.size();
    }

    @Override
    public Object getItem(int position) {
        return peers.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO We can change this if we decide to have an object here
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Convert this to the correct object if we change this
        String peerName = (String)getItem(position);
        // This will inflate the template view inside each ListView item
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.group_item, parent, false);
        }
        TextView item = (TextView) convertView.findViewById(R.id.peerNameView);
        item.setText(peerName);
        // TODO Need to figure out how to pass the message to this
        Button sendButton = (Button) convertView.findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Send message to this user
            }
        });
        return convertView;
    }
}
