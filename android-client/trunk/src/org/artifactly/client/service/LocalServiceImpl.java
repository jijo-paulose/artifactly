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

package org.artifactly.client.service;

import org.artifactly.client.content.DbAdapter;

import android.os.Binder;
import android.util.Log;

public class LocalServiceImpl extends Binder implements LocalService {

	// Logging
	private static final String LOG_TAG = "** A.L.S. **";
	
	ArtifactlyService artifactlyService;
	
	DbAdapter dbAdapter;
	
	// Constructor
	public LocalServiceImpl(ArtifactlyService artifactlyService) { 
		this.artifactlyService = artifactlyService;
		
		if(null != artifactlyService) {
			this.dbAdapter = artifactlyService.getDbAdapter();
		}
	}
	
	// API method
	public boolean createArtifact(String name, String data, String latitude, String longitude) {
		
		Log.i(LOG_TAG, "LocalService : createArtifact called");
		
		if(null == dbAdapter) {
			Log.e(LOG_TAG, "LocalService : createArtifact : dbAdapter is null");
			return false;
		}
		
		dbAdapter.insert(latitude, longitude, name, data);
		
		return true;
	}

	// API method
	public boolean startLocationTracking() {
		
		if(null != artifactlyService) {
			
			return artifactlyService.startLocationTracking();
		}
		else {
			
			return false;
		}
	}
	
	// API method
	public boolean stopLocationTracking() {

		if(null != artifactlyService) {

			return artifactlyService.stopLocationTracking();
		}
		else {

			return false;
		}
	}
}
