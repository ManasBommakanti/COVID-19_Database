package com.example.covid_19database;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.neovisionaries.i18n.CountryCode;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.StringTokenizer;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CountryActivity extends AppCompatActivity {

    TextView place;
    TextView instruct;
    TextView numCases;
    TextView numDeaths;
    TextView casePrediction;
    TextView deathPrediction;
    String country;
    GraphView graph;
    Spinner userCountry;
    ArrayList<String> codes;
    BottomNavigationView bottomNavigationView;

    ArrayList<Double> seriesC;
    ArrayList<Double> seriesD;
    int savedCasesC;
    int savedDeathsC;
    int savedCasesP;
    int savedDeathsP;
    String savedPlace;
    int savedCountry;

    ArrayAdapter<String> adapter;

    public final String COUNTRY_CURRENT_CASES = "CON_CURRENT_CASES";
    public final String COUNTRY_CURRENT_DEATHS = "CON_CURRENT_DEATHS";
    public final String COUNTRY_PREDICT_CASES = "CON_PREDICT_CASES";
    public final String COUNTRY_PREDICT_DEATHS = "CON_PREDICT_DEATHS";
    public final String COUNTRY_CASE_SERIES = "CON_CASE_SERIES";
    public final String COUNTRY_DEATH_SERIES = "CON_DEATH_SERIES";
    public final String COUNTRY_PLACE = "CON_PLACE";
    public final String COUNTRY_USER_COUNTRY = "CON_COUNTRY";
    public final String SHARED_PREFS_2 = "SHARED PREFERENCES 2";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.country);

        codes = new ArrayList<>();

        userCountry = findViewById(R.id.id_userCountry);
        country = "";

        Country countryAlpha = new Country();
        countryAlpha.fill();
        countryAlpha.alphabetize();
        String[] displayCountriesBeta = countryAlpha.getDisplayCountries();
        final String[] displayCountries = new String[displayCountriesBeta.length + 1];
        displayCountries[0] = "Please select a country";

        for(int i = 1; i < displayCountries.length; i++){
            displayCountries[i] = displayCountriesBeta[i - 1];
        }

        String[] localesBeta = countryAlpha.getLocales();
        final String[] locales = new String[displayCountriesBeta.length + 1];
        locales[0] = " ";
        for(int i = 1; i < displayCountries.length; i++){
            locales[i] = localesBeta[i - 1];
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, displayCountries);

        place = findViewById(R.id.id_place);
        numCases = findViewById(R.id.id_numCases);
        numDeaths = findViewById(R.id.id_numDeaths);
        deathPrediction = findViewById(R.id.id_deathPrediction);
        casePrediction = findViewById(R.id.id_casePrediction);
        instruct = findViewById(R.id.id_instruct);
        userCountry = findViewById(R.id.id_userCountry);
        graph = findViewById(R.id.id_graph);

        loadData();
        updateViews();

        userCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String con = locales[position];
                String coun = displayCountries[position];
                Log.d("TAG", con);
                CovidAPI_ALL api = new CovidAPI_ALL(con, coun);
                api.execute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        bottomNavigationView = findViewById(R.id.id_bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.id_country);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch(item.getItemId()){
                    case R.id.id_country:
                        return true;
                    case R.id.id_county:
                        startActivity(new Intent(getApplicationContext(), CountyStateActivity.class));
                        overridePendingTransition(0, 0);
                        return true;
                }
                return false;
            }
        });
    }
    public class Country{
        private String locale;
        private String displayCountry;
        private Country[] countries;

        public Country(){
            locale = "";
            displayCountry = "";
        }
        public Country(String displayCountry, String loc){
            this.locale = loc;
            this.displayCountry = displayCountry;
        }
        public void fill(){
            String[] locales = Locale.getISOCountries();
            countries = new Country[locales.length];
            for(int i = 0; i < locales.length; i++){
                Locale obj = new Locale("", locales[i]);
                countries[i] = new Country(obj.getDisplayCountry(), locales[i]);
            }
        }
        public String toString(){
            return displayCountry + " " + locale;
        }
        public String getDisplayCountry(){
            return displayCountry;
        }
        public String getLocale(){
            return locale;
        }
        public Country[] alphabetize(){
            ArrayList<String> countriesList = new ArrayList<>();
            for(int i = 0; i < countries.length; i++){
                countriesList.add(countries[i].toString());
            }
            Collections.sort(countriesList);

            for(int i = 0; i < countriesList.size(); i++){
                String str = countriesList.get(i);
                StringTokenizer st = new StringTokenizer(str);
                int num = st.countTokens();
                String country = "";
                String loc = "";
                while(st.hasMoreTokens() && num != 0){
                    if(num > 1)
                        country = country + " " + st.nextToken();
                    else
                        loc = st.nextToken();
                    num--;
                }
                countries[i] = new Country(country, loc);
            }
            return countries;
        }
        public String[] getDisplayCountries(){
            String[] displayCountries = new String[countries.length];
            for(int i = 0; i < displayCountries.length; i++){
                displayCountries[i] = countries[i].getDisplayCountry();
            }
            return displayCountries;
        }
        public String[] getLocales(){
            String[] locales = new String[countries.length];
            for(int i = 0; i < locales.length; i++){
                locales[i] = countries[i].getLocale();
            }
            return locales;
        }
    }
    public class CovidAPI_ALL extends AsyncTask<String, Void, String>{
        private String code;
        private String coun;
        private Response countryCurrent;
        private Response countryCurrent2;
        private JSONArray jsonMain; //Current cases and deaths for country
        private JSONObject jsonMain2; //History of cases
        private JSONObject jsonMain3; //History of deaths

        public CovidAPI_ALL(String code, String country){
            this.code = code;
            coun = country;
        }
        @Override
        protected String doInBackground(String... strings) {
            Log.d("TAG", country + " ");
            try{
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .build();
                MediaType mediaType = MediaType.parse("text/plain");
                RequestBody body = RequestBody.create("{\n    \"alpha2Code\": \"" + code + "\"\n}", mediaType);
                Request request = new Request.Builder()
                        .url("https://covid19-us-api.herokuapp.com/country")
                        .method("POST", body)
                        .build();
                countryCurrent = client.newCall(request).execute();

                String country1 = countryCurrent.body().string();
                JSONObject jsonCountry = new JSONObject(country1);
                jsonMain = jsonCountry.getJSONArray("message");
                Log.d("TAG", countryCurrent.toString());

                OkHttpClient client1 = new OkHttpClient().newBuilder()
                        .build();
                Request request1 = new Request.Builder()
                        .url("https://corona.lmao.ninja/v2/historical/" + coun + "?lastdays=all")
                        .method("GET", null)
                        .build();
                countryCurrent2 = client1.newCall(request1).execute();

                String country = countryCurrent2.body().string();
                JSONObject jsona = new JSONObject(country);
                JSONObject jsonb = jsona.getJSONObject("timeline");

                jsonMain2 = jsonb.getJSONObject("cases");
                jsonMain3 = jsonb.getJSONObject("deaths");
                Log.d("TAG", countryCurrent2.toString());
            }catch(Exception e){
                Log.d("TAG", e.getMessage());
            }
            return null;
        }
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            place = findViewById(R.id.id_place);
            numCases = findViewById(R.id.id_numCases);
            numDeaths = findViewById(R.id.id_numDeaths);
            userCountry = findViewById(R.id.id_userCountry);
            casePrediction = findViewById(R.id.id_casePrediction);
            deathPrediction = findViewById(R.id.id_deathPrediction);
            instruct = findViewById(R.id.id_instruct);
            graph = findViewById(R.id.id_graph);

            try{
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
                    long fin = (long) (calc.calculateSlope() + seriesCases.getHighestValueY());

                    TaylorCalculator calc2 = new TaylorCalculator(ys2);
                    long fin2 = (long) (calc.calculateSlope() + seriesDeaths.getHighestValueY());

                    NumberFormat myFormat = NumberFormat.getInstance();
                    myFormat.setGroupingUsed(true);

                    casePrediction.setText("Predicted Number of Cases for Tomorrow: " + myFormat.format((int) fin));
                    deathPrediction.setText("Predicted Number of Deaths for Tomorrow: " + myFormat.format((int) fin2));

                    Log.d("TAG", "reached: " + seriesCases.getHighestValueY());
                    numCases.setVisibility(View.VISIBLE);
                    numCases.setText("Confirmed: " + myFormat.format((int) seriesCases.getHighestValueY()));

                    numDeaths.setVisibility(View.VISIBLE);
                    place.setVisibility(View.VISIBLE);

                    numCases.setVisibility(View.VISIBLE);
                    numCases.setText("Confirmed: " + myFormat.format((int) seriesCases.getHighestValueY()));

                    instruct.setVisibility(View.INVISIBLE);
                    numDeaths.setVisibility(View.VISIBLE);
                    place.setVisibility(View.VISIBLE);
                    numDeaths.setText("Deaths: " + myFormat.format(seriesDeaths.getHighestValueY()));
                    place.setText(coun);

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

                    saveData((int) seriesCases.getHighestValueY(), (int) seriesDeaths.getHighestValueY(), (int) fin, (int) fin2, place.getText().toString(), userCountry.getSelectedItemPosition(), ys, ys2);
                }
                else
                    instruct.setText("Not a valid state or county! Please check your information.");
            }catch (Exception e){
                Log.d("TAG", e.getMessage());
            }
        }
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
            slope = (long)(0.75*(nums.get(size) - nums.get(size - 1)) + 0.55*(nums.get(size - 1) - nums.get(size - 2)) + 0.35*(nums.get(size - 2) - nums.get(size - 3)) + 0.15*(nums.get(size - 3) - nums.get(size - 4)) + 0.15*(nums.get(size - 4) - nums.get(size - 5))) / 5;
            return slope;
        }
    }
    public void saveData(int casesC, int deathsC, int casesP, int deathsP, String place, int countryI, ArrayList<Double> seriesC, ArrayList<Double> seriesD) {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS_2, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Gson gson = new Gson();
        String jsonC = gson.toJson(seriesC);
        editor.putString(COUNTRY_CASE_SERIES, jsonC);

        String jsonD = gson.toJson(seriesD);
        editor.putString(COUNTRY_DEATH_SERIES, jsonD);

        editor.putInt(COUNTRY_PREDICT_DEATHS, deathsP);
        editor.putInt(COUNTRY_PREDICT_CASES, casesP);
        editor.putInt(COUNTRY_CURRENT_DEATHS, deathsC);
        editor.putInt(COUNTRY_CURRENT_CASES, casesC);

        editor.putString(COUNTRY_PLACE, place);
        editor.putInt(COUNTRY_USER_COUNTRY, countryI);

        editor.apply();
    }
    public void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS_2, MODE_PRIVATE);

        Gson gson = new Gson();
        String json = sharedPreferences.getString(COUNTRY_CASE_SERIES, null);
        String json2 = sharedPreferences.getString(COUNTRY_DEATH_SERIES, null);
        Type type = new TypeToken<ArrayList<Double>>() {}.getType();
        seriesC = gson.fromJson(json, type);
        seriesD = gson.fromJson(json2, type);

        savedPlace = sharedPreferences.getString(COUNTRY_PLACE, null);

        try {
            Log.d("TAG1", COUNTRY_USER_COUNTRY);
            int num = sharedPreferences.getInt(COUNTRY_USER_COUNTRY, 0);
            Log.d("TAG1", "" + num);
            savedCountry = num;
        }catch(Exception e){
            Log.d("TAG1", e.getMessage());
        }
        savedCasesC = sharedPreferences.getInt(COUNTRY_CURRENT_CASES, 0);
        savedDeathsC = sharedPreferences.getInt(COUNTRY_CURRENT_DEATHS, 0);
        savedCasesP = sharedPreferences.getInt(COUNTRY_PREDICT_CASES, 0);
        savedDeathsP = sharedPreferences.getInt(COUNTRY_PREDICT_DEATHS, 0);

        if (seriesC == null)
            seriesC = new ArrayList<>();
        if(seriesD == null)
            seriesD = new ArrayList<>();

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

        if(savedCountry != 0) {
            userCountry.setAdapter(adapter);
            userCountry.setVisibility(View.VISIBLE);
            userCountry.setSelection(savedCountry);
        }
        else {
            all = false;
            userCountry.setAdapter(adapter);
        }

        if((seriesD.size() != 0) && (seriesC.size() != 0)) {
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
