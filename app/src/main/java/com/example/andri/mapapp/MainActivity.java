package com.example.andri.mapapp;

import android.app.ActivityManager;
import android.app.Application;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.andri.mapapp.Preferences.SettingsActivity;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,PlaceSelectionListener {
    private GoogleMap m_googleMap;
    private EditText edtText;
    private ImageView imgView;
    private LocationManager manager;
    ServiceConnection sConn;
    Intent serviceIntent;

    private static final String LOG_TAG = "PlaceSelectionListener";
    private static final LatLngBounds BOUNDS_MOUNTAIN_VIEW = new LatLngBounds(
            new LatLng(37.398160, -122.180831), new LatLng(37.430610, -121.972090));
    private static final int REQUEST_SELECT_PLACE = 1000;
    private TextView locationTextView;
    private static final String API_KEY = "AIzaSyBYsiDbmlFjNIZGWshDLnW1oqUIfSu3OOc";
    private LatLng target_pos;
    NotificationService service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edtText =(EditText)findViewById(R.id.TextSearch);
        imgView = (ImageView)findViewById(R.id.ImageSearch);
        locationTextView =(TextView)findViewById(R.id.TextSearch);
        createMapView();
        edtText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new PlaceAutocomplete.IntentBuilder
                            (PlaceAutocomplete.MODE_OVERLAY)
                            .setBoundsBias(BOUNDS_MOUNTAIN_VIEW)
                            .build(MainActivity.this);
                    startActivityForResult(intent, REQUEST_SELECT_PLACE);
                } catch (GooglePlayServicesRepairableException |
                        GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });
        manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
        }
    }

    public void moveCamera(LatLng point) {
        CameraPosition target=new CameraPosition.Builder().target(point).zoom(15.5f)
                .bearing(0)
                .tilt(25)
                .build();

        changeCamera(CameraUpdateFactory.newCameraPosition(target), new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                Toast.makeText(getBaseContext(), "Animation to target complete", Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onCancel() { }
        });
    }

    public boolean isServiceRunning(String serviceClassName){
        final ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(serviceClassName)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPlaceSelected(Place place) {
        Log.i(LOG_TAG, "Place Selected: " + place.getName());
        locationTextView.setText(place.getName().toString());
        target_pos=place.getLatLng();
        moveCamera(new LatLng(target_pos.latitude,target_pos.longitude));
        addMarker(target_pos);
        if(isServiceRunning("NotificationService")) {
            new AlertDialog.Builder(this)
                    .setTitle("Навігація")
                    .setMessage("Ви хочете завершити розпочату навігацію?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            unbindService(sConn);
                            stopService(serviceIntent);
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {return;}
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        if(checkCurrentLocation())
            displayDialog();
    }

    public void changeCamera(CameraUpdate update, GoogleMap.CancelableCallback callback) {
        try {
            m_googleMap.animateCamera(update, callback);
            m_googleMap.moveCamera(update);
        }
        catch(Exception ex) {ex.printStackTrace();}
    }

    public void displayDialog(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.observation)
                .setMessage(R.string.ask_observe)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        sConn = new ServiceConnection() {
                            public void onServiceConnected(ComponentName name, IBinder binder) {
                                Log.d(LOG_TAG, "MainActivity onServiceConnected");
                            }

                            public void onServiceDisconnected(ComponentName name) {
                                Log.d(LOG_TAG, "MainActivity onServiceDisconnected");
                            }
                        };
                        Intent serviceIntent=new Intent(MainActivity.this, NotificationService.class);
                        serviceIntent.putExtra("Latitude",target_pos.latitude);
                        serviceIntent.putExtra("Longtitude",target_pos.longitude);
                        service=new NotificationService("service");
                        service.startService(serviceIntent);
                        service.bindService(serviceIntent,sConn,0);
                        showNotification("AppMap відстежує...",10);

                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {}
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }



    @Override
    public void onError(Status status) {
        Log.e(LOG_TAG, "onError: Status = " + status.toString());
        Toast.makeText(this, "Place selection failed: " + status.getStatusMessage(),
                Toast.LENGTH_SHORT).show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SELECT_PLACE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                this.onPlaceSelected(place);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                this.onError(status);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Initialises the mapview
     */
    public void createMapView(){
        try {
            if(null == m_googleMap){
                FragmentManager mFragmentManager = getSupportFragmentManager();
                SupportMapFragment mVTMapFragment =(SupportMapFragment)mFragmentManager.findFragmentById(R.id.mapView);
                mVTMapFragment.getMapAsync(this);
            }
        } catch (NullPointerException exception){
            Log.e("mapApp", exception.toString());
        }
    }

    /**
     * Adds a marker to the map
     */
    public void addMarker(LatLng coord){
        if(m_googleMap !=null) {
            m_googleMap.clear();
            m_googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(coord.latitude, coord.longitude))
                    .title("Marker")
                    .draggable(true)
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
        //return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(i);
                break;
        }
        return super.onOptionsItemSelected(item);
        //return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        m_googleMap=googleMap;
        setUpMap();
    }

    public void setUpMap(){
        m_googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        m_googleMap.setMyLocationEnabled(true);
        m_googleMap.setTrafficEnabled(true);
        m_googleMap.setIndoorEnabled(true);
        m_googleMap.setBuildingsEnabled(true);
        m_googleMap.getUiSettings().setZoomControlsEnabled(true);
        m_googleMap.getUiSettings().setCompassEnabled(false);
    }

    public boolean checkCurrentLocation() {
        Location currentLocation;
        Location target= new Location("target");
        target.setLatitude(target_pos.latitude);
        target.setLongitude(target_pos.longitude);
        currentLocation=m_googleMap.getMyLocation();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        float radius=sharedPrefs.getInt("seekBarPreference",100);
        float dis=currentLocation.distanceTo(target);
        if(dis<radius) {
            Toast.makeText(getApplicationContext(), R.string.Location_error,Toast.LENGTH_SHORT).show();
            return false;
        }
        else return true;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.GPS_ask_toEnable)
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void showNotification(String iconTitle, int notificationId) {

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(iconTitle);
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        Notification notification = mBuilder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR|Notification.FLAG_ONGOING_EVENT;

        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(notificationId, notification);
    }
}







