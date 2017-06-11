package com.example.user.final_exam;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.content.Intent;
import android.widget.ToggleButton;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.jar.Manifest;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Toast;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {

    public GoogleMap mGoogleMap;
    // GPSTracker class
    Button btnShowLocation;
    Button btnclearLocation;
    EditText editText;

    // GPSTracker class
    GPSTracker gps = null;
    GPSTracker new_gps = null;

    public Handler mHandler;

    public static int RENEW_GPS = 1;
    public static int SEND_PRINT = 2;

    private PolylineOptions Options;
    private ArrayList<LatLng> arrayPoints;
    private ArrayList<LatLng> arrayReal;


    private static int j=2;

    //------근접 센서 변수
    private SensorManager mSensorManager;
    private Sensor mProximity;

    //------
    private WindowManager.LayoutParams params;
    private float brightness;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


//센서 선언
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

//
        params = getWindow().getAttributes();




        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.ACCESS_FINE_LOCATION  },
                    0 );
        }



        editText = (EditText) findViewById(R.id.editText);
        btnShowLocation = (Button) findViewById(R.id.btnShowLocation);
        btnclearLocation = (Button) findViewById(R.id.btnclearLocation);

        // 맵 마커 연결 하는 줄 및 배열 셋팅
        Options = new PolylineOptions();
        Options.color(Color.RED);
        Options.width(5);
        arrayPoints = new ArrayList<>();


        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                if(msg.what==RENEW_GPS){
                    makeNewGpsService();
                }
                if(msg.what==SEND_PRINT){
                    logPrint((String)msg.obj);
                }
            }
        };


        // show location button click event
        btnShowLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {



                // create class object
                if(gps == null) {
                    gps = new GPSTracker(MapsActivity.this,mHandler);

                }else{
                    gps.Update();
                }

                // check if GPS enabled
                if(gps.canGetLocation()){
                    double latitude = gps.getLatitude();
                    double longitude = gps.getLongitude();
                    // \n is for new line
                    Toast.makeText(getApplicationContext(), "현재 위치는 - \n경도: " + latitude + "\n위도: " + longitude + "입니다.", Toast.LENGTH_LONG).show();



                    LatLng MyLocation = new LatLng(latitude,longitude );
                    mGoogleMap.addMarker(new MarkerOptions().position(MyLocation).title("1번째"+getTimeStr()));
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(MyLocation));

                    CameraUpdate zoom = CameraUpdateFactory.zoomTo(18);
                    mGoogleMap.animateCamera(zoom);

                    arrayPoints.add(MyLocation);
                    Options.addAll(arrayPoints);


                }else{
                    // can't get location
                    // GPS or Network is not enabled
                    // Ask user to enable GPS/network in settings
                    gps.showSettingsAlert();
                }


            }
        });

        btnclearLocation.setOnClickListener(new View.OnClickListener() {// 스탑 버튼 메소드
            @Override
            public void onClick(View arg0){
                //mGoogleMap.clear();
                //arrayPoints.clear();
                gps.stopUsingGPS();
                //new_gps.stopUsingGPS();
            }
        });

    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        LatLng Seoul = new LatLng(37.555744, 126.970431);
        //mGoogleMap.addMarker(new MarkerOptions().position(Seoul).title("Marker in Seoul"));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(Seoul));


        CameraUpdate zoom = CameraUpdateFactory.zoomTo(18);
        mGoogleMap.animateCamera(zoom);




        // 현재 위치를 받을수 있는 코드
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            mGoogleMap.setMyLocationEnabled(true);
        } else {
            // Show rationale and request permission.
        }


        // marker 표시
        // market 의 위치, 타이틀, 짧은설명 추가 가능.
       /* MarkerOptions marker = new MarkerOptions();
        marker .position(new LatLng(37.555744, 126.970431))
                .title("서울역")
                .snippet("Seoul Station");
        googleMap.addMarker(marker).showInfoWindow();*/ // 마커추가,화면에출력




    }

    public void makeNewGpsService() {
        if (gps == null) {
            gps = new GPSTracker(MapsActivity.this, mHandler);
        } else {
            gps.Update();
        }

    }

    public void logPrint(String str){
        editText.append(getTimeStr()+" "+str+"\n");


            gps.Update();

        // check if GPS enabled
        if(gps.canGetLocation()){

            LatLng MyLocation1 = new LatLng(gps.getLatitude(), gps.getLongitude());
            mGoogleMap.addMarker(new MarkerOptions().position(MyLocation1).title(Integer.toString(j)+"번째 "+getTimeStr()));
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(MyLocation1));
            j++;

            //맵 라인 그리기
            arrayPoints.add(MyLocation1);
            Options.addAll(arrayPoints);
            mGoogleMap.addPolyline(Options);
            arrayPoints.clear();




        }else{
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }


    }

    public String getTimeStr() {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdfNow = new SimpleDateFormat("MM/dd HH:mm:ss");
        return sdfNow.format(date);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float distance = event.values[0];
        // Do something with this sensor data.


            if(distance==0) {
                // 화면 밝기 설정
                params.screenBrightness = 0f;
                // 밝기 설정 적용
                getWindow().setAttributes(params);
            }
        else{
                // 기존 밝기로 변경
                params.screenBrightness = brightness;
                getWindow().setAttributes(params);
            }

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();
        // 기존 밝기 저장
        brightness = params.screenBrightness;


        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
    }
    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();


        mSensorManager.unregisterListener(this);
    }
}
