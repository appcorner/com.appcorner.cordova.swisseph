package com.appcorner.plugin;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;

import swisseph.SweConst;
import swisseph.SweDate;
import swisseph.SwissEph;

import java.io.File;
import java.util.Locale;

public class SwissEphHandler extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        if (action.equals("greet")) {

            String name = data.getString(0);
            String message = "Hello, " + name;
            callbackContext.success(message);

            return true;

        } else if (action.equals("computeChart")) {

			String ephePath = data.getString(0);
			String message = computeChart(ephePath);
			callbackContext.success(message);

            return true;

        } else {
            
            return false;

        }
    }

	private String computeChart(String ephePath) {
        // Input data:
        int year = 2015;
        int month = 9;
        int day = 17;
        double longitude = 100.523186;    // BKK, TH
        double latitude = 13.736717;
        double hour = 12 + 30. / 60. - 7.0; // GMT+7
        String printString;

		/*Instances of utility classes */
        SwissEph sw = new SwissEph(ephePath);
        SweDate sd = new SweDate(year, month, day, hour);

        // Set sidereal mode:
        sw.swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0, 0);

        // Print input details:
        printString = getDateinfo(sd);
        printString += getLocationinfo(longitude, latitude);

        //////////////////////////////////////////////
        // Output ayanamsa value:
        //////////////////////////////////////////////
        printString += "\n" + getAyanamsainfo(sw, sd);

        //////////////////////////////////////////////
        // Output lagna:
        //////////////////////////////////////////////
        printString += getLagnainfo(sw, sd, longitude, latitude);

        //////////////////////////////////////////////
        // Calculate all planets:
        //////////////////////////////////////////////
        printString += "\n" + getAllplanets(sw, sd);

        return printString;
    }

    private String getAllplanets(SwissEph sw, SweDate sd) {
        double[] xp = new double[6];
        StringBuffer serr = new StringBuffer();
        String s = "";

        int[] planets = { SweConst.SE_SUN,
                SweConst.SE_MOON,
                SweConst.SE_MARS,
                SweConst.SE_MERCURY,
                SweConst.SE_JUPITER,
                SweConst.SE_VENUS,
                SweConst.SE_SATURN,
                SweConst.SE_TRUE_NODE };	// Some systems prefer SE_MEAN_NODE

        int flags = SweConst.SEFLG_SWIEPH |        // slow and least accurate calculation method
                SweConst.SEFLG_SIDEREAL |    // sidereal zodiac
                SweConst.SEFLG_NONUT |        // will be set automatically for sidereal calculations, if not set here
                SweConst.SEFLG_SPEED;        // to determine retrograde vs. direct motion
        boolean retrograde = false;

        for(int p = 0; p < planets.length; p++) {
            int planet = planets[p];
            String planetName = sw.swe_get_planet_name(planet);
            int ret = sw.swe_calc_ut(sd.getJulDay(),
                    planet,
                    flags,
                    xp,
                    serr);

            if (ret != flags) {
                if (serr.length() > 0) {
                    System.err.println("Warning: " + serr);
                } else {
                    System.err.println(
                            String.format("Warning, different flags used (0x%x)", ret));
                }
            }

            retrograde = (xp[3] < 0);

            s += String.format("%-9s %s %c\n",
                    planetName, toDMS(xp[0]), (retrograde ? 'R' : 'D'));
        }
        // KETU
        xp[0] = (xp[0] + 180.0) % 360;
        String planetName = "Ketu";

        s += String.format("%-9s %s %c\n",
                planetName, toDMS(xp[0]), (retrograde ? 'R' : 'D'));
        return s;
    }

    private String getAyanamsainfo(SwissEph sw, SweDate sd) {
        double ayanamsa = sw.swe_get_ayanamsa_ut(sd.getJulDay());
        return "Ayanamsa  " + toDMS(ayanamsa) + "\n";
    }

    private String getLocationinfo(double longitude, double latitude) {
        return "\nLocation  " +
                toDMS(longitude) + (longitude > 0 ? "E" : "W") +
                "\n          " +
                toDMS(latitude) + (latitude > 0 ? "N" : "S");
    }

    private String getDateinfo(SweDate sd) {
        double hour = sd.getHour() + 0.5/3600.;
        int min = (int)((hour - (int)hour) * 60);
        int sec = (int)(((hour - (int)hour) * 60 - min) * 60);
        return String.format(Locale.US, "Date: %4d-%02d-%02d, %d:%02d:%02dh UTC / %.8fh\nJul.day:    %.6f\nDelta t:  %s\n",
                sd.getYear(),
                sd.getMonth(),
                sd.getDay(),
                (int)hour,
                min,
                sec,
                hour,
                sd.getJulDay(),
                toDMS(sd.getDeltaT()));
    }

    private String getLagnainfo(SwissEph sw, SweDate sd, double longitude, double latitude) {
        int flags = SweConst.SEFLG_SIDEREAL;
        double[] cusps = new double[13];
        double[] acsc = new double[10];

        int result = sw.swe_houses(sd.getJulDay(),
                flags,
                latitude,
                longitude,
                'P',
                cusps,
                acsc);

        return "Ascendant " + toDMS(acsc[0]) + "\n";
    }

    static String toDMS(double d) {
        d += 0.5/3600./10000.;	// round to 1/1000 of a second
        int deg = (int) d;
        d = (d - deg) * 60;
        int min = (int) d;
        d = (d - min) * 60;
        double sec = d;

        return String.format("%3d\u00b0 %02d' %07.4f\"", deg, min, sec);
    }
}