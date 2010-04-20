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

import java.text.NumberFormat;
import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.TimeFormatException;

/**
 * General application settings.
 * 
 * @author Dag Rende
 */
public class Settings extends PreferenceActivity {
	public static final NumberFormat format2digits = NumberFormat.getInstance();

	OnSharedPreferenceChangeListener prefsChangeListener = new OnSharedPreferenceChangeListener() {

		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			updatePrefSummaries();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);

		format2digits.setMinimumIntegerDigits(2);
		updatePrefSummaries();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		final SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		sharedPrefs
				.registerOnSharedPreferenceChangeListener(prefsChangeListener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		final SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		sharedPrefs
				.unregisterOnSharedPreferenceChangeListener(prefsChangeListener);
	}

	private void updatePrefSummaries() {
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		setStringPrefSummaryToValue(sharedPrefs, R.string.pref_precision_key,
				R.string.precision_setting_summary);
		setTimePrefSummaryToValue(sharedPrefs, R.string.pref_lunch_start_key,
				R.string.lunch_start_summary, "11:30");
		setTimePrefSummaryToValue(sharedPrefs, R.string.pref_lunch_end_key,
				R.string.lunch_end_summary, "12:30");
	}

	/**
	 * @param sharedPrefs
	 * @param defaultValue TODO
	 * @param prefLunchStartKey
	 * @param lunchStartSummary
	 */
	private void setTimePrefSummaryToValue(SharedPreferences sharedPrefs,
			int keyId, int summaryId, String defaultValue) {
		String key = getString(keyId);
		Preference preference = findPreference(key);
		preference.setSummary(getString(summaryId) + " "
				+ formatTime(sharedPrefs.getString(key, defaultValue)));
	}

	/**
	 * @param context 
	 * @param string
	 * @return
	 */
	private String formatTime(String timeString) {
		String[] timeParts = timeString.split(":");
		int hour = Integer.parseInt(timeParts[0]);
		int minute = Integer.parseInt(timeParts[1]);
		String formattedHour = format2digits.format(hour);
		String formattedMinute = format2digits.format(minute);
		return formattedHour + ":" + formattedMinute;
	}

	private void setStringPrefSummaryToValue(SharedPreferences sharedPrefs,
			int keyId, int summaryId) {
		String key = getString(keyId);
		Preference preference = findPreference(key);
		preference.setSummary(getString(summaryId) + " "
				+ sharedPrefs.getString(key, ""));
	}

	/**
	 * Returns the precision for session hours display in list on sessions page.
	 * 
	 * @param context
	 *            normally the calling Activity
	 * @return precision as a number giving the maximum presicion (ex 0.25 gives
	 *         hours as 0, 0.25, 0.5, 0.75 etc)
	 */
	public static float getPrecision(Context context) {
		String precisionString = PreferenceManager.getDefaultSharedPreferences(
				context).getString("precision", "0.1");
		try {
			return Float.parseFloat(precisionString);
		} catch (NumberFormatException e) {
			try {
				return Float.parseFloat(precisionString.replace(',', '.'));
			} catch (NumberFormatException e1) {
				return 0.1f;
			}
		}
	}

	/**
	 * True when user wants lunch time to be excluded from the work hours
	 * calculated for each session.
	 * 
	 * @return true when lunch is to be excluded
	 */
	public static boolean isExcludeLunchTime(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean("exclude_lunch_time", false);
	}

	/**
	 * The milliseconds from midnight to start of lunch.
	 * 
	 * @param context
	 * @return ms from midnight
	 */
	public static long getLunchStart(Context context) {
		String lunchStartString = PreferenceManager
				.getDefaultSharedPreferences(context).getString("lunch_start",
						"11:30");
		return getMsFromMidnight(lunchStartString);
	}

	/**
	 * The milliseconds from midnight to end of lunch.
	 * 
	 * @param context
	 * @return ms from midnight
	 */
	public static long getLunchEnd(Context context) {
		String lunchStartString = PreferenceManager
				.getDefaultSharedPreferences(context).getString("lunch_end",
						"12:30");
		return getMsFromMidnight(lunchStartString);
	}

	/**
	 * @param lunchStartString
	 * @return
	 */
	private static long getMsFromMidnight(String timeString) {
		String[] timeParts = timeString.split(":");
		return 60000L * (Long.parseLong(timeParts[0]) * 60L + Long
				.parseLong(timeParts[1]));
	}

}
