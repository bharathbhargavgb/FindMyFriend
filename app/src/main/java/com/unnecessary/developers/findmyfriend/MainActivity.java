package com.unnecessary.developers.findmyfriend;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.net.wifi.WifiManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 1;
    private static boolean locationPermissionGranted = false;
    private static boolean foundScanResult = false;
    private static boolean isLatestResult = true;
    private static boolean isScanButtonClicked = false;

    TextView tvResultsView;
    EditText etSSID;
    EditText x_value, y_value;

    WifiManager wifi;
    WifiScanReceiver wifiReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long epochTime = System.currentTimeMillis();
                writeFileOnInternalStorage(String.valueOf(epochTime) + ".txt", tvResultsView.getText().toString());
                Snackbar.make(view, "Data in text view is saved to file in internal storage", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_ACCESS_FINE_LOCATION );
            Log.d("DEBUG", "Location permission not granted");
        } else {
            locationPermissionGranted = true;
            Log.d("DEBUG", "Location permission granted");
        }

        tvResultsView = findViewById(R.id.tvResults);
        tvResultsView.setMovementMethod(new ScrollingMovementMethod());

        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiReceiver = new WifiScanReceiver();

        Button scanButton = findViewById(R.id.btScan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if (locationPermissionGranted) {
                if (!foundScanResult) {
                    tvResultsView.setText("");
                }
                etSSID = findViewById(R.id.etSSID);
                foundScanResult = false;
                isScanButtonClicked = true;
                if (!wifi.startScan()) {
                    isLatestResult = false;
                    Toast.makeText(getApplicationContext(), "Unable to start a new scan", Toast.LENGTH_SHORT).show();
                } else {
                    isLatestResult = true;
                }
            } else {
                Log.d("DEBUG", "Location permission not granted");
            }
            }
        });

        Button resetButton = findViewById(R.id.btReset);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tvResultsView.setText("");
            }
        });
    }

    protected void onPause() {
        unregisterReceiver(wifiReceiver);
        super.onPause();
    }

    protected void onResume() {
        registerReceiver(
                wifiReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        );
        super.onResume();
    }

    public void writeFileOnInternalStorage(String fileName, String content) {
        File scanResultFile = new File(getExternalFilesDir("UnnecessaryStorage"), fileName);
        try {
            FileOutputStream fos = new FileOutputStream(scanResultFile);
            fos.write(content.getBytes());
            fos.close();

            Log.d("DEBUG", "Scan result file written to external storage: " + scanResultFile.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private class WifiScanReceiver extends BroadcastReceiver {

        public void onReceive(Context c, Intent intent) {
            StringBuilder availableSSIDs = new StringBuilder();


            if (isScanButtonClicked) {
                isScanButtonClicked = false;
                List<ScanResult> wifiScanResultList = wifi.getScanResults();
                Log.d("DEBUG", "WifiScanReceiver " + wifiScanResultList.size());
                for (ScanResult result : wifiScanResultList) {
                    int signalLevel = result.level;
                    String ssid = result.SSID;
                    Log.d("DEBUG", "wifiScanResultList " + ssid);
                    String bssid = result.BSSID;
                    String timeStamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
                    String oldResult = (isLatestResult) ? "" : "\t(old result)";

                    if (ssid.equals(etSSID.getText().toString())) {
                        x_value = findViewById(R.id.etXvalue);
                        y_value = findViewById(R.id.etYvalue);
                        tvResultsView.append(timeStamp + "\t" + bssid + "\t" +
                                x_value.getText().toString() + "," + y_value.getText().toString() + "\t"
                                + String.valueOf(signalLevel) + oldResult + "\n");
                        foundScanResult = true;
                    } else {
                        availableSSIDs.append(ssid);
                        availableSSIDs.append("\n");
                    }
                }
                if (!foundScanResult) {
                    Toast.makeText(getApplicationContext(), "No such SSID exists. Available SSID displayed", Toast.LENGTH_SHORT).show();
                    tvResultsView.append(availableSSIDs.toString());
                }
                tvResultsView.append("\n");
            }
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
