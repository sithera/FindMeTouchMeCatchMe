package slowhackaton.com.findmetouchmecatchme;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.facebook.AccessToken;
import com.facebook.login.LoginManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;

public class BeaconFinder extends ActionBarActivity implements Caller{

    private  final String SERVER_ADDRESS = "https://slow.telemabk.pl";
    private final Double TIME_IN_HOURS = 0.01;
    private final long HOUR  = 60*60*1000L;
    private final long CLEAR_FRIENDS_TIME = 3 * 1000L;

    private Region ourRegion;
    private BeaconManager beaconManager;

    private String address;
    private JSONObject data;

    private volatile boolean threadsShouldBeRunning = true;

    private final String tag_data = "data";
    private final String tag_user = "user_name";
    private final String tag_time = "time";
    private final String tag_mac = "mac";
    private final String tag_token = "token";
    private final String tag_event = "event";
    private final String tag_name = "name";
    private final String tag_description = "description";
    private final String tag_id = "user_id";

    private volatile Map<String,List<RowBean>> friends;

    private Button refreshButton;

    private volatile Map<String,Timestamp> currentBeacons;
    public ListView listView1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.support.v7.app.ActionBar barHide = getSupportActionBar();
        if (barHide != null) {
            barHide.hide();
        }

        Log.d("beacon finder", "started");

        setContentView(R.layout.activity_ranging_and_displaying);

        friends = new HashMap<String, List<RowBean>>();

        currentBeacons = new HashMap<String,Timestamp>();
        beaconManager = new BeaconManager(this);
        ourRegion =  new Region("region", null, null, null);



        listView1=(ListView)findViewById(R.id.Lista);
        refreshButton = (Button)findViewById(R.id.button);

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userList();
            }
        });
    }

    @Override
    protected void onStart(){
        super.onStart();
        startRangingBeacons();
        startSendingKnownBeaconsToServer();
        cleanOldBeaconsAfter(TIME_IN_HOURS);
        clearOldFriends();
    }

    private void startSendingKnownBeaconsToServer() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while(threadsShouldBeRunning) {
                    addUser();
                    try {
                        sleep(1*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();
    }

    @Override
    protected void onStop(){
        super.onStop();
        try {
            beaconManager.stopRanging(ourRegion);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        beaconManager.disconnect();

        threadsShouldBeRunning = false;
        LoginManager.getInstance().logOut();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        threadsShouldBeRunning = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ranging_and_displaying, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private String makeKey(Beacon beacon){
        final String key = Integer.toString(beacon.getMajor()) + Integer.toString(beacon.getMinor());
        return key;
    }

    private void startRangingBeacons(){
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> beacons) {
                for(Beacon beacon : beacons) {
                    String key = makeKey(beacon);
                    Date date= new Date();
                    currentBeacons.put(key,new Timestamp(date.getTime()));
                }
            }
        });

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startRanging(ourRegion);
                } catch (RemoteException rException) {
                    rException.printStackTrace();
                }
            }
        });
    }

    private void cleanOldBeaconsAfter(final Double timeInHours){
        Thread t = new Thread(){
            @Override
            public void run() {
                while(threadsShouldBeRunning){
                    try {
                        sleep(1*1000L);
                        Log.d("cleanOldBeacons","should run");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Date date = new Date();
                    Timestamp twoHoursAgo = new Timestamp(date.getTime() - (long)(1*1000));
                    Map<String,Timestamp> currentBeaconsCopy = new HashMap<>(currentBeacons);
                    for(Map.Entry<String,Timestamp> entry: currentBeaconsCopy.entrySet()){
                        String key = entry.getKey();
                        Timestamp timestamp = entry.getValue();
                        if(timestamp.before(twoHoursAgo)){
                            currentBeacons.remove(key);
                            Log.d("Removing beacon: ",key);
                        }
                    }
                }
            }
        };

        t.start();
    }

    public void addUser(){
        address = SERVER_ADDRESS + "/api/user/add";

        Map<String, String> data = new HashMap<String, String>();
        data.put(tag_mac, currentBeacons.toString());
        data.put(tag_token, AccessToken.getCurrentAccessToken().getToken());
        Log.d("send beacon", data.toString());
        JSONObject json = new JSONObject(data);

        send(address, json);
    }

    private void userList(){
        address = SERVER_ADDRESS + "/api/user/list";

        Map<String, String> data = new HashMap<String, String>();
        data.put(tag_mac, currentBeacons.toString());
        data.put(tag_token, AccessToken.getCurrentAccessToken().getToken());

        JSONObject json = new JSONObject(data);

        send(address, json);
    }

    private void addBeacon(String beaconID, String event, String description){
        address = SERVER_ADDRESS + "/api/beacon/add";

        Map<String, String> data = new HashMap<String, String>();
        data.put(tag_mac, beaconID);
        data.put(tag_description, description);
        data.put(tag_event, event);
        data.put(tag_token, AccessToken.getCurrentAccessToken().getToken());

        JSONObject json = new JSONObject(data);

        send(address, json);
    }

    private void send(String address,JSONObject data){
        this.address = address;
        this.data = data;
        SendPostRequestTask sendTask = new SendPostRequestTask(this);
        sendTask.execute();
    }

    @Override
    public String getAddress(){
        return this.address;
    }

    @Override
    public JSONObject getData(){
        return this.data;
    }

    @Override
    public void handleResponse(JSONObject response){
        if(response == null){
            Log.d("error", "response jest nullem");
            return;
        }
        if(response.has(tag_data)){
            try {
                JSONArray data = (JSONArray)response.get("data");
                handleListResponse(data);
            } catch(JSONException e){
                e.printStackTrace();
            }
        } else {
            Log.d("response handler", "There is no data");
        }
    };

    public void handleListResponse(JSONArray data){
        for (int i = 0; i < data.length(); i++) {
            try {
                JSONObject friend = data.getJSONObject(i);
                Log.d("Friend found", friend.toString());
                updateFriendList(friend);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Log.d("friends", friends.toString());
        updateView();
    }

    private void updateFriendList(JSONObject rawFriend) throws JSONException {
        final String mac = (String) rawFriend.get(tag_mac);
        final String time = (String) rawFriend.get(tag_time);
        final String user = (String) rawFriend.get(tag_user);
        final String id = (String) rawFriend.get(tag_id);

        if (friends.get(mac) == null) {
            Log.d("Creating new known ", "beacon for: " + mac);
            friends.put(mac, new ArrayList<RowBean>());
        }

        List<RowBean> beacon = friends.get(mac);
        RowBean friend = new RowBean(user,id, Long.parseLong(time));
        friend.requestForGlobalId();
        beacon.add(friend);
    }

    private void updateView(){

        List<RowBean> allFriendsInRange = new ArrayList<RowBean>();

        for(String mac: friends.keySet()){
            if(!currentBeacons.containsKey(mac)) continue;
            List<RowBean> friendsWithinBeacon = friends.get(mac);
            for(RowBean friend: friendsWithinBeacon) {
                if(allFriendsInRange.contains(friend)) continue;
                allFriendsInRange.add(friend);
            }
        }
        RowBean[] data = new RowBean[allFriendsInRange.size()];
        data = allFriendsInRange.toArray(data);

        Log.d("array with friends", Integer.toString(data.length));

        RowAdapter adapter = new RowAdapter(this, R.layout.format_friend_line, data);
        Log.d("adapters count: ", Integer.toString(adapter.getCount()));
        Log.d("listview", listView1.toString());
        listView1.setAdapter(adapter);
    }

    public void startConversationWith(Long userId){
        Log.d("Start conversation with",Long.toString(userId));
        Uri uri = Uri.parse("fb-messenger://user/");
        uri = ContentUris.withAppendedId(uri,userId);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    private void clearOldFriends(){
        Thread t = new Thread(){
            @Override
            public void run() {
                while(threadsShouldBeRunning){
                    userList();
                    for(String mac: friends.keySet()){
                        for(RowBean friend: friends.get(mac)){
                            if(friend.shouldBeRemovedIfOlderThan(CLEAR_FRIENDS_TIME)){
                                friends.remove(friend);
                            }
                        }
                    }
                    try {
                        sleep(1*1000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        };

        t.start();
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

}
