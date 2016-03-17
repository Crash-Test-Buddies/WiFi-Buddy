package edu.rit.se.crashavoidance.views;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import edu.rit.se.crashavoidance.R;

/**
 * Created by Chris on 3/16/2016.
 */
public class GroupMessageActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_message_view);
    }

    private void setPeerList(){
        List<String> peers = new ArrayList<String>();
        // TODO populate off of devices connected
        peers.add("peer 1");
        peers.add("peer 2");
        ListView listView = (ListView) findViewById(R.id.peerListView);
        listView.setAdapter(new GroupListViewAdapter(this, peers));
    }
}
