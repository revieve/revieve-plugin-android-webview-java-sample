# Android WebView Integration Sample

This repository contains a sample android application that demonstrates how to integrate our plugin solution within a native application using WebView. The primary goal of this sample project is to provide developers with a clear and concise guide for integrating our plugin with their native applications.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Integration Steps](#integration-steps)
3. [Communication with plugin](#communication-with-plugin)

## Getting Started

Before you begin, make sure you have the following prerequisites:

- Android Studio

1. Clone the repository:
```bash
git clone https://github.com/revieve/revieve-plugin-android-webview-java-sample.git
```
2. Open the project in Android Studio.
3. Sync the project with Gradle files.
4. Run the app on an emulator or a physical device.

## Integration Steps

Follow these step-by-step instructions to integrate the plugin solution within your android application using webView:

1. **Configure partnerId and environment:**

In your `FullscreenActivity.java` file, setup the configuration variables as instructed by our implementation team:

```java
// Select which Revieve API environment to use. Can be test or prod
static final String REVIEVE_ENV = "test";
// your partner Id provided by Revieve
static final String REVIEVE_PARTNER_ID = "9KpsLizwYK";
```

2. **Add the WebView to your layout:**

Set up a `WKWebViewConfiguration` instance, configure it with necessary settings, and create a `WKWebView` instance using the configuration:

```xml
<WebView
    android:id="@+id/revieveWebview"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

3. **Configure WebView settings:**

In your activity, configure the WebView settings to enable JavaScript and customize other settings as needed:

```java
webView = (WebView) findViewById(R.id.revieveWebview);
WebSettings webSettings = webView.getSettings();
webSettings.setJavaScriptEnabled(true);
webSettings.setAllowFileAccess(true);
webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
webSettings.setGeolocationEnabled(true);
webSettings.setAllowContentAccess(true);
webSettings.setMediaPlaybackRequiresUserGesture(false);
```

4. **Inject config on provided HTML sample file:**

```java
final String html = getHtmlWithConfig("revieve.html");
```

5. **Load the plugin from assets HTML file:**

```java
myWebView.loadDataWithBaseURL("https://d38knilzwtuys1.cloudfront.net/revieve-plugin-v4/app.html", html, "text/html", "UTF-8", null);
```

## Communication with plugin

The sample project provides basic integration with the plugin solution. Customize the response to plugin callbacks by modifying the source code as needed.

Refer to the plugin solution's basic and advanced documentation for details on available callbacks and data options.

The `postMessage`` API enables seamless communication between the plugin solution and your native application. This section guides you through setting up and handling `postMessage`` communication in the integration sample.

### Configuring PostMessage Listener

1. **Set up a JavaScript interface:**

Create a JavaScriptInterface class that will handle messages from the plugin:

```java
webView.addJavascriptInterface(new WebAppInterface(this), "Android");
```

2. **Inject JavaScript handler:**

Add a JavaScript method to your HTML that forwards the received messages to your native app:

```javascript
function postMessageToCallbackHandler(type, payload) {
    const message = { type, payload };
    const serializedMessage = JSON.stringify(message);
    Android.handleMessage(serializedMessage);
}
```

Use this method inside any callback you can to forward to the native side:

```javascript
...
onClickProduct: function (product) {
    postMessageToCallbackHandler("onClickProduct", [product]);
},
...
```


3. **Handle JavaScript messages:**

Create a `JavaScriptInterface` class and implement the `handleMessage` method to handle incoming plugin events:

```java
class JavaScriptInterface {
  @JavascriptInterface
  public void handleMessage(String message) {
    // Handle JavaScript messages here
  }
}
```

Inside the `handleMessage` method, you can parse the JSON message body and perform actions based on the received events. For example, you may want to display a Toast when a specific event is triggered:

```java
@JavascriptInterface
public void handleMessage(String message) {
    try {
        JsonObject message = new Gson().fromJson(body, JsonObject.class);
        String type = message.get("type").getAsString();

        if (type.equals("someEventType")) {
            showToast("Event triggered: 'someEventType'");
        }
    } catch (JSONException e) {
        e.printStackTrace();
    }
}

private void showToast(final String message) {
    ((MainActivity) context).runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
}
```

Refer to the plugin internal basic and advanced documentation for a comprehensive list of available events/callbacks and their payloads.

### Sending API Commands

In some cases, like PDP try-on integration, you may want to send commands from your native app to the Revive API. This section demonstrates how to call a JavaScript function in the plugin solution from your native app.

1. **Create the function to send the command:**

For example, let's say you want to send a `tryonProduct` command with a specific product ID when a button is clicked. 

```java
private void sendTryonProductCommand(String productId) {
    String action = String.format("{\"type\":\"tryonProduct\", \"payload\": {\"id\":\"%s\"}}", productId);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        myWebView.postWebMessage(
            new WebMessage(action),
            Uri.parse(REVIEVE_CDN_DOMAIN)
        );
    }
}
```

In the example above, a javascript Revieve API function is called directly with the product ID as a parameter.

2. **Add a button to trigger the command:**

Add a Button to your app's user interface that triggers the `sendTryonProductCommand` function when clicked.

```xml
<Button
    android:id="@+id/sendTryonProductButton"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Tryon Product" /> 
```

In your activity's onCreate method, find the Button and set an OnClickListener to trigger the sendTryonProductCommand function when the Button is clicked:

```java
Button sendTryonProductButton = findViewById(R.id.sendTryonProductButton);
sendTryonProductButton.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        sendTryonProductCommand("example-product-id");
    }
});
```

That's it! Now, your native app can communicate with the plugin solution both ways. You can send commands to the plugin and handle incoming events.

For more information about available commands and events, refer to the plugin solution's documentation.

### Legacy integration without loader script

You can find our legacy documentation for integrating the plugin solution without the loader script [here](https://github.com/revieve/revieve-plugin-android-webview-java-sample/tree/legacy)