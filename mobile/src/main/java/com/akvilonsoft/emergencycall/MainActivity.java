package com.akvilonsoft.emergencycall;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class MainActivity extends ActionBarActivity {
    public static FragmentManager fragmentManager;
    private static GoogleMap mMap;
    private static Double latitude = 0.0, longitude = 0.0, longitudenew, latitudenew;
    int zaehler = 0;
    boolean makeCall = true;
    Locale current;
    long amountSec;
    private long begin;
    private Context context;
    private int counter = 0;
    private Handler handler;
    private Handler guiHandler = new Handler();
    private MediaPlayer mPlayer;
    Runnable statusChecker = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(statusChecker, 5000);
            checkMoving();
        }
    };

    private static void setUpMap() {
        mMap.setMyLocationEnabled(true);
        mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("My Home").snippet("Home Address"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,
                longitude), 6.0f));
    }

    public void stopRepeatingTask(View view) {
        handler.removeCallbacks(statusChecker);
        counter = 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        current = getResources().getConfiguration().locale;
        Locale.setDefault(current);
        Configuration conf = res.getConfiguration();
        conf.locale = current;
        res.updateConfiguration(conf, dm);
        setContentView(R.layout.activity_main);
        fragmentManager = getSupportFragmentManager();
        context = MainActivity.this;
        handler = new Handler();
        turnGPSOn();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        amountSec = Long.valueOf(sharedPref.getString("example_list", "10"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRepeatingTask(this.findViewById(android.R.id.content));
        if (mPlayer != null && mPlayer.isPlaying()) mPlayer.stop();
        setResult(0);
        QuitApplication();
    }

    private void turnGPSOn() {
        LocationManager lm;
        boolean gps_enabled = false, network_enabled = false;
        lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            AlertDialog.Builder dlgWarning = new AlertDialog.Builder(context);
            dlgWarning.setMessage(context.getResources().getString(R.string.gps_activate_error));
            dlgWarning.show();
        }

        if (!gps_enabled && !network_enabled) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setMessage(context.getResources().getString(R.string.gps_network_not_enabled));
            dialog.setPositiveButton(context.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(myIntent);
                }
            });
            dialog.setNegativeButton(context.getString(R.string.Cancel), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                }
            });
            dialog.show();
        }
        final Intent poke = new Intent();
        poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
        poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
        poke.setData(Uri.parse("3"));
        sendBroadcast(poke);
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
        //       fragmentTransaction.commit();
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
                .title(getString(R.string.position_title))
                .snippet(getString(R.string.actual_position))
                .position(latLng));
        mMap.setTrafficEnabled(true);
    }

    public void checkMoving() {
        Location location;
        Location locationGPS = null;
        Location locationNet = null;
        LocationManager locationManager = (LocationManager) this.getBaseContext().getSystemService(Context.LOCATION_SERVICE);
        try {
            locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException e) {

        }


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
//             location.setLatitude(50);
//             location.setLongitude(8);
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

            if ((System.currentTimeMillis() - begin > amountSec * 1000L) && (counter == 0)) {
                notifyWear();
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(5000);
                mPlayer = MediaPlayer.create(MainActivity.this, R.raw.schrei);
                mPlayer.start();
                mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        zaehler++;
                        if (zaehler < 20) mPlayer.start();
                        else {
                            mPlayer.stop();
                        }
                    }
                });

                AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                dialog.setMessage(context.getResources().getString(R.string.question_disable));
                dialog.setPositiveButton(context.getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        if (mPlayer.isPlaying()) mPlayer.stop();
                        stopRepeatingTask(getWindow().findViewById(android.R.id.content));
                        QuitApplication();
                    }
                });
                dialog.show();
                makeCall();
                counter++;
            }
        }
    }

    private void makeCall() {
        Geocoder geocoder = new Geocoder(context, Locale.GERMANY);

        String street = "";
        String routingAddress = "";
        try {
            List<Address> address = geocoder.getFromLocation(latitudenew, longitudenew, 1);
            if (!address.isEmpty()) {
                street = address.get(0).getLocality() + " \n" + address.get(0).getAddressLine(0);
                routingAddress = address.get(0).getLocality() + "+" + address.get(0).getAddressLine(0).replace(" ", "+");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        final String finalStreet = street;
        final String finalRoutingAddress = routingAddress;
        guiHandler.postDelayed(new Runnable() {
            public void run() {
                SmsManager sms = SmsManager.getDefault();
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                String phone = sharedPref.getString("example_text", "");
                sms.sendTextMessage(phone, null, "Hilfe: " + finalStreet + "\n" + "https://www.google.com/maps/dir/" + finalRoutingAddress + ",+Deutschland/Neue+Gasse+7,+71149+Bondorf/,12z", null, null);
                Intent phoneIntent = new Intent(Intent.ACTION_CALL);
                phoneIntent.setData(Uri.parse("tel:" + phone));
                AudioManager audioService = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                audioService.setSpeakerphoneOn(true);
                try {
                    startActivity(phoneIntent);
                } catch (SecurityException e) {

                }
            }
        }, 20000);
    }


    public void startRepeatingTask(View view) {
        begin = System.currentTimeMillis();
        counter = 0;
        statusChecker.run();
        makeCall = true;
    }

    private void notifyWear() {
        int notificationId = 1;

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(MainActivity.this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Emergency Call")
                        .setContentText("Emergency Call activated");

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(MainActivity.this);

        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    private void QuitApplication() {

        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);

    }
}
