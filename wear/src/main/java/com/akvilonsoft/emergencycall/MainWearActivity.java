package com.akvilonsoft.emergencycall;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;


public class MainWearActivity extends Activity {
    private static GoogleMap mMap;
    public static FragmentManager fragmentManager;
    private static Double latitude=0.0, longitude=0.0, longitudenew, latitudenew;
    private long begin, end;
    private Context context;
    private int counter = 0;
    private int interval = 5000;
    private Handler handler;
    Runnable statusChecker = new Runnable() {
        @Override
        public void run() {
            //   updateStatus(); //this function can change value of mInterval.
            handler.postDelayed(statusChecker, interval);
            checkMoving();
        }
    };

    void startRepeatingTask() {
        statusChecker.run();
    }

    void stopRepeatingTask() {
        handler.removeCallbacks(statusChecker);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
  //      fragmentManager = getSupportFragmentManager();
        context = this.getBaseContext();
        handler = new Handler();
        turnGPSOn();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRepeatingTask();

    }


    private void turnGPSOn(){
        String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

        //    if(!provider.contains("gps")){ //if gps is disabled
        final Intent poke = new Intent();
        poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
        poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
        poke.setData(Uri.parse("3"));
        sendBroadcast(poke);
        //      }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
 //       getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
  //      if (id == R.id.action_settings) {
   //         return true;
   //     }

        return super.onOptionsItemSelected(item);
    }
    public void goHome() {
        //     latitude = 26.78;
        //     longitude = 72.56;
        MapFragment mMapFragment = MapFragment.newInstance();
        FragmentTransaction fragmentTransaction =
                getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.scrollView, mMapFragment);
        LatLng sydney = new LatLng(latitude, longitude);
        // LatLng sydney = new LatLng(48.520665, 8.833652);

        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) MainWearActivity.fragmentManager
                    .findFragmentById(R.id.location_map)).getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null)
                setUpMap();
        }

        mMap.setMyLocationEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 10));

        mMap.addMarker(new MarkerOptions()
                .title("My home")
                .snippet("The best home in Bondorf.")
                .position(sydney));
        mMap.setTrafficEnabled(true);
        //       fragmentTransaction.commit();
/*        new AlertDialog.Builder(this)
                .setTitle("Delete entry")
                .setMessage("Are you sure you want to delete this entry?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();*/
    }
    /**
     * This is where we can add markers or lines, add listeners or move the
     * camera.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap}
     * is not null.
     */

    /***** Sets up the map if it is possible to do so *****/
    public static void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) MainWearActivity.fragmentManager
                    .findFragmentById(R.id.location_map)).getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null)
                setUpMap();
        }
    }
    private static void setUpMap() {
        // For showing a move to my loction button
        mMap.setMyLocationEnabled(true);
        // For dropping a marker at a point on the Map
        mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("My Home").snippet("Home Address"));
        // For zooming automatically to the Dropped PIN Location
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,
                longitude), 6.0f));
    }

    public void getLocation(View v) {
        startRepeatingTask();
        // flag for GPS status
        //    Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
        //  intent.putExtra("enabled", true);
        //     sendBroadcast(intent);
        Location  myLocation;
        LocationManager locationManager;
        LocationListener locListener;
        locationManager = (LocationManager) this.getBaseContext().getSystemService(Context.LOCATION_SERVICE);
        locListener = new LocationListener() {
            @Override
            public void onStatusChanged(String provider, int status,
                                        Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }

            @Override
            public void onLocationChanged(Location location) {

            }
        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000L, 1f, locListener);
        //     locationManager.requestLocationUpdates(
        //             LocationManager.GPS_PROVIDER, 1000, 1, locListener);
        //      mobileLocation = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        //      locationManager = (LocationManager) this.getBaseContext()
        //              .getSystemService(LOCATION_SERVICE);
        //      Location location = locationManager.getLastKnownLocation("gps");

        boolean isGPSEnabled = true;
        //     turnGPSOn();
        //GPSTracker gps;
        // flag for network status
        boolean isNetworkEnabled = false;
        //gps = new GPSTracker(MainWearActivity.this);

        // Check if GPS enabled
 /*       if(gps.canGetLocation()) {

            latitude = gps.getLatitude();
            longitude = gps.getLongitude();
            goHome();
            // \n is for new line
   //         Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
        } else {
            // Can't get location.
            // GPS or network is not enabled.
            // Ask user to enable GPS/network in settings.
            gps.showSettingsAlert();

        }*/
    }

    public void checkMoving() {
        Location  location;
        LocationManager locationManager;
        locationManager = (LocationManager) this.getBaseContext().getSystemService(Context.LOCATION_SERVICE);
        Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        location = locationGPS;
        long GPSLocationTime = 0;
        if (null != locationGPS) { GPSLocationTime = locationGPS.getTime(); }

        long NetLocationTime = 0;

        if (null != locationNet) {
            NetLocationTime = locationNet.getTime();
        }

        if ( 0 < GPSLocationTime - NetLocationTime ) {
            location = locationGPS;
        }
        else {
            location = locationNet;
        }
        Toast.makeText(context, "Your Location is - \nLat: " + location.getLatitude() + "\nLong: " + location.getLongitude(), Toast.LENGTH_LONG).show();
        longitudenew = location.getLongitude();
        latitudenew = location.getLatitude();
        if (counter == 0) goHome();
        if (Math.abs(longitudenew - longitude) > 0.00010 || Math.abs(latitude - latitudenew) > 0.00010) {
            longitude = longitudenew;
            latitude = latitudenew;
            goHome();
            begin = System.currentTimeMillis();
            return;
        }
        if (System.currentTimeMillis() - begin > 20000) {
            if (counter == 0) {
                //     Intent i = new Intent(Intent.ACTION_SEND);
                //     i.putExtra("address","00491775820007");
                // Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                //   v.vibrate(500);
                Geocoder geocoder = new Geocoder(context, Locale.GERMANY);
                String street = "";
                String routingAdresse="";
                try {
                    List<Address> adresse = geocoder.getFromLocation(latitudenew, longitudenew, 1);
                    street = adresse.get(0).getLocality() + " \n" + adresse.get(0).getAddressLine(0);
                    routingAdresse = adresse.get(0).getLocality() + "+" + adresse.get(0).getAddressLine(0).replace(" ", "+");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                SmsManager sms = SmsManager.getDefault();
                //            sms.sendTextMessage("0049172676890", null, "Hilfe: " + street + "\n" +"http://maps.google.com/?q=" + latitudenew + "," +  longitudenew + ",15z", null, null);
                sms.sendTextMessage("0049172676890", null, "Hilfe: " + street + "\n" +"https://www.google.de/maps/dir/" + routingAdresse + ",+Deutschland/Neue+Gasse+7,+71149+Bondorf/,12z", null, null);
                //     i.putExtra("sms_body", "Hilfe: " + street);
                //      Uri uri = Uri.parse("content://media/external/images/media/1");
                //     i.putExtra(Intent.EXTRA_STREAM, uri);
                //      i.setType("image/bmp");
                //       startActivity(i);

                Intent phoneIntent = new Intent(Intent.ACTION_CALL);

                phoneIntent.setData(Uri.parse("tel:00491775820007"));
                AudioManager audioService = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                audioService.setSpeakerphoneOn(true);
                startActivity(phoneIntent);
                counter++;
            }
        }
        else {

        }
    }

}