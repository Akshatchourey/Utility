package com.example.binaryhackandroid;

import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private boolean isServerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView ipTextView = findViewById(R.id.ipTextView);
        Button toggleButton = findViewById(R.id.toggleButton);

        ipTextView.setText("IP: " + getLocalIpAddress() + ":8080");

        toggleButton.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, ClipboardService.class);
            if (!isServerRunning) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                toggleButton.setText("STOP SERVER");
                isServerRunning = true;
            } else {
                stopService(serviceIntent);
                toggleButton.setText("START SERVER");
                isServerRunning = false;
            }
        });
    }

    private String getLocalIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        if (ipAddress == 0) return "Check Network";
        return String.format(Locale.getDefault(), "%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }
}