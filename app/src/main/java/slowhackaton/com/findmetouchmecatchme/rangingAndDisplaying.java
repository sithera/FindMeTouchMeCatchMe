package slowhackaton.com.findmetouchmecatchme;

import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class rangingAndDisplaying extends ActionBarActivity {
    private Region ourRegion;
    private BeaconManager beaconManager;
    private Map<String,Timestamp> currentBeacons;
    private Double TIME_IN_OURS = 0.5;
    private volatile boolean threadsShouldBeRunning = true;
    private final long HOUR  = 3600*1000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranging_and_displaying);
        currentBeacons = new HashMap<String,Timestamp>();
        beaconManager = new BeaconManager(this);
        ourRegion =  new Region("region", null, null, null);
    }

     @Override
     protected void onStart(){
        startRangingBeacons();
         cleanOldBeaconsAfter(TIME_IN_OURS);
     }

    @Override
    protected void onStop(){
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ranging_and_displaying, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
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

        try {
            beaconManager.startRanging(ourRegion);
        }catch(RemoteException rException){
            rException.printStackTrace();
        }
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

    private void sendRequest(String url,JSONObject obj){
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params,10000);
        HttpConnectionParams.setSoTimeout(params, 10000);
        HttpClient httpClient = new DefaultHttpClient(params);
        String json = obj.toString();
        try{
            HttpPost httppost = new HttpPost(url.toString());
            httppost.setHeader("Content-type","application/json");

            StringEntity se = new StringEntity(json);
            httppost.setEntity(se);
//            httppost.setHeader("","");
            HttpResponse response = httpClient.execute(httppost);
        }catch(UnsupportedEncodingException e){
            e.printStackTrace();
        }catch(ClientProtocolException e){
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
