package slowhackaton.com.findmetouchmecatchme;

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

public class BeaconFinder extends ActionBarActivity {

    private  final String SERVER_ADDRESS = "https://slow.telemabk.pl";
    private final Double TIME_IN_OURS = 0.5;
    private final long HOUR  = 60*60*1000;

    private Region ourRegion;
    private BeaconManager beaconManager;

    private String address;
    private JSONObject data;

    private volatile boolean threadsShouldBeRunning = true;

    private final String tag_data = "data";
    private final String tag_user = "user";
    private final String tag_time = "time";
    private final String tag_mac = "mac";
    private final String tag_token = "token";
    private final String tag_event = "event";
    private final String tag_name = "name";
    private final String tag_description = "description";

    private Map<String,Map<String,String>> friends;

    private Button sendButton;

    private Map<String,Timestamp> currentBeacons;
    public ListView listView1;
    private final ArrayList<String> dataList = new ArrayList<String>(30);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranging_and_displaying);

        friends = new HashMap<String,Map<String,String>>();

        currentBeacons = new HashMap<String,Timestamp>();
        beaconManager = new BeaconManager(this);
        ourRegion =  new Region("region", null, null, null);

        listView1=(ListView)findViewById(R.id.Lista);
        sendButton = (Button)findViewById(R.id.button);

        sendButton.setOnClickListener(new View.OnClickListener() {
                                          @Override
                                          public void onClick(View v) {
                                              userList();
                                              addUser();
                                              addBeacon("pierwszy", "drugi", "trzeci");
                                          }
        });
    }

    @Override
    protected void onStart(){
        super.onStart();
        startRangingBeacons();
        cleanOldBeaconsAfter(TIME_IN_OURS);
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
                        sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Date date = new Date();
                    Timestamp twoHoursAgo = new Timestamp(date.getTime() - (long)(timeInHours * HOUR));
                    for(Map.Entry<String,Timestamp> entry: currentBeacons.entrySet()){
                        String key = entry.getKey();
                        Timestamp timestamp = entry.getValue();
                        if(timestamp.before(twoHoursAgo)){
                            currentBeacons.remove(key);
                        }
                    }
                }
            }
        };

        t.start();
    }

    private void addUser(){
        address = SERVER_ADDRESS + "/api/user/add";

        Map<String, String> data = new HashMap<String, String>();
        data.put(tag_mac, currentBeacons.toString());
        data.put(tag_token, AccessToken.getCurrentAccessToken().getToken());

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

    public String getAddress(){
        return this.address;
    }

    public JSONObject getData(){
        return this.data;
    }

    public void handleResponse(JSONObject response){
        Map<String, Object> serverResponseMap = null;
        if(response == null){
            Log.d("error", "response jest nullem");
            return;
        }
        if(response.has(tag_data)){
            try {
                JSONArray data = (JSONArray)response.get("data");
                serverResponseMap = handleListResponse(data);
            } catch(JSONException e){
                e.printStackTrace();
            }

        } else {
            Log.d("response handler", "There is no data");
        }
        if (serverResponseMap == null) return;
        Log.d("server Response", serverResponseMap.toString());
    };

    public Map<String,Object> handleListResponse(JSONArray data){
        for (int i = 0; i < data.length(); i++) {
            try {
                JSONObject friend = data.getJSONObject(i);
                updateFriendList(friend);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void updateFriendList(JSONObject friend) throws JSONException {
        String mac = (String) friend.get(tag_mac);
        String time = (String) friend.get(tag_time);
        String user = (String) friend.get(tag_user);

        if(friends.get(mac) == null){
            Log.d("Creating new known ", "beacon for: "+mac);
            friends.put(mac, new HashMap<String,String>());
        }

        Map<String,String> beacon = friends.get(mac);
        beacon.put(user, time);
        updateView();
    }

    private void updateView(){


        RowBean RowBean_data[] = new RowBean[]{
                new RowBean(tag_user)
        };
        RowAdapter adapter = new RowAdapter(this, R.layout.format_friend_line, RowBean_data);

        listView1.setAdapter(adapter);
    }

}
