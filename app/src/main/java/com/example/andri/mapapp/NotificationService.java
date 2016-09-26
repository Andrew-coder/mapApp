package com.example.andri.mapapp;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;


public class NotificationService extends IntentService implements LocationListener {
    private Location target;
    SharedPreferences sharedPrefs;
    int radius;
    public NotificationService() {super("NotificationService");}

    /*public NotificationService(String name) {
        super(name);
    }*/

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras=intent.getExtras();
        target = new Location("target");
        target.setLatitude(extras.getDouble("Latitude"));
        target.setLongitude(extras.getDouble("Longtitude"));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        radius=sharedPrefs.getInt("seekBarPreference",100);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        double distance = target.distanceTo(location);
        if(Math.abs(distance)<radius) {
            long mills = 3000L;
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(mills);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


}
