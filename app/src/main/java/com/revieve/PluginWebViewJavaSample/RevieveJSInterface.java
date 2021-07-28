package com.revieve.PluginWebViewJavaSample;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;

public class RevieveJSInterface {
    Context mContext;

    /** Instantiate the interface and set the context */
    RevieveJSInterface(Context c) {
        mContext = c;
    }

    // Messages from Revieve plugin arrive here
    @JavascriptInterface
    public void handleMessage(String body) {
        Log.d("REVIEVE_PLUGIN_DATA", body);
    }

}
