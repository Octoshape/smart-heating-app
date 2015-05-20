package ch.ethz.smartheating;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by schmisam on 19/05/15.
 */
public class Request {

    private HttpRequestBase mRequest;

    public void registerResidence (String RFID) {
        URI uri = null;
        try {
            uri = new URI("http", null, "52.28.68.182", 8000, "/residence/", null, null);

            mRequest = new HttpPost(uri);

            List<NameValuePair> params = new ArrayList<NameValuePair>(2);
            params.add(new BasicNameValuePair("rfid", RFID));
            ((HttpPost)mRequest).setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        // execute request via AsyncTask
        PostAsyncWorker worker = new PostAsyncWorker();
        worker.execute();
    }

    public void registerUser(String IMEI, String RFID) {
        URI uri = null;
        try {
            uri = new URI("http", null, "52.28.68.182", 8000, "/residence/" + RFID + "/user/", null, null);

            mRequest = new HttpPost(uri);

            List<NameValuePair> params = new ArrayList<NameValuePair>(2);
            params.add(new BasicNameValuePair("imei", IMEI));
            params.add(new BasicNameValuePair("name", "du faggot!"));
            ((HttpPost)mRequest).setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // execute request via AsyncTask
        AsyncWorker worker = new AsyncWorker();
        worker.execute();
    }

    public void getTemperature() {

        URI uri = null;
        try {
            uri = new URI("http", null, "52.28.68.182", 8000, "/residence/", null, null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        // create request with Uri
        mRequest = new HttpGet(uri);

        // add header's fields
        //mRequest.addHeader("Connection", "close");
        //mRequest.addHeader("Accept", "application/json");

        // execute request via AsyncTask
        AsyncWorker worker = new AsyncWorker();
        worker.execute();
    }

    public class PostAsyncWorker extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            // use library to create and send request
            HttpClient client = new DefaultHttpClient();

            String response = "";

            try {
                HttpResponse httpResponse = client.execute(mRequest);
                response = EntityUtils.toString(httpResponse.getEntity());

            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // return response
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Log.d("Request", result);
        }
    }

    public class AsyncWorker extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            // use library to create and send request
            HttpClient client = new DefaultHttpClient();

            String response = "";

            try {
                HttpResponse httpResponse = client.execute(mRequest);
                response = EntityUtils.toString(httpResponse.getEntity());

            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // return response
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            JSONTokener tokener = new JSONTokener(result);
            try {
                JSONArray residences = new JSONArray(tokener);
                Log.d("Request.java", residences.getJSONObject(0).getString("rfid"));

            } catch (JSONException e) {
                System.out.println("JSON parse problem");
                e.printStackTrace();
            }
        }
    }
}
