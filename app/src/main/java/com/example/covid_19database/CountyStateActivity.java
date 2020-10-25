package com.example.covid_19database;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.provider.Telephony.Mms.Part.TEXT;

public class CountyStateActivity extends AppCompatActivity {
    TextView place;
    TextView numCases;
    TextView numDeaths;
    TextView instruct;
    TextView casePrediction;
    TextView deathPrediction;
    EditText userCounty;
    Spinner userState;
    String county;
    String state;
    String abbrv;
    GraphView graph;
    Map<String, String> states;
    BottomNavigationView bottomNavigationView;

    ArrayList<Double> seriesC;
    ArrayList<Double> seriesD;
    int savedCasesC;
    int savedDeathsC;
    int savedCasesP;
    int savedDeathsP;
    String savedPlace;
    int savedState;
    String savedCounty;

    ArrayAdapter<String> adapter;

    public final String COUNTY_CURRENT_CASES = "CO_CURRENT_CASES";
    public final String COUNTY_CURRENT_DEATHS = "CO_CURRENT_DEATHS";
    public final String COUNTY_PREDICT_CASES = "CO_PREDICT_CASES";
    public final String COUNTY_PREDICT_DEATHS = "CO_PREDICT_DEATHS";
    public final String COUNTY_CASE_SERIES = "CO_CASE_SERIES";
    public final String COUNTY_DEATH_SERIES = "CO_DEATH_SERIES";
    public final String COUNTY_PLACE = "CO_PLACE";
    public final String COUNTY_USER_COUNTY = "CO_COUNTY";
    public final String COUNTY_USER_STATE = "CO_STATE";
    public final String SHARED_PREFS = "SHARED PREFERENCES";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.state_and_county);

        states = new HashMap<>();
        fillMap();

        String[] keys = new String[states.size()];
        String[] values = new String[states.size()];
        final String[] displayStates = new String[states.size() + 1];

        int index = 0;
        for (Map.Entry<String, String> mapEntry : states.entrySet()) {
            keys[index] = mapEntry.getKey();
            values[index] = mapEntry.getValue();
            index++;
        }
        ArrayList<String> keysList = new ArrayList<>();
        for(int i = 0; i < keys.length; i++){
            keysList.add(keys[i]);
        }
        Collections.sort(keysList);

        displayStates[0] = ("Select state");
        for(int i = 1; i < keysList.size() + 1; i++){
            displayStates[i] = keysList.get(i - 1);
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, displayStates);

        place = findViewById(R.id.id_place);
        numCases = findViewById(R.id.id_numCases);
        numDeaths = findViewById(R.id.id_numDeaths);
        deathPrediction = findViewById(R.id.id_deathPrediction);
        casePrediction = findViewById(R.id.id_casePrediction);
        instruct = findViewById(R.id.id_instruct);
        userCounty = findViewById(R.id.id_userCounty);
        userState = findViewById(R.id.id_userState);
        graph = findViewById(R.id.id_graph);

        loadData();
        updateViews();

        bottomNavigationView = findViewById(R.id.id_bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.id_county);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch(item.getItemId()){
                    case R.id.id_country:
                        startActivity(new Intent(getApplicationContext(), CountryActivity.class));
                        overridePendingTransition(0, 0);
                        return true;
                    case R.id.id_county:
                        return true;
                }
                return false;
            }
        });

        userCounty = findViewById(R.id.id_userCounty);
        userState = findViewById(R.id.id_userState);
        county = "";
        state = "";
        abbrv = "";

        instruct = findViewById(R.id.id_instruct);

        userState.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                state = displayStates[position];
                if(!state.equals("Select state")) {
                    try {
                        abbrv = states.get(state);
                        Log.d("TAG", state);
                        instruct.setText("Please enter a county in the field provided.");
                    } catch (Exception e) {
                        Log.d("TAG", e.getMessage());
                    }
                    userCounty.setVisibility(View.VISIBLE);
                }
                else
                    userCounty.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        userCounty.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() != 0) {
                    county = s.toString().substring(0, 1).toUpperCase() + s.toString().substring(1).toLowerCase();
                    if ((county.length() != 0) && (state.length() != 0)) {
                        CovidAPI_US api = new CovidAPI_US(county, state.toLowerCase());
                        api.execute();
                    }
                }
            }
        });
    }
    public class CovidAPI_US extends AsyncTask<String, Void, String> {
        private String county;
        private String state;
        private Response countyCurrent;
        private Response countyHistory;
        private JSONObject jsonMain; //Current cases and deaths for county
        private JSONObject jsonMain2; //History of cases
        private JSONObject jsonMain3; //History of deaths

        public CovidAPI_US(String county, String state){
            this.county = county;
            this.state = state;
        }

        @Override
        protected String doInBackground(String... strings) {
            Log.d("TAG", county);
            try{
                OkHttpClient client = new OkHttpClient().newBuilder().build();
                String url = "https://covid19-us-api.herokuapp.com/county";
                MediaType mediaType = MediaType.parse("text/plain");
                RequestBody body = RequestBody.create("{\n    \"state\": \"" + abbrv + "\",\n    \"county\": \"" + county + "\"\n}", mediaType);
                Request request = new Request.Builder()
                        .url(url)
                        .method("POST", body)
                        .build();
                countyCurrent = client.newCall(request).execute();

                String state1 = countyCurrent.body().string();
                JSONObject jsonState = new JSONObject(state1);
                JSONArray jsonMsg = jsonState.getJSONArray("message");
                jsonMain = jsonMsg.getJSONObject(0);

                OkHttpClient client2 = new OkHttpClient().newBuilder()
                        .build();
                Request request2 = new Request.Builder()
                        .url("https://corona.lmao.ninja/v2/historical/usacounties/" + state + "?lastdays=all")
                        .method("GET", null)
                        .build();
                countyHistory = client2.newCall(request2).execute();
                Log.d("TAG", countyHistory.toString());

                String county1 = countyHistory.body().string();
                JSONArray arr = new JSONArray(county1);

                int i = 0;
                boolean found = false;

                while(i < arr.length() && !found){
                    JSONObject js = arr.getJSONObject(i);
                    if(js.getString("county").equalsIgnoreCase(county)) {
                        found = true;
                        jsonMain2 = js;
                    }
                    i++;
                }
                Log.d("TAG", jsonMain2.getString("county"));
                JSONObject time = jsonMain2.getJSONObject("timeline");
                jsonMain2 = time.getJSONObject("cases");
                jsonMain3 = time.getJSONObject("deaths");
                Log.d("TAG", jsonMain2.toString());
                Log.d("TAG", jsonMain3.toString());
            }catch (Exception e){
                Log.d("TAG", e.getMessage());
            }
            return null;
        }
        @Override
        protected void onPostExecute(String s) {
            place = findViewById(R.id.id_place);
            numCases = findViewById(R.id.id_numCases);
            numDeaths = findViewById(R.id.id_numDeaths);
            casePrediction = findViewById(R.id.id_casePrediction);
            deathPrediction = findViewById(R.id.id_deathPrediction);
            graph = findViewById(R.id.id_graph);

            try {
                final PointsGraphSeries<DataPoint> seriesCases = new PointsGraphSeries<>();
                final PointsGraphSeries<DataPoint> seriesDeaths = new PointsGraphSeries<>();

                if((jsonMain2.length() != 0) && (jsonMain3.length() != 0)) {
                    String str1 = jsonMain2.toString();
                    String str2 = jsonMain3.toString();
                    ArrayList<Double> ys = new ArrayList<>();
                    ArrayList<Double> ys2 = new ArrayList<>();
                    for (int i = 0; i < jsonMain2.length(); i++) {
                        boolean single1 = false;
                        for (int j = 1; j < 10; j++) {
                            if (str1.substring(2, 11).contains("/" + j + "\\")) {
                                single1 = true;
                                break;
                            }
                        }
                        DataPoint point1;
                        DataPoint point2;
                        if (!single1) {
                            String sub1 = str1.substring(2, 3) + str1.substring(4, 7) + str1.substring(8, 11); //12
                            int num1 = jsonMain2.getInt(sub1);
                            ys.add((double) num1);
                            int length1 = String.valueOf(num1).length();
                            point1 = new DataPoint(i, num1);
                            str1 = str1.substring(0, 1) + str1.substring(14 + length1);

                            String sub2 = str2.substring(2, 3) + str2.substring(4, 7) + str2.substring(8, 11); //12
                            int num2 = jsonMain3.getInt(sub2);
                            ys2.add((double) num2);
                            int length2 = String.valueOf(num2).length();
                            point2 = new DataPoint(i, num2);
                            str2 = str2.substring(0, 1) + str2.substring(14 + length2);
                        } else{
                            String sub1 = str1.substring(2, 3) + str1.substring(4, 6) + str1.substring(7, 10); //12
                            int num1 = jsonMain2.getInt(sub1);
                            ys.add((double) num1);
                            int length1 = String.valueOf(num1).length();
                            point1 = new DataPoint(i, num1);
                            str1 = str1.substring(0, 1) + str1.substring(13 + length1);

                            String sub2 = str2.substring(2, 3) + str2.substring(4, 6) + str2.substring(7, 10); //12
                            int num2 = jsonMain3.getInt(sub2);
                            ys2.add((double) num2);
                            int length2 = String.valueOf(num2).length();
                            point2 = new DataPoint(i, num2);
                            str2 = str2.substring(0, 1) + str2.substring(13 + length2);
                        }
                        seriesCases.appendData(point1, true, jsonMain2.length());
                        seriesDeaths.appendData(point2, true, jsonMain3.length());
                    }
                    TaylorCalculator calc = new TaylorCalculator(ys);
                    long fin = (long)(calc.calculateSlope() + seriesCases.getHighestValueY());

                    TaylorCalculator calc2 = new TaylorCalculator(ys2);
                    long fin2 = (long)(calc2.calculateSlope() + Integer.parseInt(jsonMain.getString("death")));

                    NumberFormat myFormat = NumberFormat.getInstance();
                    myFormat.setGroupingUsed(true);

                    casePrediction.setText("Predicted Number of Cases for Tomorrow: " + myFormat.format((int) fin));
                    deathPrediction.setText("Predicted Number of Deaths for Tomorrow: " + myFormat.format((int) fin2));

                    Log.d("TAG", "reached: " + fin);
                    numCases.setVisibility(View.VISIBLE);
                    numCases.setText("Confirmed: " + myFormat.format((int) seriesCases.getHighestValueY()));

                    instruct.setVisibility(View.INVISIBLE);
                    numDeaths.setVisibility(View.VISIBLE);
                    place.setVisibility(View.VISIBLE);
                    numDeaths.setText("Deaths: " + myFormat.format(seriesDeaths.getHighestValueY()));
                    place.setText(jsonMain.getString("county_name") + ", " + jsonMain.getString("state_name"));

                    graph.setVisibility(View.VISIBLE);

                    graph.getViewport().setScrollable(true); // enables horizontal scrolling
                    graph.getViewport().setScrollableY(true); // enables vertical scrolling
                    graph.getViewport().setScalable(true); // enables horizontal zooming and scrolling
                    graph.getViewport().setScalableY(true); // enables vertical zooming and scrolling

                    seriesCases.setSize(16f);
                    seriesDeaths.setSize(12f);

                    graph.removeAllSeries();

                    seriesCases.setColor(Color.WHITE);
                    graph.addSeries(seriesCases);
                    seriesDeaths.setColor(Color.RED);
                    graph.addSeries(seriesDeaths);

                    saveData((int) seriesCases.getHighestValueY(), (int) seriesDeaths.getHighestValueY(), (int) fin, (int) fin2, place.getText().toString(), userCounty.getText().toString(), userState.getSelectedItemPosition(), ys, ys2);
                }
                else
                    instruct.setText("Not a valid state or county! Please check your information.");
            }catch (Exception e){
                Log.d("TAG", e.getMessage());
            }
            super.onPostExecute(s);
        }
    }
    public void fillMap(){
        states.put("Alabama","AL");
        states.put("Alaska","AK");
        states.put("Arizona","AZ");
        states.put("Arkansas","AR");
        states.put("California","CA");
        states.put("Colorado","CO");
        states.put("Connecticut","CT");
        states.put("Delaware","DE");
        states.put("Florida","FL");
        states.put("Georgia","GA");
        states.put("Hawaii","HI");
        states.put("Idaho","ID");
        states.put("Illinois","IL");
        states.put("Indiana","IN");
        states.put("Iowa","IA");
        states.put("Kansas","KS");
        states.put("Kentucky","KY");
        states.put("Louisiana","LA");
        states.put("Maine","ME");
        states.put("Maryland","MD");
        states.put("Massachusetts","MA");
        states.put("Michigan","MI");
        states.put("Minnesota","MN");
        states.put("Mississippi","MS");
        states.put("Missouri","MO");
        states.put("Montana","MT");
        states.put("Nebraska","NE");
        states.put("Nevada","NV");
        states.put("New Hampshire","NH");
        states.put("New Jersey","NJ");
        states.put("New Mexico","NM");
        states.put("New York","NY");
        states.put("North Carolina","NC");
        states.put("North Dakota","ND");
        states.put("Ohio","OH");
        states.put("Oklahoma","OK");
        states.put("Oregon","OR");
        states.put("Pennsylvania","PA");
        states.put("Puerto Rico","PR");
        states.put("Rhode Island","RI");
        states.put("South Carolina","SC");
        states.put("South Dakota","SD");
        states.put("Tennessee","TN");
        states.put("Texas","TX");
        states.put("Utah","UT");
        states.put("Vermont","VT");
        states.put("Virginia","VA");
        states.put("Washington","WA");
        states.put("West Virginia","WV");
        states.put("Wisconsin","WI");
        states.put("Wyoming","WY");
    }
    public class TaylorCalculator{
        private ArrayList<Double> nums;
        private long slope;

        public TaylorCalculator(ArrayList<Double> list){
            this.nums = list;

            Log.d("TAG", nums.toString());
        }
        public long calculateSlope(){
            int size = nums.size() - 1;
            slope = (long)((nums.get(size) - nums.get(size - 1)) + 0.85*(nums.get(size - 1) - nums.get(size - 2)) + 0.65*(nums.get(size - 2) - nums.get(size - 3)) + 0.45*(nums.get(size - 3) - nums.get(size - 4)) + 0.25*(nums.get(size - 4) - nums.get(size - 5))) / 5;
            return slope;
        }
    }
    public void saveData(int casesC, int deathsC, int casesP, int deathsP, String place, String county, int state, ArrayList<Double> seriesC, ArrayList<Double> seriesD) {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Gson gson = new Gson();
        String jsonC = gson.toJson(seriesC);
        editor.putString(COUNTY_CASE_SERIES, jsonC);

        String jsonD = gson.toJson(seriesD);
        editor.putString(COUNTY_DEATH_SERIES, jsonD);

        editor.putInt(COUNTY_PREDICT_DEATHS, deathsP);
        editor.putInt(COUNTY_PREDICT_CASES, casesP);
        editor.putInt(COUNTY_CURRENT_DEATHS, deathsC);
        editor.putInt(COUNTY_CURRENT_CASES, casesC);

        editor.putString(COUNTY_PLACE, place);
        editor.putString(COUNTY_USER_COUNTY, county);
        editor.putInt(COUNTY_USER_STATE, state);

        editor.apply();
    }
    public void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);

        Gson gson = new Gson();
        String json = sharedPreferences.getString(COUNTY_CASE_SERIES, null);
        String json2 = sharedPreferences.getString(COUNTY_DEATH_SERIES, null);
        Type type = new TypeToken<ArrayList<Double>>() {}.getType();
        seriesC = gson.fromJson(json, type);
        seriesD = gson.fromJson(json2, type);

        savedCounty = sharedPreferences.getString(COUNTY_USER_COUNTY, null);
        savedState = sharedPreferences.getInt(COUNTY_USER_STATE, 0);
        savedPlace = sharedPreferences.getString(COUNTY_PLACE, null);

        savedCasesC = sharedPreferences.getInt(COUNTY_CURRENT_CASES, 0);
        savedDeathsC = sharedPreferences.getInt(COUNTY_CURRENT_DEATHS, 0);
        savedCasesP = sharedPreferences.getInt(COUNTY_PREDICT_CASES, 0);
        savedDeathsP = sharedPreferences.getInt(COUNTY_PREDICT_DEATHS, 0);

        if (seriesC == null)
            seriesC = new ArrayList<>();
        if(seriesD == null)
            seriesD = new ArrayList<>();

        if(savedCounty == null)
            savedCounty = " ";
        if(savedPlace == null)
            savedPlace = " ";
    }
    public void updateViews() {
        NumberFormat myFormat = NumberFormat.getInstance();
        myFormat.setGroupingUsed(true);

        boolean all = true;

        if (!savedPlace.equals(" ")){
            place.setVisibility(View.VISIBLE);
            place.setText(savedPlace);
        }
        else
            all = false;

        if(savedCasesC != 0) {
            numCases.setVisibility(View.VISIBLE);
            numCases.setText("Confirmed: " + myFormat.format(savedCasesC));
        }
        else
            all = false;

        if(savedDeathsC != 0) {
            numDeaths.setVisibility(View.VISIBLE);
            numDeaths.setText("Deaths: " + myFormat.format(savedDeathsC));
        }
        else
            all = false;

        if(savedDeathsP != 0) {
            deathPrediction.setVisibility(View.VISIBLE);
            deathPrediction.setText("Predicted number of deaths for tomorrow: " + myFormat.format(savedDeathsP));
        }
        else
            all = false;

        if(savedCasesC != 0) {
            casePrediction.setVisibility(View.VISIBLE);
            casePrediction.setText("Predicted number of cases for tomorrow: " + myFormat.format(savedCasesP));
        }
        else
            all = false;

        if(!savedCounty.equals(" ")) {
            userCounty.setVisibility(View.VISIBLE);
            userCounty.setText(savedCounty);
        }
        else
            all = false;

        if(savedState != 0) {
            userState.setAdapter(adapter);
            userState.setVisibility(View.VISIBLE);
            userState.setSelection(savedState);
        }
        else {
            all = false;
            userState.setAdapter(adapter);
        }

        if((seriesD.size() != 0) && (seriesC.size() != 0)) {
            graph = findViewById(R.id.id_graph);

            PointsGraphSeries<DataPoint> seriesCases = new PointsGraphSeries<>();
            PointsGraphSeries<DataPoint> seriesDeaths = new PointsGraphSeries<>();

            for(int i = 0; i < seriesC.size(); i++){
                seriesCases.appendData(new DataPoint(i, seriesC.get(i)), true, seriesC.size());
                seriesDeaths.appendData(new DataPoint(i, seriesD.get(i)), true, seriesD.size());
            }

            graph.setVisibility(View.VISIBLE);
            graph.getViewport().setScrollable(true); // enables horizontal scrolling
            graph.getViewport().setScrollableY(true); // enables vertical scrolling
            graph.getViewport().setScalable(true); // enables horizontal zooming and scrolling
            graph.getViewport().setScalableY(true); // enables vertical zooming and scrolling

            seriesCases.setSize(16f);
            seriesDeaths.setSize(12f);

            graph.removeAllSeries();

            seriesCases.setColor(Color.WHITE);
            graph.addSeries(seriesCases);
            seriesDeaths.setColor(Color.RED);
            graph.addSeries(seriesDeaths);
        }
        else
            all = false;

        if(all)
            instruct.setVisibility(View.INVISIBLE);
    }
}
