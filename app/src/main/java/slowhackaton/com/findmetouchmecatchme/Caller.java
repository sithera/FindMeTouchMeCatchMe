package slowhackaton.com.findmetouchmecatchme;

import org.json.JSONObject;

/**
 * Created by adam on 01.06.15.
 */
public interface Caller {
    void handleResponse(JSONObject result);
    String getAddress();
    JSONObject getData();
}
