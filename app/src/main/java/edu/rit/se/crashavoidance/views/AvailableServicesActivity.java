//package edu.rit.se.crashavoidance.views;
//
//import android.content.Intent;
//import android.net.wifi.p2p.WifiP2pDevice;
//import android.net.wifi.p2p.WifiP2pManager;
//import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
//import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
//import android.util.Log;
//import android.widget.ListView;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//import edu.rit.se.crashavoidance.R;
//import edu.rit.se.crashavoidance.WiFiP2pService;
//
//public class AvailableServicesActivity extends AppCompatActivity {
//
//    List<WiFiP2pService> services = new ArrayList<WiFiP2pService>();
//    AvailableServicesListViewAdapter serviceListAdapter;
//
//    public void onServiceClick(WiFiP2pService service) {
//        //TODO: call actual connection logic.
//        Intent intent = new Intent(this, GroupMessageActivity.class);
//        //TODO: pass data between the activities?
//        startActivity(intent);
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_available_services);
//
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        setServiceList();
//        //startDiscoveringServices();
//    }
//
//    //TODO: There will need to be a way to dynamically add/remove services
//    private void setServiceList(){
//        ListView listView = (ListView) findViewById(R.id.availableServicesList);
//        serviceListAdapter = new AvailableServicesListViewAdapter(this, services);
//        listView.setAdapter(serviceListAdapter);
//    }
//
//    // TODO move this to use the one in WifiTester
//    public void startDiscoveringServices() {
//        WifiP2pManager.DnsSdTxtRecordListener txtRecordListener = new WifiP2pManager.DnsSdTxtRecordListener() {
//            @Override
//            public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
////                dnsSdTxtRecordMap.put(srcDevice.deviceAddress, new DnsSdTxtRecord(fullDomainName, txtRecordMap, srcDevice));
//            }
//        };
//
//        WifiP2pManager.DnsSdServiceResponseListener serviceResponseListener = new WifiP2pManager.DnsSdServiceResponseListener() {
//            @Override
//            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
//                Log.d("AvailableServices", "Service available");
////                dnsSdServiceMap.put(srcDevice.deviceAddress, new DnsSdService(instanceName, registrationType, srcDevice));
//                // Add service to service list and let the adapter know the list has changed
//                // TODO not sure if we are still using this service object
//                WiFiP2pService service = new WiFiP2pService();
//                service.device = srcDevice;
//                service.instanceName = instanceName;
//                service.serviceRegistrationType = registrationType;
//                services.add(service);
//                for (WiFiP2pService loopService : services) {
//                    Log.d("AvailbleServices", "Service discovered: " + loopService.instanceName);
//                }
//                serviceListAdapter.notifyDataSetChanged();
//            }
//            //TODO: Maybe an observer pattern or something to indicate a change
//
//        };
//
//        wifiP2pManager.setDnsSdResponseListeners(wifiP2pChannel, serviceResponseListener, txtRecordListener);
//        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
//        wifiP2pManager.addServiceRequest(wifiP2pChannel, serviceRequest,
//                new WifiP2pManager.ActionListener() {
//
//                    @Override
//                    public void onSuccess() {
//                    }
//
//                    @Override
//                    public void onFailure(int errorCode) {
//                    }
//                });
//        wifiP2pManager.discoverServices(wifiP2pChannel, new WifiP2pManager.ActionListener() {
//
//            @Override
//            public void onSuccess() {
//            }
//
//            @Override
//            public void onFailure(int arg0) {
//
//            }
//        });
//
//    }
//}
