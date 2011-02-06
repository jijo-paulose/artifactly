/*
 * Copyright 2011 Thomas Amsler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package org.artifactly.client;

import java.util.Random;

import org.artifactly.client.service.ArtifactlyService;
import org.artifactly.client.service.LocalService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class Artifactly extends Activity implements ApplicationConstants {
	
	private static final String ARTIFACTLY_URL = "file:///android_asset/artifactly.html";
	
	private static final String LOG_TAG = " ** A.A. **";
	
	// Preferences
	private static final String PREFS_NAME = "ArtifactlyPrefsFile";
	
	// JavaScript functions
	private static final String JAVASCRIPT_PREFIX = "javascript:";
	private static final String JAVASCRIPT_FUNCTION_OPEN_PARENTHESIS = "(";
	private static final String JAVASCRIPT_FUNCTION_CLOSE_PARENTHESIS = ")";
	private static final String SHOW_SERVICE_RESULT = "showServiceResult";
	private static final String JAVASCRIPT_BRIDGE_PREFIX = "android";
	
	private WebView webView = null;
	
	private Handler mHandler = new Handler();
	
	// Access to service API
	private ServiceConnection serviceConnection = getServiceConnection();
	private LocalService localService = null;
	private boolean isBound = false;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        // Create the service intent and start the service
        final Intent artifactlyService = new Intent (this, ArtifactlyService.class);
        startService(artifactlyService);

        // Setting up the WebView
        webView = (WebView) findViewById(R.id.webview);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageFinished(WebView view, String url) {
				// Nothing to do
			}
		});
		
		webView.loadUrl(ARTIFACTLY_URL);
		
		webView.addJavascriptInterface(new JavaScriptInterface(), JAVASCRIPT_BRIDGE_PREFIX);
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	
    	if(!isBound) {
    		// Connect to the local service API
            bindService(new Intent(this, ArtifactlyService.class), serviceConnection, BIND_AUTO_CREATE);
            isBound = true;
            Log.i(LOG_TAG, "onStart Binding service done");
    	}

    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	if(!isBound) {
    		// Connect to the local service API
            bindService(new Intent(this, ArtifactlyService.class), serviceConnection, BIND_AUTO_CREATE);
            isBound = true;
            Log.i(LOG_TAG, "onResume Binding service done");
    	}
    }
    
   
    @Override
    public void onPause() {
    	super.onPause();

    	if(isBound) {

    		isBound = false;
    		unbindService(serviceConnection);
    	}
    }

    @Override
    public void onStop() {
    	super.onStop();
    	
    	if(isBound) {

    		isBound = false;
    		unbindService(serviceConnection);
    	}
    }
    
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	if(isBound) {
    		
    		isBound = false;
    		unbindService(serviceConnection);
    	}
    }

    
    @Override
    public void onNewIntent(Intent intent) {

    	Bundle extras = intent.getExtras();

    	if(null != extras && extras.containsKey(NOTIFICATION_INTENT_KEY)) {
    		String data = extras.getString(NOTIFICATION_INTENT_KEY);
    		//Log.i(LOG_TAG, "Notification data = " + data);
      		callJavaScriptFunction(SHOW_SERVICE_RESULT, data);
    	}
    }

    /*
     * Helper method to call JavaScript methods
     */
    private void callJavaScriptFunction(final String functionName, final String json) {

    	mHandler.post(new Runnable() {

    		public void run() {
    			
    			StringBuilder stringBuilder = new StringBuilder();
    			stringBuilder.append(JAVASCRIPT_PREFIX);
    			stringBuilder.append(functionName);
    			stringBuilder.append(JAVASCRIPT_FUNCTION_OPEN_PARENTHESIS);
    			stringBuilder.append(json);
    			stringBuilder.append(JAVASCRIPT_FUNCTION_CLOSE_PARENTHESIS);
    			Log.i(LOG_TAG, stringBuilder.toString());
    			webView.loadUrl(stringBuilder.toString());
    		}
    	});
    }

    // Define methods that are called from JavaScript
    public class JavaScriptInterface {
    	
    	// TEST data
    	String[] latitudes = { "38.540013", "38.535298", "38.540095" };
    	String[] longitudes = { "-121.57983", "-121.57983", "-121.549062" };
        
    	public void setRadius(String radius) {

    		int newRadius = 0;
    		
    		try {
    			newRadius = Integer.parseInt(radius);
    		}
    		catch(NumberFormatException	nfe) {
    			// TODO: show toast error message
    		}
    		
    		Log.i(LOG_TAG, "A setRadius to " + newRadius);
    		
    		if(PREFERENCE_RADIUS_DEFAULT < newRadius) {

    			SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    			SharedPreferences.Editor editor = settings.edit();
    			Log.i(LOG_TAG, "B setRadius to " + newRadius);
    			editor.putInt(PREFERENCE_RADIUS, newRadius);
    			editor.commit();
    		}
    	}
    	
    	public void createArtifact() {
    		Log.i(LOG_TAG, "called createArtifact");
    		// FIXME: This is for testing until we send the real user data from the UI
    		Random r = new Random();
    		int randomNumber = r.nextInt(latitudes.length);
    		boolean isSuccess = localService.createArtifact("Name " + randomNumber, "Data " + randomNumber, latitudes[randomNumber], longitudes[randomNumber]);
    		
    		if(isSuccess) {
    			
    			Toast toast = Toast.makeText(getApplicationContext(), R.string.create_artifact_success, Toast.LENGTH_SHORT);
    			toast.show();
    		}
    		else {
    			
    			Toast toast = Toast.makeText(getApplicationContext(), R.string.create_artifact_failure, Toast.LENGTH_SHORT);
    			toast.show();
    		}
    	}
    	
    	public void startLocationTracking() {

    		boolean isSuccess = localService.startLocationTracking();

    		if(isSuccess) {

    			Toast toast = Toast.makeText(getApplicationContext(), R.string.start_location_tracking_success, Toast.LENGTH_SHORT);
    			toast.show();
    		}
    		else {

    			Toast toast = Toast.makeText(getApplicationContext(), R.string.start_location_tracking_failure, Toast.LENGTH_SHORT);
    			toast.show();
    		}
    	}

    	public void stopLocationTracking() {

    		boolean isSuccess = localService.stopLocationTracking();

    		if(isSuccess) {

    			Toast toast = Toast.makeText(getApplicationContext(), R.string.stop_location_tracking_success, Toast.LENGTH_SHORT);
    			toast.show();
    		}
    		else {

    			Toast toast = Toast.makeText(getApplicationContext(), R.string.stop_location_tracking_failure, Toast.LENGTH_SHORT);
    			toast.show();
    		}
    		
    	}
    	
    	public int getRadius() {
    		
    		SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    		int radius = settings.getInt(PREFERENCE_RADIUS, PREFERENCE_RADIUS_DEFAULT);
    		Log.i(LOG_TAG, "Radius = " + radius);
    		return radius;
    	}
    } 
    
    // Method that returns a service connection
    private ServiceConnection getServiceConnection() {
    	
    	return new ServiceConnection() {

			public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
				
				localService = (LocalService)iBinder;
				isBound = true;
				Log.i(LOG_TAG, "onServiceConnected called");
			}

			public void onServiceDisconnected(ComponentName componentName) {
				
				localService = null;
				isBound = false;
				Log.i(LOG_TAG, "onServiceDisconnected called");
			}
    	};
    }
}