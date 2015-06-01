package slowhackaton.com.findmetouchmecatchme;

import android.os.AsyncTask;
import android.util.Log;

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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

public class SendPostRequestTask extends AsyncTask<Object,JSONObject,JSONObject> {
    private String address;
    private JSONObject data;
    private Caller caller;

    public SendPostRequestTask(Caller caller){
        trustEveryone();
        this.caller = caller;
        this.address = caller.getAddress();
        this.data = caller.getData();
    }

    @Override
    protected JSONObject doInBackground(Object[] objects) {
        JSONObject response = sendRequest(this.address, this.data);
        return response;
    }

    @Override
    protected void onPostExecute(JSONObject result) {
        this.caller.handleResponse(result);
    }

    private JSONObject sendRequest(final String url, final JSONObject obj){
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 10000);
        HttpConnectionParams.setSoTimeout(params, 10000);
        HttpClient httpClient = new DefaultHttpClient(params);
        String json = obj.toString();
        try {
            HttpPost httppost = new HttpPost(url.toString());
            httppost.setHeader("Content-type", "application/json");

            Log.d("request", json);

            StringEntity se = new StringEntity(json);
            httppost.setEntity(se);
            HttpResponse rawResponse = httpClient.execute(httppost);
            Log.d("response",rawResponse.toString());
            String responseJson = EntityUtils.toString(rawResponse.getEntity());
            Log.d("response",responseJson);
            return new JSONObject(responseJson);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void trustEveryone() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager(){
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {}
                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {}
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }}}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(
                    context.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
