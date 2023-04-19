package com.revieve.PluginWebViewJavaSample;

import android.webkit.WebView;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.net.Uri;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class RevieveJSInterface {
    FullscreenActivity mActivity;
    WebView mWebview;

    /** Instantiate the interface and set the context */
    RevieveJSInterface(FullscreenActivity activity, WebView webview) {
        mActivity = activity;
        mWebview = webview;
    }

    // Messages from Revieve plugin arrive here
    @JavascriptInterface
    public void handleMessage(String body) {
        Log.d("REVIEVE_PLUGIN_DATA", body);
        try {
            JsonObject message = new Gson().fromJson(body, JsonObject.class);
            String type = message.get("type").getAsString();

            // callback message handling examples
            if ("onClose".equals(type)) {
                mActivity.moveTaskToBack(true);
            } else if ("onClickProduct".equals(type)) {
                JsonObject payload = message.get("payload").getAsJsonArray().get(0).getAsJsonObject();
                String url = payload.get("url").getAsString();
                String productId = payload.get("id").getAsString();
                String title = "User clicked product";
                String description = String.format("id: %s%nurl: %s", productId, url);
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mActivity.showAlert(title, description);
                    }
                });
            }
        } catch (JsonSyntaxException e) {
            // Handle JSON parsing exception
        }
    }

}
