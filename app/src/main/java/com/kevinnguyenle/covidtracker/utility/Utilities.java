package com.kevinnguyenle.covidtracker.utility;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.transition.Fade;
import android.view.Window;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;

public class Utilities {

    // Enables and sets transition on specified activity
    public static void setTransition(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            activity.getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);

            // Create a transition (Slide, Fade, Explode, Propagate..)
            android.transition.Transition transition = new Fade();

            transition.excludeTarget(android.R.id.navigationBarBackground, true);
            transition.excludeTarget(android.R.id.statusBarBackground, true);

            transition.setDuration(750);

            // Slide transition
            activity.getWindow().setEnterTransition(transition);
            activity.getWindow().setExitTransition(transition);
        }
    }

    // Loads the provinces GeoJSON stored in Assets folder
    public static String loadGeoJsonFromAsset(Context context) {
        try {
            // Load GeoJSON file from local asset folder
            InputStream is = context.getAssets().open("provinces.geojson");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    // Obtains date one day behind the current date
    public static Date yesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }

    // Get appropriate color based on provincial number of COVID-19 cases
    // Credit source of Colour-blind palette:  https://davidmathlogic.com/colorblind/
    public static int getColour(float totalCases, boolean colourAssistance) {
        int color;

        if (totalCases < 1000) {
            color = colourAssistance ?
                    Color.argb(255, 100, 143, 255) :
                    Color.argb(255, 147, 191, 0);
        }
        else if (totalCases < 10000) {
            color = colourAssistance ?
                    Color.argb(255, 120, 94, 240) :
                    Color.argb(255, 191, 166, 0);
        }
        else if (totalCases < 100000) {
            color = colourAssistance ?
                    Color.argb(255, 254, 97, 0) :
                    Color.argb(255, 191, 120, 0);
        }
        else {
            color = colourAssistance ?
                    Color.argb(255, 220, 38, 127) :
                    Color.argb(255, 191, 0, 0);
        }

        return color;
    }
}
