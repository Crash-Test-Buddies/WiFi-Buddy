//package edu.rit.se.crashavoidance;
//
//import android.app.Fragment;
//import android.content.Context;
//import android.location.Location;
//import android.os.Bundle;
//import android.os.Handler;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ArrayAdapter;
//import android.widget.ListView;
//import android.widget.TextView;
//
//import org.w3c.dom.Text;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import io.nlopez.smartlocation.OnLocationUpdatedListener;
//
///**
// * This fragment handles chat related UI which includes a list view for messages
// * and a message entry field with send button.
// */
//public class WiFiChatFragment extends Fragment implements OnLocationUpdatedListener {
//
//    private View view;
//    private ChatManager chatManager;
////    private TextView chatLine;
//    private ListView listView;
//    private TextView myLocation;
//    ChatMessageAdapter adapter = null;
//    private List<String> items = new ArrayList<String>();
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        view = inflater.inflate(R.layout.fragment_chat, container, false);
////        chatLine = (TextView) view.findViewById(R.id.txtChatLine);
//        listView = (ListView) view.findViewById(android.R.id.list);
//        adapter = new ChatMessageAdapter(getActivity(), android.R.id.text1,
//                items);
//        listView.setAdapter(adapter);
//        myLocation = (TextView) view.findViewById(R.id.my_location);
//        return view;
//    }
//
//    @Override
//    public void onLocationUpdated(Location location) {
//        String locationStr = location.toString();
//        if (chatManager != null) {
//            chatManager.write(locationStr.getBytes());
//        }
//        myLocation.setText("My location: " + location);
//    }
//
//    public interface MessageTarget {
//        public Handler getHandler();
//    }
//
//    public void setChatManager(ChatManager obj) {
//        chatManager = obj;
//    }
//
//    public void pushMessage(String readMessage) {
//        adapter.add(readMessage);
//        adapter.notifyDataSetChanged();
//    }
//
//    /**
//     * ArrayAdapter to manage chat messages.
//     */
//    public class ChatMessageAdapter extends ArrayAdapter<String> {
//
//        List<String> messages = null;
//
//        public ChatMessageAdapter(Context context, int textViewResourceId,
//                                  List<String> items) {
//            super(context, textViewResourceId, items);
//        }
//
//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//            View v = convertView;
//            if (v == null) {
//                LayoutInflater vi = (LayoutInflater) getActivity()
//                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//                v = vi.inflate(android.R.layout.simple_list_item_1, null);
//            }
//            String message = items.get(position);
//            if (message != null && !message.isEmpty()) {
//                TextView nameText = (TextView) v
//                        .findViewById(android.R.id.text1);
//                nameText.setText(message);
//            }
//            return v;
//        }
//    }
//}
