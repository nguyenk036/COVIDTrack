package com.kevinnguyenle.covidtracker;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager;
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.mapboxsdk.utils.ColorUtils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.kevinnguyenle.covidtracker.utility.Utilities.getColour;
import static com.kevinnguyenle.covidtracker.utility.Utilities.loadGeoJsonFromAsset;
import static com.kevinnguyenle.covidtracker.utility.Utilities.setTransition;
import static com.kevinnguyenle.covidtracker.utility.Utilities.yesterday;

/**
 * MapActivity - Loads the Mapbox with GeoJSON data containing CircleOptions of provinces and their
 *               COVID statistical data
 */
public class MapActivity extends AppCompatActivity {

    private MapView mapView;
    private CircleManager circleManager;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTransition(this);

        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_map);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapboxMap -> mapboxMap.setStyle(Style.DARK, style -> {

            List<CircleOptions> circleOptionsList = new ArrayList<>();
            List<JsonObject> provincesList = null;
            OkHttpClient okHttpClient = new OkHttpClient();
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

            // Load local GeoJson, parse into a FeatureCollection and add as source to map
            String geoJson = loadGeoJsonFromAsset(this);
            FeatureCollection featureCollection = FeatureCollection.fromJson(geoJson);
            Source source = new GeoJsonSource("provinces", featureCollection);
            style.addSource(source);

            // Adjust starting camera position
            CameraPosition position = new CameraPosition.Builder()
                                            .target(new LatLng(60.30661950835823, -107.0507817760034))
                                            .zoom(2)
                                            .build();
            mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));

            circleManager = new CircleManager(mapView, mapboxMap, style);

            // For each circle that is clicked..
            circleManager.addClickListener(circle -> {
                // Obtain the data stored in the circle
                JsonObject circleData = circle.getData().getAsJsonObject();
                JsonObject data = circleData.getAsJsonArray("data").get(0).getAsJsonObject();

                Bundle bundle = new Bundle();

                // Extract individual strings and values from stored circle data
                String province = circleData.get("name").getAsString();
                String date = "Last updated on " + data.get("date");
                String totalCases = data.get("total_cases").getAsString();
                String totalDeaths = data.get("total_fatalities").getAsString();
                String totalRecoveries = data.get("total_recoveries").getAsString();
                String totalVaccinated = data.get("total_vaccinations").getAsString();
                float population = circleData.get("population").getAsFloat();
                float percent = Math.round(((float) Long.parseLong(totalVaccinated) / population) * 100);
                String percentageVaccinated = province + " is " + percent + "% vaccinated.";

                // Append data onto bundle
                bundle.putString("province_name", province);
                bundle.putString("date", date);
                bundle.putString("cases", totalCases);
                bundle.putString("deaths", totalDeaths);
                bundle.putString("recoveries", totalRecoveries);
                bundle.putString("vaccinated", totalVaccinated);
                bundle.putString("percentage", percentageVaccinated);
                bundle.putFloat("percent", percent);

                // Show BottomSheetDialog with bundled data
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog();
                bottomSheetDialog.setArguments(bundle);
                bottomSheetDialog.show(getSupportFragmentManager(), "ModalBottomSheet");

                return false;
            });

            // First API call with provincial population, name
            Request provinces = new Request.Builder().url("https://api.covid19tracker.ca/provinces").build();
            CallbackFuture futureProvinces = new CallbackFuture();
            okHttpClient.newCall(provinces).enqueue(futureProvinces);

            try {
                Response response = futureProvinces.get();
                Gson gson = new Gson();
                Type listType = new TypeToken<List<JsonObject>>() {}.getType();
                provincesList = gson.fromJson(response.body().string(), listType);
            } catch (InterruptedException | ExecutionException | IOException e) {
                e.printStackTrace();
            }

            // Loop FeatureCollection of provinces, run API call to obtain data for each province
            for (Feature feature : featureCollection.features()) {

                // Build the appropriate API Url with date and province code stored in FeatureCollection
                String code = feature.getStringProperty("code");
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.CANADA).format(yesterday());
                String url = "https://api.covid19tracker.ca/reports/province/" + code + "?date=" + today;
                Request request = new Request.Builder().url(url).build();

                // Instantiate a JsonElement variable that will store the data returned from API
                final JsonElement[] covidJson = new JsonElement[1];

                // CallbackFuture class waits for the API data to return - to prevent map loading
                // when API calls may not have been completed
                CallbackFuture future = new CallbackFuture();
                okHttpClient.newCall(request).enqueue(future);

                // Obtain the provinces JSON data from provinces url
                try {
                    Response response = future.get();
                    Gson gson = new Gson();
                    JsonObject jsonObject = gson.fromJson(response.body().string(), JsonObject.class);

                    // Loop through and append province name and population as the next API call will not have this data
                    for(JsonObject j : provincesList) {
                        if (j.get("code").getAsString().equals(code)) {
                            jsonObject.addProperty("name", j.get("name").getAsString());
                            jsonObject.addProperty("population", j.get("population").getAsFloat());
                        }
                    }

                    covidJson[0] = jsonObject;
                } catch(Exception e) {
                    e.printStackTrace();
                }

                // Obtain values from the returned JSON object to determine circle size and colour
                JsonObject outerJsonObject = covidJson[0].getAsJsonObject();
                JsonObject innerJsonObject = covidJson[0].getAsJsonObject().getAsJsonArray("data").get(0).getAsJsonObject();
                boolean colourAssist = settings.getBoolean("colour", false);
                float totalCases = innerJsonObject.get("total_cases").getAsFloat();
                float radius = 15 + ((innerJsonObject.get("total_cases").getAsFloat() / outerJsonObject.get("population").getAsFloat()) * 500);
                int strokeColor = Color.argb(255, 10, 10, 10);

                // Create the circle on map
                if (feature.geometry() instanceof Point) {
                    circleOptionsList.add(new CircleOptions()
                            .withGeometry((Point) feature.geometry())
                            .withCircleColor(ColorUtils.colorToRgbaString(getColour(totalCases, colourAssist)))
                            .withCircleStrokeColor(ColorUtils.colorToRgbaString(strokeColor))
                            .withCircleStrokeWidth(1.3f)
                            .withCircleOpacity(0.5f)
                            .withCircleRadius(radius)
                            .withData(covidJson[0]));
                }
            }

            circleManager.create(circleOptionsList);
        }));
    }

    /** Override Methods**/

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

}