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
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

public class Artifactly extends Activity implements ApplicationConstants {

	private static final String ARTIFACTLY_URL = "file:///android_asset/artifactly.html";

	private static final String LOG_TAG = " ** A.A. **";

	// Preferences
	private static final String PREFS_NAME = "ArtifactlyPrefsFile";

	// Constants
	private static final String EMPTY_STRING = "";
	
	// JavaScript function constants
	private static final String JAVASCRIPT_PREFIX = "javascript:";
	private static final String JAVASCRIPT_FUNCTION_OPEN_PARENTHESIS = "(";
	private static final String JAVASCRIPT_FUNCTION_CLOSE_PARENTHESIS = ")";
	private static final String JAVASCRIPT_BRIDGE_PREFIX = "android";
	
	// JavaScript functions
	private static final String SHOW_SERVICE_RESULT = "showServiceResult";
	
	private WebView webView = null;

	private Handler mHandler = new Handler();

	// Access to service API
	private ServiceConnection serviceConnection = getServiceConnection();
	private LocalService localService = null;
	private boolean isBound = false;


	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i(LOG_TAG, "onCreate()");

		setContentView(R.layout.main);

		/*
		 * Calling startService so that the service keeps running. e.g. After application installation
		 * The start of the service at boot is handled via a BroadcastReceiver and the BOOT_COMPLETED action
		 */
		startService(new Intent(this, ArtifactlyService.class));
		
		// Bind to the service
		bindService(new Intent(this, ArtifactlyService.class), serviceConnection, BIND_AUTO_CREATE);
		isBound = true;

		// Setting up the WebView
		webView = (WebView) findViewById(R.id.webview);
		webView.getSettings().setJavaScriptEnabled(true);

		// Disable the vertical scroll bar
		webView.setVerticalScrollBarEnabled(false);
		
		webView.addJavascriptInterface(new JavaScriptInterface(), JAVASCRIPT_BRIDGE_PREFIX);
		
		webView.setWebChromeClient(new WebChromeClient() {
			  public boolean onConsoleMessage(ConsoleMessage cm) {
			    Log.d("** A.A - JS **", cm.message() + " -- From line "
			                         + cm.lineNumber() + " of "
			                         + cm.sourceId() );
			    return true;
			  }
			});

		webView.loadUrl(ARTIFACTLY_URL);

		
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	public void onStart() {
		super.onStart();

		Log.i(LOG_TAG, "onStart()");
		
		if(!isBound) {
			// Connect to the local service API
			bindService(new Intent(this, ArtifactlyService.class), serviceConnection, BIND_AUTO_CREATE);
			isBound = true;
			Log.i(LOG_TAG, "onStart Binding service done");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public void onResume() {
		super.onResume();

		Log.i(LOG_TAG, "onResume()");
		
		if(!isBound) {
			// Connect to the local service API
			bindService(new Intent(this, ArtifactlyService.class), serviceConnection, BIND_AUTO_CREATE);
			isBound = true;
			Log.i(LOG_TAG, "onResume Binding service done");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	public void onPause() {
		super.onPause();

		Log.i(LOG_TAG, "onPause()");
		
		if(isBound) {

			isBound = false;
			unbindService(serviceConnection);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop() {
		super.onStop();

		Log.i(LOG_TAG, "onStop()");
		
		if(isBound) {

			isBound = false;
			unbindService(serviceConnection);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();

		Log.i(LOG_TAG, "onDestroy()");
		
		if(isBound) {

			isBound = false;
			unbindService(serviceConnection);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	public void onNewIntent(Intent intent) {

		Log.i(LOG_TAG, "onNewIntent()");
		
		Bundle extras = intent.getExtras();

		if(null != extras && extras.containsKey(NOTIFICATION_INTENT_KEY)) {
			
			String data = extras.getString(NOTIFICATION_INTENT_KEY);
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
				webView.loadUrl(stringBuilder.toString());
			}
		});
	}

	// Define methods that are called from JavaScript
	public class JavaScriptInterface {

		// TEST data
		String[] latitudes = { "38.540013", "38.535298", "38.540095" };
		String[] longitudes = { "-121.57983", "-121.57983", "-121.549062" };

		public void setRadius(int radius) {

			Log.i(LOG_TAG, "A setRadius to " + radius);

			if(PREFERENCE_RADIUS_DEFAULT < radius) {

				String message = String.format(getResources().getString(R.string.set_location_radius), radius);
				Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

				SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
				SharedPreferences.Editor editor = settings.edit();
				editor.putInt(PREFERENCE_RADIUS, radius);
				editor.commit();
			}
		}

		public void createArtifact(String name, String data) {
			
			Log.i(LOG_TAG, "called createArtifact name = " + name + " : data = " + data);
			
			if(null == name || EMPTY_STRING.equals(name)) {
				
				Toast.makeText(getApplicationContext(), R.string.create_artifact_name_error, Toast.LENGTH_SHORT).show();
				return;
			}
			
			Random r = new Random();
			int randomNumber = r.nextInt(latitudes.length);
			boolean isSuccess = localService.createArtifact(name, data, latitudes[randomNumber], longitudes[randomNumber]);

			if(isSuccess) {

				Toast.makeText(getApplicationContext(), R.string.create_artifact_success, Toast.LENGTH_SHORT).show();
			}
			else {

				Toast.makeText(getApplicationContext(), R.string.create_artifact_failure, Toast.LENGTH_SHORT).show();
			}
		}

		public void startLocationTracking() {

			boolean isSuccess = localService.startLocationTracking();

			if(isSuccess) {

				Toast.makeText(getApplicationContext(), R.string.start_location_tracking_success, Toast.LENGTH_SHORT).show();
			}
			else {

				Toast.makeText(getApplicationContext(), R.string.start_location_tracking_failure, Toast.LENGTH_SHORT).show();
			}
		}

		public void stopLocationTracking() {

			boolean isSuccess = localService.stopLocationTracking();

			if(isSuccess) {

				Toast.makeText(getApplicationContext(), R.string.stop_location_tracking_success, Toast.LENGTH_SHORT).show();
			}
			else {

				Toast.makeText(getApplicationContext(), R.string.stop_location_tracking_failure, Toast.LENGTH_SHORT).show();
			}
		}

		public int getRadius() {

			SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
			return settings.getInt(PREFERENCE_RADIUS, PREFERENCE_RADIUS_DEFAULT);
		}
		
		public void showRadius() {
			
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
			int radius = settings.getInt(PREFERENCE_RADIUS, PREFERENCE_RADIUS_DEFAULT);
			String message = String.format(getResources().getString(R.string.set_location_radius), radius);
			Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
		}

		public String getLocation() {

			return localService.getLocation();
		}
		
		public String logArtifacts() {
			
			return localService.getArtifacts();
		}
		
		public String getArtifactsForCurrentLocation() {

			return localService.getArtifactsForCurrentLocation();
		}
		
		public boolean canAccessInternet() {
			
			boolean canAccessInternet = localService.canAccessInternet();
			
			if(!canAccessInternet) {
				Toast.makeText(getApplicationContext(), R.string.can_access_internet_error, Toast.LENGTH_LONG).show();
			}
			return canAccessInternet;
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