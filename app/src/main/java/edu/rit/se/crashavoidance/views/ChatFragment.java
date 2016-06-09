package edu.rit.se.crashavoidance.views;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import edu.rit.se.crashavoidance.R;
import edu.rit.se.crashavoidance.wifi.CommunicationManager;
import edu.rit.se.crashavoidance.wifi.WifiDirectHandler;

/**
 * This fragment handles chat related UI which includes a list view for messages
 * and a message entry field with a send button.
 */
public class ChatFragment extends ListFragment {

    private EditText textMessageEditText;
    private ChatMessageAdapter adapter = null;
    private List<String> items = new ArrayList<>();
    private ArrayList<String> messages = new ArrayList<>();
    private WiFiDirectHandlerAccessor handlerAccessor;
    private Toolbar toolbar;
    private Button sendButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        sendButton = (Button) view.findViewById(R.id.sendButton);
        sendButton.setEnabled(false);

        textMessageEditText = (EditText) view.findViewById(R.id.textMessageEditText);
        textMessageEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendButton.setEnabled(true);
            }
        });

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        adapter = new ChatMessageAdapter(getActivity(), android.R.id.text1, items);
        listView.setAdapter(adapter);
        listView.setDividerHeight(0);

        // Prevents the keyboard from pushing the fragment and messages up and off the screen
        listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        listView.setStackFromBottom(true);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.i(WifiDirectHandler.LOG_TAG, "Send button tapped");
                CommunicationManager communicationManager = handlerAccessor.getWifiHandler().getCommunicationManager();
                if (communicationManager != null && !textMessageEditText.toString().equals("")) {
                    String message = textMessageEditText.getText().toString();
                    // Gets first word of device name
                    String author = handlerAccessor.getWifiHandler().getThisDevice().deviceName.split(" ")[0];
                    byte[] messageBytes = (author + ": " + message).getBytes();
                    communicationManager.write(messageBytes);
                } else {
                    Log.e(WifiDirectHandler.LOG_TAG, "Communication Manager is null");
                }
                String message = textMessageEditText.getText().toString();
                if (!message.equals("")) {
                    pushMessage("Me: " + message);
                    messages.add(message);
                    Log.i(WifiDirectHandler.LOG_TAG, "Message: " + message);
                    textMessageEditText.setText("");
                }
                sendButton.setEnabled(false);
            }
        });

        toolbar = (Toolbar) getActivity().findViewById(R.id.mainToolbar);

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
                        // My message
                        nameText.setGravity(Gravity.RIGHT);
                    } else {
                        // Buddy's message
                        nameText.setGravity(Gravity.LEFT);
                    }
                }
            }
            return v;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        toolbar.setTitle("Chat");
    }

    @Override
    public void onPause() {
        super.onPause();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(textMessageEditText.getWindowToken(), 0);
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
