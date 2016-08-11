# Wi-Fi Buddy
A Library to handle Android Wi-Fi Direct

The goal of this project is to document and understand Wi-Fi Peer-to-Peer / Wi-Fi Direct on Android through an application that demonstrates its use, and also to build a library that simplifies the use of Wi-Fi Direct on Android. If this project is successful, it will remove some of the hurdles preventing developers from using Wi-Fi Direct in Android applications.

Android's implementation of Wi-Fi Direct is typically dreaded by developers and used only as a last resort if the app actually needs it. Its documentation is notoriously confusing, and often requested features such as no-prompt connections [are ignored](https://code.google.com/p/android/issues/detail?id=30880). If we can make it easier to develop Wi-Fi Direct apps on Android, then maybe the momentum will lead to better documentation and more development of Wi-Fi Direct within Android itself.

### Goals

Library

- Good documentation that explains _why_
- Consistent service discovery (i.e. why can't these adjacent devices find each other?)
- Fast connections
- Sane error handling (turning error codes into human readable strings)
- Logging of everything
- POJOs to replace complicated returns (looking at you dnsSdTxtRecord and dnsSdService)
- No prompt connections between devices
- Graceful disconnects when connection is lost
- Merge adjacent groups (if possible)
- Swap group owner (if possible)
- Large groups

Tester App

- Control over most variables in connection process (Group owner intent, name of the service, records broadcast in service, etc.)
- Fluent interface that is self explanatory
- Clear visual indication of what is happening with Wi-Fi Direct (displaying errors in a friendly way)

### Non-goals
- Rewrite Android's Wi-Fi Direct implementation (although we don't mind inspiring someone else to do so)
- Perfect compatibility on all devices

## Basic Library Usage

Build the project with gradle and you are good to go.

1. Create an Android Studio Project
2. It is easiest to use JitPack to add the library to your project. Add JitPack as a dependency in the `build.gradle` project file

```
buildscript {
   repositories {
       jcenter()
       maven { url "https://jitpack.io" }
   }
   dependencies {
       classpath 'com.android.tools.build:gradle:2.1.2'
   }
}

allprojects {
   repositories {
       jcenter()
       maven { url "https://jitpack.io" }
   }
}
```

3. Add WiFi-Buddy as a dependency in the build.gradle module file

`compile 'com.github.Crash-Test-Buddies:WiFi-Buddy:v0.8.0'`

4. Set the min SDK to 16 in the `build.gradle` file, this is becasue WiFi-Direct requires Android 4.1 or greater.

```
android {
   compileSdkVersion 23
   buildToolsVersion "23.0.2"
   useLibrary 'org.apache.http.legacy'

   defaultConfig {
       applicationId "rit.se.crashavoidance.datacollectiontests"
       minSdkVersion 16
       targetSdkVersion 23
       versionCode 1
       versionName "1.0"
   }
   buildTypes {
       release {
           minifyEnabled false
           proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
       }
   }
}
```

5. Create a ServiceConnection (typically in your Main Activity)

```
// Note: This is used to run WifiDirectHandler as a Service instead of being coupled to an
//          Activity. This is NOT a connection to a P2P service being broadcast from a device
private ServiceConnection wifiServiceConnection = new ServiceConnection() {

   /**
    * Called when a connection to the Service has been established, with the IBinder of the
    * communication channel to the Service.
    * @param name The component name of the service that has been connected
    * @param service The IBinder of the Service's communication channel
    */
   @Override
   public void onServiceConnected(ComponentName name, IBinder service) {
       Log.i(TAG, "Binding WifiDirectHandler service");
       Log.i(TAG, "ComponentName: " + name);
       Log.i(TAG, "Service: " + service);
       WifiDirectHandler.WifiTesterBinder binder = (WifiDirectHandler.WifiTesterBinder) service;

       wifiDirectHandler = binder.getService();
       wifiDirectHandlerBound = true;
       Log.i(TAG, "WifiDirectHandler service bound");

       // Add MainFragment to the 'fragment_container' when wifiDirectHandler is bound
       mainFragment = new MainFragment();
       replaceFragment(mainFragment);

       deviceInfoTextView.setText(wifiDirectHandler.getThisDeviceInfo());
   }

   /**
    * Called when a connection to the Service has been lost.  This typically
    * happens when the process hosting the service has crashed or been killed.
    * This does not remove the ServiceConnection itself -- this
    * binding to the service will remain active, and you will receive a call
    * to onServiceConnected when the Service is next running.
    */
   @Override
   public void onServiceDisconnected(ComponentName name) {
       wifiDirectHandlerBound = false;
       Log.i(TAG, "WifiDirectHandler service unbound");
   }
};
```

6. Create and locally register a BroadcastReceiver to listen for the intents you want from the library.

```
IntentFilter filter = new IntentFilter();
filter.addAction(WifiDirectHandler.Action.SERVICE_CONNECTED);
filter.addAction(WifiDirectHandler.Action.MESSAGE_RECEIVED);
filter.addAction(WifiDirectHandler.Action.DEVICE_CHANGED);
filter.addAction(WifiDirectHandler.Action.WIFI_STATE_CHANGED);
LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {

   private static final String TAG = WifiDirectHandler.TAG + "CommReceiver";

   @Override
   public void onReceive(Context context, Intent intent) {
      . . .
   }
, filter);
```

7. Bind to the service (typically in `onCreate`)

```
Intent intent = new Intent(this, WifiDirectHandler.class);
bindService(intent, wifiServiceConnection, BIND_AUTO_CREATE);
```

When this has successfully completed the `onServiceConnected` method from step 5 will be called. At this point you are ready to use the library. Outlined below are some of the features you may wish to use with the `WifiDirectHandler` instance you now have.

### Registering a P2P Service

`addLocalService(String serviceName, HashMap<String,String> serviceRecord)`

This function brodcasts a service out for other WiFi-Direct enabled devices to discover. You will want to name your service and provide any additional records you want to associate with it. Records are visible to another device when it discovers your service. You do not need to provide any records.

### Discovering a P2P Service

`continuouslyDiscoverServices()`

This method adds a service discovery request and begins discovering services. As it discovers the services, they are stored for later access. Using Android's API, service discovery times out after 2 minutes without giving any warning whatsoever to the application. Fortunately this library uses a timer to resume service discovery every two minutes in order to prevent frustration.

As services are discovered, two different intents may be locally broadcast by the library for an application to listen for. 

- `WifiDirectHandler.Action.DNS_SD_TXT_RECORD_AVAILABLE` - A Bonjour text record is available. If you have been following along this would indicate that a service record from the previous _Registering a P2P Service_ section has been found. This intent contains a String extra holding the device address of the discovered device. This extra can be accessed using `WifiDirectHandler.TXT_MAP_KEY`. The actual results can be found by calling `getDnsSdTxtRecordMap()` and keying the resulting map using the device address. 
- `WifiDirectHandlerAction.DNS_SD_SERVICE_AVAILABLE` - A Bonjour service discovery response has been received. This intent contains a String extra holding the device address of the discovered device. This extra can be accessed using `WifiDirectHandler.SERVICE_MAP_KEY`. The actual results can be found by calling `getDnsSdServiceMap()` and keying the resulting map using the device address.

### Connecting to a Discovered P2P Service

Using the service you found within `getDnsSdServiceMap` in the previous step, call `initiateConnectToService(DnsSdService service)` when the `WifiDirectHandler.Action.SERVICE_CONNECTED` intent is broadcast that indicates that the connection is complete and you are ready to send messages back and forth between devices.

### Sending Messages

Once connected, call `getCommunicationManager()` and use the `write(byte[] message)` method in the resulting object to send messages. You may want to use something such as serializable for complex messages.

### Receiving Messages

The `WifiDirectHandler.Action.MESSAGE_RECEIVED` intent indicates that a message has been received. The message is accessible as a byte[] extra within the intent. The message can be accessed with the key `WifiDirectHandler.MESSAGE_KEY`