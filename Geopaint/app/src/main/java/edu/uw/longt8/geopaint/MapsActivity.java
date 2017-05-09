package edu.uw.longt8.geopaint;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import static edu.uw.longt8.geopaint.R.id.pen;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "Map Activity";
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private ShareActionProvider mShareActionProvider;
    private static final int LOCATION_REQUEST_CODE = 0;
    private boolean penDown;
    private Polyline mPolyline;
    List<Polyline> lineShape;
    private int drawingColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        penDown = false;
        drawingColor = -1;
        lineShape = new ArrayList<Polyline>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mapFragment.setRetainInstance(true); //retain data even when rotate the screen
        mapFragment.setHasOptionsMenu(true); //show option menu at the action bar

        // Create Google API Client that tracks the location data
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.connect();
        super.onStop();
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

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case pen:
                Log.v(TAG, "Select Pen");
                togglePen(item);
                return true;
            case R.id.picker:
                Log.v(TAG, "Select Picker");
                setDrawingColor();
                return true;
            case R.id.save:
                Log.v(TAG, "Select Save");
//                saveDrawing();
                return true;
            case R.id.share:
                Log.v(TAG, "Select Share");
                //instantiate the ShareActionProvider for share
                mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
//                handleShareFile(null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setDrawingColor() {
        final Activity thisActivity = this;
        ColorPickerDialogBuilder
                .with(this)
                .setTitle("Choose color")
                .initialColor(-1)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(10)
                .setOnColorSelectedListener(new OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int selectedColor) {
                        Toast.makeText(thisActivity, "Color Selected: 0x" + Integer.toHexString(selectedColor), Toast.LENGTH_SHORT).show();
                        Log.v(TAG, "onColorSelected: 0x" + Integer.toHexString(selectedColor));
                    }
                })
                .setPositiveButton("ok", new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                        drawingColor = selectedColor;
                        Log.v(TAG, "drawingColor set to: 0x" + Integer.toHexString(selectedColor));
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .build()
                .show();

    }

    //Change the state of the pen, allow user to either draw or not draw on the map
    public void togglePen(MenuItem item){
        if(penDown){ // if the current pen is down, the click will make the pen up
            item.setIcon(R.drawable.ic_pen_up);
            Toast.makeText(this, "Drawing Stopped", Toast.LENGTH_SHORT).show();
            mPolyline = null; //reset the current polyline
        }else{ // the click will make the pen down
            item.setIcon(R.drawable.ic_pen_down);
            Toast.makeText(this, "Start Drawing", Toast.LENGTH_SHORT).show();
        }
        penDown = !penDown; //toggle the pen
}

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationRequest request = new LocationRequest();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(10000); //location updates desired interval
        request.setFastestInterval(5000); //fastest interval for location updates

        //Runtime permission check
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permissionCheck == PackageManager.PERMISSION_GRANTED){
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, this);
        }else{
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onConnected(null);
                }
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v(TAG, "Google API Connection Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.v(TAG, "Google API Connection Failed");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.v(TAG, "Location Changed");
        if(penDown){ //if the pen is down
            LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude()); //get the current lat/lng
            // initialize the mPolyline at the first place
            if(mPolyline == null){
                PolylineOptions lines = new PolylineOptions().color(drawingColor);
                mPolyline = mMap.addPolyline(lines);
                lineShape.add(mPolyline); //store the drawn lines
            }
            //add points to the current line
            List<LatLng> points = mPolyline.getPoints();
            points.add(newPoint);
            mPolyline.setPoints(points);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPoint, 17));
        }
    }
}
