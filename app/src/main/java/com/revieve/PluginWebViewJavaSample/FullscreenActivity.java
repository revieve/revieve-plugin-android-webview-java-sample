package com.revieve.PluginWebViewJavaSample;

import android.Manifest;
import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.service.autofill.OnClickAction;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebMessage;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.graphics.Bitmap;
import com.revieve.PluginWebViewJavaSample.databinding.ActivityFullscreenBinding;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    private ValueCallback<Uri[]> mFilePathCallback;
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private WebView myWebView;

    // Select which Revieve API environment to use. Can be test or prod
    static final String REVIEVE_ENV = "test";
    // your partner Id provided by Revieve
    static final String REVIEVE_PARTNER_ID = "9KpsLizwYK"; // skincare demo
    // static final String REVIEVE_PARTNER_ID = "GHru81v4aU"; // vto makeup demo
    // default locale
    static final String REVIEVE_LOCALE = "en";

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (AUTO_HIDE) {
                        delayedHide(AUTO_HIDE_DELAY_MILLIS);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    break;
                default:
                    break;
            }
            return false;
        }
    };
    /**
     * Example of native -> Revieve webview communication
     */
    private final View.OnClickListener mDummyButtonOnClickListener = new View.OnClickListener() {
        private void sendTryonProductCommand(String productId) {
            final String jsCommand = String.format("window.Revieve.API.liveAR.addTryOnProduct('%s');", productId);
            // to disable tryOn effects you can use "window.Revieve.API.liveAR.resetTryOnProducts();";
            Log.d("REVIEVE_PLUGIN_API", jsCommand);
            myWebView.evaluateJavascript(jsCommand, null);
        }

        @Override
        public void onClick(View view) {
            final String productId = "613705";
            sendTryonProductCommand(productId);
            
        }
    };
    
    private ActivityFullscreenBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityFullscreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mVisible = true;
        mControlsView = binding.fullscreenContentControls;
        mContentView = binding.fullscreenContent;

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        binding.dummyButton.setOnTouchListener(mDelayHideTouchListener);
        binding.dummyButton.setOnClickListener(mDummyButtonOnClickListener);

        // Initialize WebView and set onShowFileChooser listener
        WebView webView = findViewById(R.id.revieveWebview);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // Handle error
            }
        });
    }

    private String getHtmlWithConfig(String filename) {
        try {
            // Read HTML content from assets
            InputStream is = getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String html = new String(buffer, "UTF-8");

            // inject config vars
            html = html.replace("%REVIEVE_PARTNER_ID%", REVIEVE_PARTNER_ID)
            .replace("%REVIEVE_LOCALE%", REVIEVE_LOCALE)
            .replace("%REVIEVE_ENV%", REVIEVE_ENV);

            // Load modified HTML content into WebView
            return html;
        } catch (IOException e) {
            e.printStackTrace();
            // Handle error
            return "";
        }
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);


        // Revieve webview example:
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);


        // Load it
        myWebView = (WebView) findViewById(R.id.revieveWebview);

        myWebView.setWebChromeClient(new WebChromeClient() {
            private Boolean listenerCalled = false;
            
            @Override
            public Bitmap getDefaultVideoPoster() {
                // disables the default video tag poster image (grey play button)
                // https://stackoverflow.com/questions/35220624/android-webview-gray-play-button
                return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("REVIEVE_WEBVIEW_CONSOLE", String.valueOf(consoleMessage));
                return super.onConsoleMessage(consoleMessage);
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                Log.d("REVIEVE_WEBVIEW", "onPermissionRequest: " + Arrays.toString(request.getResources()));

                request.grant(request.getResources());

                // TODO: If you want the Revieve camera to open inside the webview, request the camera permissions from
                // the user and then grant it to the webview as described here:
                // https://github.com/googlesamples/android-PermissionRequest
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                // Save callback
                mFilePathCallback = filePathCallback;

                // Create intent to open file chooser for image selection
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");

                // Launch file chooser
                Intent chooserIntent = Intent.createChooser(intent, "Choose Image");
                startActivityForResult(chooserIntent, 44745);
                return true;
            }
        });

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        final String html = getHtmlWithConfig("revieve.html");
        myWebView.addJavascriptInterface(new RevieveJSInterface(this, myWebView), "Android");
        myWebView.loadDataWithBaseURL("https://d38knilzwtuys1.cloudfront.net/revieve-plugin-v4/app.html", html, "text/html", "UTF-8", null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 44745) {
            if(resultCode == RESULT_OK) {
                if(data != null) {
                    // Get the selected image URI(s)
                    Uri uri = data.getData();
                    Uri[] results = new Uri[]{uri};

                    // Return the selected image URI(s) to the WebView
                    mFilePathCallback.onReceiveValue(results);
                    mFilePathCallback = null;
                }
            } else {
                mFilePathCallback.onReceiveValue(null);
                mFilePathCallback = null;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    /**
     * Example dialog for callback test
     * @param title alert title
     * @param message alert message
     */
    public void showAlert(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(false)
                .create()
                .show();
    }

    private static String getExtensionFromMimeType(String mime) {
        if (mime == null) return ".png"; // default
        mime = mime.toLowerCase();

        // First try Android's map
        String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
        if (ext != null && !ext.isEmpty()) return "." + ext;

        // Fallbacks for common image types
        switch (mime) {
            case "image/jpeg": return ".jpg";
            case "image/jpg":  return ".jpg";
            case "image/png":  return ".png";
            case "image/webp": return ".webp";
            case "image/heic": return ".heic";
            case "image/heif": return ".heif";
            case "image/avif": return ".avif";
            case "image/gif":  return ".gif";
            case "image/bmp":  return ".bmp";
            case "image/tiff": return ".tif";
            default:           return ".png";
        }
    }
    /**
     * Example function for native sharing using callback
     * Note: This implementation writes a temporary file into the app's cache
     * directory and does not remove it afterwards. In a real integration you should
     * add cleanup logic (e.g. delete the file after a delay, or sweep old
     * "share_*" files on app startup) to avoid storage accumulation.
     *
     * @param base64Data base64 image data string
     */
    public void shareBase64Image(String base64Data) {
        try {
            String mimeType = "image/png"; // default
            String extension = ".png";     // default
            String pureBase64 = base64Data;

            if (base64Data != null && base64Data.startsWith("data:")) {
                int sepIdx = base64Data.indexOf(",");
                if (sepIdx > 0) {
                    String header = base64Data.substring(5, sepIdx); // skip "data:"
                    int semiIdx = header.indexOf(";");
                    if (semiIdx > 0) {
                        mimeType = header.substring(0, semiIdx);
                    } else {
                        mimeType = header;
                    }
                    // strip any whitespace/params just in case
                    int paramIdx = mimeType.indexOf(";");
                    if (paramIdx > 0) mimeType = mimeType.substring(0, paramIdx);
                    mimeType = mimeType.trim().toLowerCase();

                    extension = getExtensionFromMimeType(mimeType);
                    pureBase64 = base64Data.substring(sepIdx + 1);
                }
            }

            byte[] decoded = Base64.decode(pureBase64, Base64.DEFAULT);

            String fileName = "share_" + System.currentTimeMillis() + extension;
            File outFile = new File(getCacheDir(), fileName);

            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(decoded);
            }

            Uri uri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", outFile);

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType(mimeType != null ? mimeType : "image/*");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(share, "Share image"));
        } catch (Exception e) {
            android.util.Log.e("REVIEVE_SHARE", "Failed to share image", e);
        }
    }
}