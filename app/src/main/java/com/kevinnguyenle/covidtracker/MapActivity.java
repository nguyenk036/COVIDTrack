package com.kevinnguyenle.covidtracker;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.kevinnguyenle.covidtracker.databinding.ActivityMapBinding;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.Circle;
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager;
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.mapboxsdk.utils.BitmapUtils;
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
public class MapActivity extends AppCompatActivity implements PermissionsListener, View.OnClickListener {

    private static final String MAPBOX_MARKER = "marker";
    private MapView mapView;
    private CircleManager circleManager;
    private SymbolManager symbolManager;
    private MapboxMap mapboxMap;
    private ActivityMapBinding binding;
    private FirebaseUser user;
    private Symbol symbol;
    private boolean showLastLocation = false;

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTransition(this);

        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { binding.fabMenu.setVisibility(View.GONE); }

        binding.fabSaveLocation.setOnClickListener(this);
        binding.fabContactLocations.setOnClickListener(this);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapboxMap -> mapboxMap.setStyle(Style.DARK, style -> {

            this.mapboxMap = mapboxMap;

            addMarkerImageToStyle(style);

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
            symbolManager = new SymbolManager(mapView, mapboxMap, style);

            if(settings.getBoolean("currentLocation", false)) { enableLocationComponent(style); }
            if(user != null) { loadLastLocation(); }
            boolean colourAssist = settings.getBoolean("colour", false);

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

    // Enables current location - prompting user if permission not yet initialized
    @SuppressLint("MissingPermission")
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            LocationComponentOptions locationComponentOptions = LocationComponentOptions.builder(this)
                    .foregroundTintColor(R.color.mapboxBlueLight)
                    .backgroundTintColor(R.color.white)
                    .pulseEnabled(true)
                    .pulseSingleDuration(2500)
                    .pulseAlpha(0.5f)
                    .pulseMaxRadius(20)
                    .build();

            // Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

            // Activate with options
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, loadedMapStyle).locationComponentOptions(locationComponentOptions).build());

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.NORMAL);

        } else {
            PermissionsManager permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    // Loads the last saved location from the FirebaseDB for the current user
    private void loadLastLocation() {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("users/" + user.getUid()).child("LastLocation");

        db.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(MapActivity.this, "Unable to get contract tracing location from database.", Toast.LENGTH_LONG).show();
            } else {
                try {
                    DataSnapshot locationSnapshot = task.getResult();
                    double lat = (double) locationSnapshot.child("latitude").getValue();
                    double lng = (double) locationSnapshot.child("longitude").getValue();

                    SymbolOptions symbolOptions = new SymbolOptions()
                            .withLatLng(new LatLng(lat, lng))
                            .withIconImage(MAPBOX_MARKER)
                            .withIconColor(ColorUtils.colorToRgbaString(Color.RED))
                            .withIconSize(0.8f);

                    symbolManager.setIconAllowOverlap(true);
                    symbolManager.create(symbolOptions).setIconSize(0.0f);
                    this.symbol = symbolManager.create(symbolOptions);
                } catch(Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Unable to obtain contact tracing information from database.", Toast.LENGTH_LONG).show();
                }

            }
        });
    }

    private void addMarkerImageToStyle(Style style) {
        style.addImage(MAPBOX_MARKER, BitmapUtils.getBitmapFromDrawable(getResources()
                        .getDrawable(R.drawable.mapbox_marker_icon_default)), true);
    }

    /** Override Methods**/

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fabSaveLocation) {
            Location currentLocation = mapboxMap.getLocationComponent().getLastKnownLocation();
            DatabaseReference db = FirebaseDatabase.getInstance().getReference("users/" + user.getUid());
            db.child("LastLocation").setValue(currentLocation);

            Toast.makeText(MapActivity.this, "Current location saved to database for contract tracing.", Toast.LENGTH_LONG).show();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, MainActivity.class), ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
    }

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

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(this::enableLocationComponent);
        } else {
            Toast.makeText(this, "Location permissions denied.", Toast.LENGTH_LONG).show();
            finish();
        }
    }


}
