package edu.rit.se.crashavoidance.views;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.wifi.WifiDirectHandler;

/**
 * This fragment handles chat related UI which includes a list view for messages
 * and a message entry field with a send button.
 */
public class ChatFragment extends ListFragment {
    private EditText textMessageEditText;
    private ChatMessageAdapter adapter = null;
    private List<String> items = new ArrayList<>();
    private WiFiDirectHandlerAccessor handlerAccessor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        textMessageEditText = (EditText) view.findViewById(R.id.textMessageEditText);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        adapter = new ChatMessageAdapter(getActivity(), android.R.id.text1, items);
        listView.setAdapter(adapter);

        view.findViewById(R.id.sendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.i(WifiDirectHandler.LOG_TAG, "Send button tapped");
                if (handlerAccessor.getWifiHandler().getChatManager() != null) {
                    handlerAccessor.getWifiHandler().getChatManager().write(textMessageEditText.getText().toString().getBytes());
                } else {
                    Log.e(WifiDirectHandler.LOG_TAG, "Chat manager is null");
                }
                String message = textMessageEditText.getText().toString();
                Log.i(WifiDirectHandler.LOG_TAG, "Message: " + message);
                pushMessage("Me: " + message);
                textMessageEditText.setText("");
            }
        });
        return view;
    }

    public interface MessageTarget {
        Handler getHandler();
    }

    public void pushMessage(byte[] readMessage) {
        String message = new String(readMessage);
        pushMessage(message);
    }

    public void pushMessage(String message) {
        adapter.add(message);
        adapter.notifyDataSetChanged();
    }

    /**
     * ArrayAdapter to manage chat messages.
     */
    public class ChatMessageAdapter extends ArrayAdapter<String> {
        List<String> messages = null;

        public ChatMessageAdapter(Context context, int textViewResourceId, List<String> items) {
            super(context, textViewResourceId, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(android.R.layout.simple_list_item_1, null);
            }
            String message = items.get(position);
            if (message != null && !message.isEmpty()) {
                TextView nameText = (TextView) v.findViewById(android.R.id.text1);
                if (nameText != null) {
                    nameText.setText(message);
                    if (message.startsWith("Me: ")) {
                        nameText.setTypeface(null, Typeface.NORMAL);
                    } else {
                        nameText.setTypeface(null, Typeface.BOLD);
                    }
                }
            }
            return v;
        }
    }

    /**
     * This is called when the Fragment is opened and is attached to MainActivity
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            handlerAccessor = ((WiFiDirectHandlerAccessor) getActivity());
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement WiFiDirectHandlerAccessor");
        }
    }
}
