package com.akvilonsoft.emergencycall;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
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


public class MainActivity extends ActionBarActivity  {
    private static GoogleMap mMap;
    public static FragmentManager fragmentManager;
    private static Double latitude=0.0, longitude=0.0, longitudenew, latitudenew;
    private long begin, end;
    private Context context;
    private int counter = 0;
    private int interval = 5000;
    private Handler handler;
    private Handler guiHandler = new Handler();
    private MediaPlayer mPlayer;
    int zaehler = 0;
    Runnable statusChecker = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(statusChecker, interval);
            checkMoving();
        }
    };


    void stopRepeatingTask() {
        handler.removeCallbacks(statusChecker);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fragmentManager = getSupportFragmentManager();
        context = MainActivity.this;
        handler = new Handler();
        turnGPSOn();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRepeatingTask();
        if (mPlayer!= null && mPlayer.isPlaying()) mPlayer.stop();
    }


    private void turnGPSOn(){
        LocationManager lm = null;
        boolean gps_enabled = false,network_enabled = false;
        if(lm==null)
            lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try{
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch(Exception ex){}
        try{
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }catch(Exception ex){}

        if(!gps_enabled && !network_enabled){
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setMessage(context.getResources().getString(R.string.gps_network_not_enabled));
            dialog.setPositiveButton(context.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub
                    Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(myIntent);
                    //get gps
                }
            });
            dialog.setNegativeButton(context.getString(R.string.Cancel), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub

                }
            });
            dialog.show();

        }
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
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void goHome() {

        MapFragment mMapFragment = MapFragment.newInstance();
        FragmentTransaction fragmentTransaction =
                getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.scrollView, mMapFragment);
        LatLng latLng = new LatLng(latitude, longitude);

        if (mMap == null) {
            mMap = ((SupportMapFragment) MainActivity.fragmentManager
                    .findFragmentById(R.id.location_map)).getMap();
            if (mMap != null) {
                setUpMap();
            }
        }

        mMap.setMyLocationEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));

        mMap.addMarker(new MarkerOptions()
                .title("You are here")
                .snippet("Recent position")
                .position(latLng));
        mMap.setTrafficEnabled(true);
    }

    private static void setUpMap() {
        mMap.setMyLocationEnabled(true);
        mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("My Home").snippet("Home Address"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,
                longitude), 6.0f));
    }


    public void checkMoving() {
        SoundPool spool;
        Location location;
        LocationManager locationManager;
        locationManager = (LocationManager) this.getBaseContext().getSystemService(Context.LOCATION_SERVICE);
        Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        long GPSLocationTime = 0;
        if (null != locationGPS) {
            GPSLocationTime = locationGPS.getTime();
        }

        long NetLocationTime = 0;

        if (null != locationNet) {
            NetLocationTime = locationNet.getTime();
        }

        if (0 < GPSLocationTime - NetLocationTime) {
            location = locationGPS;
        } else {
            location = locationNet;
        }
        location = new Location("Home");
       location.setLatitude(50);
        location.setLongitude(8);
        if (location != null) {
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
                    Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate for 500 milliseconds
                    v.vibrate(500);
                    mPlayer = MediaPlayer.create(MainActivity.this, R.raw.schrei);
                    //mPlayer.setLooping(true);
                    mPlayer.start();
                    mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            zaehler++;
                            if (zaehler < 10) {
                                mPlayer.start();
                            } else {
                                mPlayer.stop();
                            }
                        }
                    });

                    AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                    dialog.setMessage(context.getResources().getString(R.string.question_disable));
                    dialog.setPositiveButton(context.getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            mPlayer.stop();
                            return;
                        }
                    });
                    dialog.setNegativeButton(context.getString(R.string.no), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            // TODO Auto-generated method stub

                        }
                    });
                    dialog.show();

               //     SystemClock.sleep(20000);

                    Geocoder geocoder = new Geocoder(context, Locale.GERMANY);
                    String street = "";
                    String routingAddress = "";
                    try {
                        List<Address> address = geocoder.getFromLocation(latitudenew, longitudenew, 1);
                        street = address.get(0).getLocality() + " \n" + address.get(0).getAddressLine(0);
                        routingAddress = address.get(0).getLocality() + "+" + address.get(0).getAddressLine(0).replace(" ", "+");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    final String finalStreet = street;
                    final String finalRoutingAddress = routingAddress;
                    guiHandler.postDelayed(new Runnable() {
                        public void run() {
                            SmsManager sms = SmsManager.getDefault();
                            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences( MainActivity.this);
                            String phone = sharedPref.getString("example_text", "");
                           // sms.sendTextMessage(phone, null, "Hilfe: " + finalStreet + "\n" + "http://maps.google.com/?q=" + latitudenew + "," + longitudenew + ",15z", null, null);
                            sms.sendTextMessage(phone, null, "Hilfe: " + finalStreet + "\n" + "https://www.google.de/maps/dir/" + finalRoutingAddress + ",+Deutschland/Neue+Gasse+7,+71149+Bondorf/,12z", null, null);
                            Intent phoneIntent = new Intent(Intent.ACTION_CALL);
                            phoneIntent.setData(Uri.parse("tel:" + phone));
                            AudioManager audioService = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                            audioService.setSpeakerphoneOn(true);
                            startActivity(phoneIntent);
                        }
                    }, 20000);

                    counter++;
                }
            } else {

            }
        }
    }


    public void startRepeatingTask(View view) {
        statusChecker.run();
    }
}
