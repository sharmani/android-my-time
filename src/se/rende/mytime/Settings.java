/*
 * Copyright (C) 2010 Dag Rende
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.rende.mytime;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * General application settings.
 * 
 * @author Dag Rende
 */
public class Settings extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
	}
	
	/**
	 * Returns the precision for session hours display in list on sessions page.
	 * 
	 * @param context normally the calling Activity
	 * @return precision as a number giving the maximum presicion (ex 0.25 gives hours as 0, 0.25, 0.5, 0.75 etc)
	 */
	public static float getPrecision(Context context) {
		String prefString = PreferenceManager.getDefaultSharedPreferences(context).getString("precision", "0.1");
		try {
			return Float.parseFloat(prefString);
		} catch (NumberFormatException e) {
			return 0.1f;
		}
	}
}
