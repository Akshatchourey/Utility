package com.example.binaryhackandroid;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;

public class ClipboardService extends Service {

    private ClipboardServer server;
    private PowerManager.WakeLock wakeLock;
    private Handler heartbeatHandler;
    private static final long HEARTBEAT_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes

    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            // No-op: keeps the service active in Android's eyes
            heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        server = new ClipboardServer(8080, this);

        // Acquire partial WakeLock to prevent CPU from sleeping
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BinaryHack::ServerLock");
        wakeLock.acquire();

        // Start heartbeat keep-alive
        heartbeatHandler = new Handler(Looper.getMainLooper());
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, "BinaryHackChannel")
                .setContentTitle("Binary Hack Server")
                .setContentText("Server is running on port 8080")
                .setSmallIcon(android.R.drawable.ic_menu_save)
                .setContentIntent(pendingIntent)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(1, notification);
        }

        try {
            if (server != null && !server.isAlive()) {
                server.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop heartbeat
        if (heartbeatHandler != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
        }

        // Release WakeLock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        // Stop server
        if (server != null) {
            server.stop();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "BinaryHackChannel",
                    "Binary Hack Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}