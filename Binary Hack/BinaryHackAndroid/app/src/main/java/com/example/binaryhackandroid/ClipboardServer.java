package com.example.binaryhackandroid;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;
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
}