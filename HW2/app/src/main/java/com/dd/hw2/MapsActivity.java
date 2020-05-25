package com.dd.hw2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapLongClickListener,
        SensorEventListener {

    private FloatingActionButton floatingRec;
    private FloatingActionButton floatingExit;

    private TextView accelerateText;

    private GoogleMap mMap;

    private final String MARKER_JSON_FILE = "marker.json";

    private List<LatLng> markersJson;

    private List<Marker> markerList;

    boolean animFlag = false;

    private int screenHeight;
    private ConstraintLayout mainContainer;

    private SensorManager sensorManager;
    private Sensor mSensor;
    private boolean sensorState = false;

    private long lastUpdate = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        markersJson = new ArrayList<>();
        markerList = new ArrayList<>();
        restoreMapMarker();

        floatingRec = (FloatingActionButton) findViewById(R.id.floatingActionButtonRec);
        floatingExit = (FloatingActionButton) findViewById(R.id.floatingActionButtonExit);
        floatingRec.setVisibility(View.INVISIBLE);
        floatingExit.setVisibility(View.INVISIBLE);
        floatingExit.setAlpha(0f);
        floatingRec.setAlpha(0f);
        accelerateText = findViewById(R.id.textView);
        accelerateText.setVisibility(View.INVISIBLE);



        mainContainer = findViewById(R.id.mainView);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }else{
            mSensor = null;
        }

        mainContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                screenHeight = mainContainer.getHeight();
                mainContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

    }

    @Override
    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        saveMarkersToJson();

    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    //Animation methods
    private void animationFloatingButtons(final boolean showButtons)  {

        ObjectAnimator floatingButRecAnim;
        ObjectAnimator floatingButExitAnim;
        if(showButtons){
            floatingButRecAnim = ObjectAnimator.ofFloat(floatingRec, "y", (float)screenHeight,screenHeight-(floatingRec.getHeight()/2)-floatingRec.getHeight());
            floatingButExitAnim = ObjectAnimator.ofFloat(floatingExit, "y", (float)screenHeight,screenHeight-(floatingRec.getHeight()/2)-floatingRec.getHeight());

        }else{
            floatingButRecAnim = ObjectAnimator.ofFloat(floatingRec, "y", screenHeight-(floatingRec.getHeight()/2)-floatingRec.getHeight(),(float)screenHeight);
            floatingButExitAnim = ObjectAnimator.ofFloat(floatingExit, "y",screenHeight-(floatingRec.getHeight()/2)-floatingRec.getHeight(), (float)screenHeight);

        }
        floatingButExitAnim.setDuration(800);
        floatingButRecAnim.setDuration(800);
        AnimatorSet animSet = new AnimatorSet();

        animSet.play(floatingButRecAnim).with(floatingButExitAnim);
        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if(!showButtons){
                    floatingRec.setVisibility(View.INVISIBLE);
                    floatingRec.setAlpha(0f);
                    floatingExit.setVisibility(View.INVISIBLE);
                    floatingExit.setAlpha(0f);
                }
            }
        });

        animSet.start();

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////


    private void saveMarkersToJson(){
        String listJson = new Gson().toJson(markersJson);
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(MARKER_JSON_FILE, MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            writer.write(listJson);
            writer.close();

        }catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void restoreMapMarker() {
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 10000;
        Gson gson = new Gson();
        String readJson;

        try{
            inputStream = openFileInput(MARKER_JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();
            while ((n = reader.read(buf)) >= 0) {

                String tmp = String.valueOf(buf);
                String substing = (n < DEFAULT_BUFFER_SIZE) ? tmp.substring(0, n) : tmp;
                builder.append(substing);
            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<LatLng>>() {
            }.getType();
            markersJson = gson.fromJson(readJson, collectionType);
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    public void viewMarkersFromList(){
        for(int i = 0; i < markersJson.size(); i++){
            LatLng m = markersJson.get(i);

            Marker mar = mMap.addMarker(new MarkerOptions().position(m).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            .alpha(0.8f).title(String.format("Position: (%.2f, %.2f)", m.latitude, m.longitude)));
            markerList.add(mar);
        }
    }

    public void zoomInClick(View v){
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View v){
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
    }

    @Override
    public void onMapLoaded() {
        viewMarkersFromList();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Marker marker = mMap.addMarker(new MarkerOptions()
            .position(new LatLng(latLng.latitude, latLng.longitude))
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            .alpha(0.8f)
            .title(String.format("Position: (%.2f, %.2f)", latLng.latitude, latLng.longitude)));
        markersJson.add(marker.getPosition());
        markerList.add(marker);
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        floatingRec.setVisibility(View.VISIBLE);
        floatingRec.setAlpha(1f);
        floatingExit.setVisibility(View.VISIBLE);
        floatingExit.setAlpha(1f);
        animationFloatingButtons(true);
        return false;
    }

    public void deleteAllMarkers(View view) {
        for(int i = 0; i < markerList.size();i++){
            Marker m = markerList.get(i);
            m.remove();
        }
        markerList.clear();
        markersJson.clear();
        String listJson = new Gson().toJson(markersJson);
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(MARKER_JSON_FILE, MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            writer.write(listJson);
            writer.close();

        }catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void onClickExit(View view) {
        animationFloatingButtons(false);

    }


    public void OnClickRec(View view) {
        if(mSensor != null){
            if(!sensorState){
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Acceleration:\nx: %d  y: %d", 0, 0));
                accelerateText.setVisibility(View.VISIBLE);
                sensorState = true;
            }else{
                accelerateText.setVisibility(View.INVISIBLE);
                sensorState = false;
            }
        }else{
            Toast.makeText(this,"Nie znaleziono Accelerometru", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Acceleration:\nx: %.4f  y: %.4f", event.values[0], event.values[1]));
        accelerateText.setText(sb.toString());

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
