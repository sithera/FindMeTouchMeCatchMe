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

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

public class BeaconFinder extends ActionBarActivity {

    private Region ourRegion;
    private BeaconManager beaconManager;
    private Double TIME_IN_OURS = 0.5;
    private volatile boolean threadsShouldBeRunning = true;
    private final long HOUR  = 3600*1000;
    private Button sendButton;
    private  final String SERVER_ADDRESS = "https://slow.telemabk.pl";
    private String address;
    private JSONObject data;

    private final String tag_data = "data";
    private final String tag_user = "user";
    private final String tag_time = "time";
    private final String tag_mac = "mac";
    private final String tag_token = "token";
    private final String tag_event = "event";
    private final String tag_name = "name";
    private final String tag_description = "description";

    private Map<String,Timestamp> currentBeacons;
    public ListView listView1;
    //public static String[] dataList = new String[30];
    ArrayList<String> dataList = new ArrayList<String>(30);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranging_and_displaying);
        currentBeacons = new HashMap<String,Timestamp>();
        beaconManager = new BeaconManager(this);
        ourRegion =  new Region("region", null, null, null);

//        listView1=(ListView)findViewById(R.id.Lista);
//        sendButton = (Button)findViewById(R.id.button);
//
//        sendButton.setOnClickListener(new View.OnClickListener() {
//                                          @Override
//                                          public void onClick(View v) {
//
//                                          }
//        });
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
        address = SERVER_ADDRESS + "api/beacon/add";

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
//        RowBean RowBean_data[] = new RowBean[]{
//                new RowBean(tag_user)
//        };
//        RowAdapter adapter = new RowAdapter(getApplicationContext(), R.layout.format_friend_line, RowBean_data);
//
//        listView1.setAdapter(adapter);
    }

    public String getAddress(){
        return this.address;
    }

    public JSONObject getData(){
        return this.data;
    }

    public void handleResponse(JSONObject response){};

}
