package com.example.binaryhackandroid;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class ClipboardServer extends NanoHTTPD {

    private final Context context;
    private final ClipboardManager clipboardManager;

    public ClipboardServer(int port, Context context) {
        super(port);
        this.context = context;
        this.clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        if (Method.POST.equals(method) && "/set-clipboard".equals(uri)) {
            return handleSetClipboard(session);
        } else if (Method.GET.equals(method) && "/get-clipboard".equals(uri)) {
            return handleGetClipboard();
        } else if (Method.POST.equals(method) && "/set-image".equals(uri)) {
            return handleSetImage(session);
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
    }

    private Response handleSetClipboard(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            String postData = files.get("postData");

            JSONObject json = new JSONObject(postData);
            String text = json.getString("text");

            new Handler(Looper.getMainLooper()).post(() -> {
                ClipData clip = ClipData.newPlainText("PC Sync", text);
                clipboardManager.setPrimaryClip(clip);
            });

            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"success\"}");
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error");
        }
    }

    private Response handleGetClipboard() {
        final String[] copiedText = {""};

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = () -> {
            if (clipboardManager.hasPrimaryClip() && clipboardManager.getPrimaryClip().getItemCount() > 0) {
                CharSequence text = clipboardManager.getPrimaryClip().getItemAt(0).getText();
                if (text != null) {
                    copiedText[0] = text.toString();
                }
            }
        };

        handler.post(runnable);

        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {
        }

        String safeText = copiedText[0].replace("\"", "\\\"").replace("\n", "\\n");
        String jsonResponse = "{\"text\":\"" + safeText + "\"}";

        return newFixedLengthResponse(Response.Status.OK, "application/json", jsonResponse);
    }

    private Response handleSetImage(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);

            // NanoHTTPD saves uploaded files to temp paths
            String tempFilePath = files.get("file");
            if (tempFilePath == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "No file uploaded");
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String filename = "screenshot_" + timestamp + ".png";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ : Use MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BinaryHack");

                Uri imageUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (imageUri == null) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Failed to create MediaStore entry");
                }

                try (InputStream in = new FileInputStream(tempFilePath);
                     OutputStream out = context.getContentResolver().openOutputStream(imageUri)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            } else {
                // Android 9 and below : Direct file write
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "BinaryHack");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File outFile = new File(dir, filename);

                try (InputStream in = new FileInputStream(tempFilePath);
                     OutputStream out = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                // Make the image visible in Gallery
                MediaScannerConnection.scanFile(context, new String[]{outFile.getAbsolutePath()}, new String[]{"image/png"}, null);
            }

            String jsonResponse = "{\"status\":\"success\",\"filename\":\"" + filename + "\"}";
            return newFixedLengthResponse(Response.Status.OK, "application/json", jsonResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error saving image: " + e.getMessage());
        }
    }
}