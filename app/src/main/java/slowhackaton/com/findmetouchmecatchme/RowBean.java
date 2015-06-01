package slowhackaton.com.findmetouchmecatchme;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mateusz Bereziński on 2015-04-24.
 */
public class RowBean implements Caller{

    private Timestamp timestamp;
    public String userName;
    public String id;
    private String globalId = null;
    private JSONObject data;
    private String tag_id = "id";
    private String SERVER_ADDRESS = "https://slow.telemabk.pl/api/user/fuck_fb";
    private String tag_success = "success";

    public RowBean(){

    }

    public RowBean(String userName, String id) {
        this.userName = userName;
        this.id = id;
        this.globalId = null;
    }

    public RowBean(String userName, String id, Timestamp timestamp){
        this.userName = userName;
        this.id = id;
        this.timestamp = timestamp;
        this.globalId = null;
    }

    @Override
    public boolean equals(Object object)
    {
        boolean isEqual= false;

        if (object != null && object instanceof RowBean)
        {
            isEqual = (this.id.equals(((RowBean) object).id));
        }

        return isEqual;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    public boolean isClickable(){
        return this.globalId != null;
    }

    public void requestForGlobalId(){
        Map<String, String> data = new HashMap<String, String>();
        data.put(tag_id,id);
        JSONObject json = new JSONObject(data);
        send(json);
    }

    public String getGlobalId(){
        return this.globalId;
    }

    private void send(JSONObject data){
        this.data = data;
        SendPostRequestTask sendTask = new SendPostRequestTask(this);
        sendTask.execute();
    }


    @Override
    public void handleResponse(JSONObject response) {
        if(response.has(tag_success)){
            boolean success;
            try {
                success = (boolean) response.get(tag_success);
                if(success){
                    this.globalId = (String) response.get(tag_id);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public String getAddress() {
        return SERVER_ADDRESS;
    }

    @Override
    public JSONObject getData() {
        return this.data;
    }
}