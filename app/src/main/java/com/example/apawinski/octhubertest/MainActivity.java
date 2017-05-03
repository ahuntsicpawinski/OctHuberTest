package com.example.apawinski.octhubertest;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, AdapterView.OnItemClickListener,
        GoogleApiClient.OnConnectionFailedListener {

    private EditText editTextSearch;
    private Button buttonSearch;
    private ListView listViewSearchResults;
    private TextView textViewSearchResults;

    private GoogleApiClient googleApiClient;
    private AutocompleteFilter filter;

    // The next few attributes are used in resolving google api errors
    // More here : https://developers.google.com/android/guides/api-client#HandlingFailures

    private static final int REQUEST_RESOLVE_ERROR = 1001;      // Request code for resolution activity
    private static final String DIALOG_ERROR = "dialog_error";  // Unique tag for the error dialog fragment
    private boolean isResolvingError = false;                   // Used to track whether app is already resolving error

    ArrayList<String> businessIds;
    HashMap<String,String> businessMap;
    ArrayList<HashMap<String, String>> businessDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        businessIds = new ArrayList<>();
        businessMap = new HashMap<>();
        businessDetails = new ArrayList<>();

        editTextSearch = (EditText) findViewById(R.id.editTextSearch);
        buttonSearch = (Button) findViewById(R.id.buttonSearch);
        listViewSearchResults = (ListView) findViewById(R.id.listViewSearchResults);
        textViewSearchResults = (TextView) findViewById(R.id.textViewSearchResults);

        buttonSearch.setOnClickListener(this);
        listViewSearchResults.setOnItemClickListener(this);

        googleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, this)           // this = MainActivity, this = OnConnectionFailedListener
                .build();

        filter = new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ESTABLISHMENT)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // This whole part is a check on whether the api is available.
        // If not, it throws an error dialog that prompts Google Play Store update
        // More here : https://developers.google.com/android/guides/setup

        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        int resultCode = availability.isGooglePlayServicesAvailable(this);

        if(resultCode != 0){                            // 0 is SUCCESS
            showErrorDialog(resultCode);                // Shows dialog using GoogleApiAvailability.getErrorDialog()
            isResolvingError = true;
        }
    }

    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.buttonSearch :
                if (editTextSearch.getText() != null) {
                    String businessName = editTextSearch.getText().toString();
                    getBusinessIds(businessName);
                    Log.d("GET_IDS","Entered");

                    ArrayList<HashMap<String, String>> businessDetails = new ArrayList<>();

                    for (String businessId : businessIds){
                        Log.d("GET_DETAILS","Entered");
                        new GetBusinessDetailsTask().execute(businessId);
                        businessDetails.add(businessMap);
                    }

                    BusinessAdapter adapter = new BusinessAdapter(MainActivity.this, R.layout.custom_list_business, businessDetails);
                    listViewSearchResults.setAdapter(adapter);

                    if (businessDetails.size() == 1){
                        setDetails(businessDetails.get(0));
                    }

                } else {
                    Log.d("SEARCH","Search field is empty");
                }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id){
        switch (parent.getId()){
            case R.id.listViewSearchResults :
                setDetails(businessDetails.get(position));
        }
    }

    private void setDetails(HashMap<String, String> details){

            String strDetails = "Business name : " + details.get("name") + "\n" + "Business hours : " + details.get("hours");
            textViewSearchResults.setText(strDetails);
    }

    private void getBusinessIds(String businessName) {

        LatLngBounds bounds = new LatLngBounds(new LatLng(45.443469, -73.718508),
                new LatLng(45.595500, -73.506549));
        PendingResult<AutocompletePredictionBuffer> results =
                Places.GeoDataApi.getAutocompletePredictions(googleApiClient, businessName, bounds, filter);
//        AutocompletePredictionBuffer autocompletePredictions = results.await(60, TimeUnit.SECONDS);
        results.setResultCallback(new ResultCallback<AutocompletePredictionBuffer>() {
            @Override
            public void onResult(@NonNull AutocompletePredictionBuffer autocompletePredictions) {
                if (autocompletePredictions.getStatus().isSuccess()){
                    businessIds = new ArrayList<>();
                    for(int i = 0; i < 5; i++){
                        String businessId = autocompletePredictions.get(i).getPlaceId();
                        Log.d("GET_IDS","Business id: " + businessId);
                        businessIds.add(businessId);
                    }
                }
            }
        });
//        autocompletePredictions.release();
    }

    private class GetBusinessDetailsTask extends AsyncTask<String, Integer, HashMap<String,String>> {

        protected HashMap<String, String> doInBackground(String... strings){

            HttpURLConnection conn = null;
            StringBuilder jsonResults = new StringBuilder();
            try {
                StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place");
                sb.append("/details");
                sb.append("/json");
                sb.append("?placeid=" + strings);
                sb.append("&key=AIzaSyAfywnBfjs2TxJHNA9YJ-KQGioS85ehP5g");

                URL url = new URL(sb.toString());
                conn = (HttpURLConnection) url.openConnection();
                InputStreamReader in = new InputStreamReader(conn.getInputStream());

                // Load the results into a StringBuilder
                int read;
                char[] buff = new char[1024];
                while ((read = in.read(buff)) != -1) {
                    jsonResults.append(buff, 0, read);
                }
                Log.d("GET_DETAILS","JSONresult: " + jsonResults.toString());
            } catch (MalformedURLException e) {
                Log.e("GET_DETAILS", "Error processing Places API URL", e);
                return null;
            } catch (IOException e) {
                Log.e("GET_DETAILS", "Error connecting to Places API", e);
                return null;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            HashMap<String,String> businessMap = new HashMap<>();

            try {
                // Create a JSON object hierarchy from the results
                JSONObject jsonObj = new JSONObject(jsonResults.toString()).getJSONObject("result");
                Log.d("GET_DETAILS","result: " + jsonObj.toString());

                if(jsonObj.has("name")){
                    businessMap.put("name",jsonObj.getString("name"));
                }
                if(jsonObj.has("opening_hours")){
                    JSONObject openHoursObj = jsonObj.getJSONObject("opening_hours");
                    if (openHoursObj.has("periods[]")){
                        JSONArray periodsArray = openHoursObj.getJSONArray("periods[]");
                        businessMap.put("hours", periodsArray.toString());

                        // TODO: Further JSON parsing
/*                  for(int i = 0; i < 7; i++){
                        JSONObject period = periodsArray.getJSONObject(i);
                        if (period.name) {

                        }
                    } */
                    } else if (openHoursObj.has("weekday_text")){
                        businessMap.put("hours", openHoursObj.getString("weekday_text"));
                        // TODO: Further JSON parsing
                    }
                } else {
                    businessMap.put("hours","Opening hours unavailable");
                }
            } catch (JSONException e) {
                Log.e("CREATE_TIMES", "Error processing JSON results", e);
            }
            return businessMap;
        }

        protected void onPostExecute(HashMap<String, String> businessMap) {
            setData(businessMap);
        }
    }

    private void setData(HashMap<String,String> businessMap){
        this.businessMap = businessMap;
    }

    @Override
    public void onConnectionFailed(ConnectionResult result){
        if (!isResolvingError) {                // Not already attempting to resolve error
            if (result.hasResolution()) {
                try {
                    isResolvingError = true;
                    result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
                } catch (IntentSender.SendIntentException e) {      // Error with resolution intent, retries to connect
                    googleApiClient.connect();
                }
            } else {
                showErrorDialog(result.getErrorCode());     // Shows dialog using GoogleApiAvailability.getErrorDialog()
                isResolvingError = true;
            }
        }
    }

    // This next part is building the error dialog used in onConnectionFailed()

    private void showErrorDialog(int errorCode) {
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), DIALOG_ERROR);
    }

    public void onDialogDismissed() {
        isResolvingError = false;
    }

    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() {}

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((MainActivity) getActivity()).onDialogDismissed();
        }
    }

    // Activity retries to connect once error is resolved

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_OK) {
            if (!googleApiClient.isConnecting() && !googleApiClient.isConnected()){
                googleApiClient.connect();
            }
        }
    }
}
