/*
 * Copyright (C) 2006 Dag Rende
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

import static se.rende.mytime.Constants.CONTENT_URI_PROJECT;
import static se.rende.mytime.Constants.CONTENT_URI_SESSION;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TreeMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

/**
 *
 * @author Dag Rende
 */
public class ShareProjectReport extends Activity implements OnClickListener {
	private static final NumberFormat hoursFormat = NumberFormat.getInstance();
	private DateFormatSymbols dateFormatSymbols = new DateFormatSymbols();
	private long currentProjectId;
	private String projectName;
	private TreeMap<Long, Float> weekTotals = new TreeMap<Long, Float>();
	private TreeMap<Long, Float> monthTotals = new TreeMap<Long, Float>();
	private CheckBox includeWeekTotals;
	private CheckBox includeMonthTotals;
	private CheckBox groupByDay;
	private SharedPreferences sharedPrefs;
	private Spinner period;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Uri intentData = getIntent().getData();
		currentProjectId = Long.parseLong(intentData.getLastPathSegment());
		projectName = getProjectName();
		setContentView(R.layout.share_project_report);
		Sessions.calculateTotals(this, currentProjectId, monthTotals, weekTotals);
		
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		period = (Spinner) findViewById(R.id.share_project_period);
		setPeriodLabels();
		period.setSelection(sharedPrefs.getInt(getString(R.string.pref_share_project_period_key), 1));
		includeWeekTotals = (CheckBox) findViewById(R.id.share_project_include_week_totals);
		String key = getString(R.string.pref_share_project_include_week_totals_key);
		boolean value = sharedPrefs.getBoolean(key, true);
		includeWeekTotals.setChecked(value);
		includeMonthTotals = (CheckBox) findViewById(R.id.share_project_include_month_totals);
		includeMonthTotals.setChecked(sharedPrefs.getBoolean(getString(R.string.pref_share_project_include_month_totals_key), true));
		groupByDay = (CheckBox) findViewById(R.id.share_project_group_by_day);
		groupByDay.setChecked(sharedPrefs.getBoolean(getString(R.string.pref_share_project_group_by_day_key), true));

		findViewById(R.id.share_project_button).setOnClickListener(this);
	}
	
	/**
	 * Describes a period to generate a report on.
	 */
	static class PeriodInfo {
		public String label;	// text to display in spinner menu item
		public long from;		// period start (inclusive)
		public long upTo;		// period end (exclusive)

		public PeriodInfo(String label, long from, long upTo) {
			this.label = label;
			this.from = from;
			this.upTo = upTo;
		}
		
		public static PeriodInfo getWeekInfo(String labelPrefix, long start, int weeksOffset) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(start);
			cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			cal.add(Calendar.WEEK_OF_YEAR, weeksOffset);
			long from = cal.getTimeInMillis();
			int weekNo = cal.get(Calendar.WEEK_OF_YEAR);
			cal.add(Calendar.WEEK_OF_YEAR, 1);
			long upTo = cal.getTimeInMillis();
			return new PeriodInfo(labelPrefix + " (" + weekNo + ")", from, upTo);
		}

		public static PeriodInfo getMonthInfo(String labelPrefix, long start, int monthsOffset, DateFormatSymbols dateFormatSymbols) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(start);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			cal.add(Calendar.MONTH, monthsOffset);
			long from = cal.getTimeInMillis();
			int monthNo = cal.get(Calendar.MONTH);
			cal.add(Calendar.MONTH, 1);
			long upTo = cal.getTimeInMillis();
			return new PeriodInfo(labelPrefix + " (" + dateFormatSymbols.getMonths()[monthNo] + ")", from, upTo);
		}
	}
	
	List<PeriodInfo> periodInfos = new ArrayList<PeriodInfo>();
	
	
	/**
	 * Sets the labels for the period selection spinner, to include the specific month names and week numbers.
	 */
	private void setPeriodLabels() {
		periodInfos.add(PeriodInfo.getMonthInfo(getString(R.string.share_period_this_month), System.currentTimeMillis(), 0, dateFormatSymbols));
		periodInfos.add(PeriodInfo.getMonthInfo(getString(R.string.share_period_last_month), System.currentTimeMillis(), -1, dateFormatSymbols));
		periodInfos.add(PeriodInfo.getWeekInfo(getString(R.string.share_period_this_week), System.currentTimeMillis(), 0));
		periodInfos.add(PeriodInfo.getWeekInfo(getString(R.string.share_period_last_week), System.currentTimeMillis(), -1));
		periodInfos.add(new PeriodInfo(getString(R.string.share_period_all_sessions), 0, Long.MAX_VALUE));
		
		List<CharSequence> list = new ArrayList<CharSequence>();
		for (PeriodInfo periodInfo : periodInfos) {
			list.add(periodInfo.label);
		}
		
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, list);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		period.setAdapter(adapter);
	}

	/**
	 * @return
	 */
	private String getProjectName() {
		Cursor cursor = getContentResolver().query(CONTENT_URI_PROJECT,
				new String[] { "name" }, "_id=" + currentProjectId, null, null);
		try {
			if (cursor.moveToNext()) {
				return cursor.getString(0);
			}
			return "";
		} finally {
			cursor.close();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.share_project_button:
			savePrefs();
			shareSession();
			break;

		default:
			break;
		}
	}

	/**
	 * Stores the report selection in prefs storage.
	 */
	private void savePrefs() {
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putInt(getString(R.string.pref_share_project_period_key), period.getSelectedItemPosition());
		String key = getString(R.string.pref_share_project_include_week_totals_key);
		boolean value = includeWeekTotals.isChecked();
		editor.putBoolean(
				key,
				value);
		editor.putBoolean(
				getString(R.string.pref_share_project_include_month_totals_key),
				includeMonthTotals.isChecked());
		editor.putBoolean(
				getString(R.string.pref_share_project_group_by_day_key),
				groupByDay.isChecked());
		editor.commit();
	}

	/**
	 * Send a report using any app that can take a text message - normally a mail or sms app.
	 */
	private void shareSession() {
		try {
			String fileName = getString(R.string.time_report) + " " + projectName + " " + DateFormat.getDateFormat(this).format(System.currentTimeMillis()).replace('/', '-');
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			writeHtmlReport(baos);
			baos.close();

			Intent i=new Intent(android.content.Intent.ACTION_SEND);
			i.putExtra(Intent.EXTRA_SUBJECT, fileName);
			i.setType("text/plain");
			i.putExtra(Intent.EXTRA_TEXT, baos.toString("UTF-8"));
			startActivity(Intent.createChooser(i, getString(R.string.share_title)));
		} catch (Exception e) {
			new AlertDialog.Builder(this)
		      .setMessage(getString(R.string.share_error_message) + " " + e)
		      .show();
		}
	}

	/**
	 * Writes time report as html in UTF-8 encoding to the output stream.
	 * @param os where xml is written
	 * @throws IOException
	 */
	public void writeHtmlReport(OutputStream os) throws IOException {
		OutputStreamWriter sw = new OutputStreamWriter(os, "UTF-8");
		PrintWriter pw = new PrintWriter(sw);
		PeriodInfo periodInfo = periodInfos.get(period.getSelectedItemPosition());
//		pw.println("PeriodInfo(" + periodInfo.label + ", " + 
//				DateFormat.getDateFormat(this).format(periodInfo.from) + " " + DateFormat.getTimeFormat(this).format(periodInfo.from) + ", " + 
//				DateFormat.getDateFormat(this).format(periodInfo.upTo) + " " + DateFormat.getTimeFormat(this).format(periodInfo.upTo) + ")");
		Cursor sessionCursor = null;
		try {
			sessionCursor = getContentResolver().query(CONTENT_URI_SESSION,
					new String[] { "_id", "start", "end", "comment" }, 
					"project_id=? and end is not null", new String[] {"" + currentProjectId}, "start asc");
			String lastDate = null;
			long lastDateMillis = 0;
			long lastId = -1;
			float daySum = 0f;
			StringBuilder comments = new StringBuilder();
			while (sessionCursor.moveToNext()) {
				long startTime = sessionCursor.getLong(1);
				if (periodInfo.from <= startTime && startTime < periodInfo.upTo) {
					long id = sessionCursor.getLong(0);					
					long endTime = sessionCursor.getLong(2);
					String comment = sessionCursor.getString(3);
					
					String dateString = DateFormat.getDateFormat(this).format(startTime);
					if (!groupByDay.isChecked()) {
						printDateLine(pw, comment, dateString, startTime, Sessions.getWorkHours(this, startTime, endTime), id);
					} else {
						if (lastDate == null) {
							lastDate = dateString;
							lastDateMillis = startTime;
						} else if (!lastDate.equals(dateString)) {
							printDateLine(pw, comments.toString(), lastDate, lastDateMillis, daySum, lastId);
							lastDate = dateString;
							lastDateMillis = startTime;
							daySum = 0f;
							comments.setLength(0);
						}
						daySum += Sessions.getWorkHours(this, startTime, endTime);
						if (comment != null && comment.length() > 0) {
							if (comments.length() > 0 && comment.length() > 0) {
								comments.append(", ");
							}
							comments.append(comment);
						}
						lastId = id;
					}
				}
			}
			if (lastId != -1) {
		    	printDateLine(pw, comments.toString(), lastDate, lastDateMillis, daySum, lastId);
			}
		} finally {
			if (sessionCursor != null) {
				sessionCursor.close();
			}
		}
		pw.flush();
	}

	private void printDateLine(PrintWriter pw, String comment, String dateString, long lastDateMillis, float workHours, long lastId) {
		pw.print(dateString + "\t" + hoursFormat.format(workHours));
		if (comment != null && comment.length() > 0) {
			pw.print("\t" + comment);
		}
		pw.println();
		
		if (includeWeekTotals.isChecked()) {
			Float weekTotal = weekTotals.get(lastId);
			if (weekTotal != null) {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(lastDateMillis);
				pw.println(getString(R.string.report_date_line_week) + " " + cal.get(Calendar.WEEK_OF_YEAR) + " " + getString(R.string.report_date_line_total) + "\t" + hoursFormat.format(weekTotal));
			}
		}
		
		if (includeMonthTotals.isChecked()) {
			Float monthTotal = monthTotals.get(lastId);
			if (monthTotal != null) {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(lastDateMillis);
				pw.println(dateFormatSymbols.getMonths()[cal.get(Calendar.MONTH)] + " total\t" + hoursFormat.format(monthTotal));
			}
		}
	}
}
