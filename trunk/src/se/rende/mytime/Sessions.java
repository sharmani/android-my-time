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

import static se.rende.mytime.Constants.CONTENT_URI_PROJECT;
import static se.rende.mytime.Constants.CONTENT_URI_SESSION;

import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * Displays sessions for the project described by the item Url in intent data.
 * let user start stop new sessions and edit or remove listed sessions.
 * 
 * @author Dag Rende
 */
public class Sessions extends ListActivity implements OnClickListener {
	private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;
	private static final SimpleDateFormat WEEKDAY_MONTHDAY_FORMAT = new java.text.SimpleDateFormat("E d");
	private static final NumberFormat hoursFormat = NumberFormat.getInstance();
	private static final String[] FROM = { "start", "end", "_id+5", "comment", "_id",
			"_id+1", "_id+2", "_id+3", "_id+4" };
	private static final int[] TO = { R.id.start_date_time, R.id.end_time, R.id.end_date_time,
			R.id.comment, R.id.hours, R.id.month_total_label, R.id.month_total,
			R.id.week_total_label, R.id.week_total };
	private long currentProjectId = 1;
	private TreeMap<Long, Float> monthTotals = new TreeMap<Long, Float>();
	private Set<String> monthTotalsMonths = new HashSet<String>();
	private TreeMap<Long, Float> weekTotals = new TreeMap<Long, Float>();
	private Set<String> weekTotalsWeeks = new HashSet<String>();
	private DateFormatSymbols dateFormatSymbols = new DateFormatSymbols();
	private IntentFilter dbUpdateFilter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		long timeBefore = System.currentTimeMillis();
		super.onCreate(savedInstanceState);

	    Uri intentData = getIntent().getData();
		currentProjectId = Long.parseLong(intentData.getLastPathSegment());

		setContentView(R.layout.sessions);

		TextView projectNameView = (TextView) findViewById(R.id.sessionsProjectName);
		projectName = getProjectName();
		projectNameView.setText(projectName);

		getListView().setOnCreateContextMenuListener(this);
		startButton = findViewById(R.id.StartButton);
		startButton.setOnClickListener(this);
		stopButton = findViewById(R.id.StopButton);
		stopButton.setOnClickListener(this);

		hoursFormat.setMaximumFractionDigits(2);

		showSessions(getSessions(currentProjectId));

		adjustButtonEnablement();
		
	    dbUpdateFilter = new IntentFilter(Constants.INTENT_DB_UPDATE_ACTION);
		Log.d("onCreate", "time=" + (System.currentTimeMillis() - timeBefore) + "ms");

	}

	@Override
	protected void onResume() {
		registerReceiver(dbUpdateReceiver, dbUpdateFilter);
		super.onResume();
		clearTotals();
		adjustButtonEnablement();
	}
	
	@Override
	protected void onPause() {
		unregisterReceiver(dbUpdateReceiver);
		super.onPause();
	}

	/**
	 * Make week and month totals be recalculated next time sessions list is updated.
	 */
	private void clearTotals() {
		monthTotals.clear();
		monthTotalsMonths.clear();
		weekTotals.clear();
		weekTotalsWeeks.clear();
	}

	/**
	 * Calculates week total for the week containing the specified time.
	 * @param time millisecond time to calculate total for
	 */
	public void calculateWeekTotals(long time) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		String timeWeekKey = cal.get(Calendar.YEAR) + "-"
				+ cal.get(Calendar.WEEK_OF_YEAR);
		if (!weekTotalsWeeks.contains(timeWeekKey)) {
			int firstDayOfWeek = cal.getFirstDayOfWeek();
			int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
			cal.setTimeInMillis(time - (dayOfWeek - firstDayOfWeek) * DAY_MILLIS);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			long fromTime = cal.getTimeInMillis();
			long toTime = fromTime + 7L * DAY_MILLIS;
			Cursor cursor = getContentResolver().query(CONTENT_URI_SESSION,
				new String[] { "_id", "start", "end" },
				"project_id=? and end is not null and start between ? and ?",
				new String[] { Long.toString(currentProjectId), Long.toString(fromTime), Long.toString(toTime) }, "start desc");
			
			try {
				String currentWeekKey = "";
				float currentWeekTotal = 0f;
				long firstSessionIdOfWeek = 0;
				while (cursor.moveToNext()) {
					// week
					long sessionId = cursor.getLong(0);
					long startTime = cursor.getLong(1);
					long endTime = cursor.getLong(2);

					cal.setTimeInMillis(startTime);
					String weekKey = cal.get(Calendar.YEAR) + "-"
							+ cal.get(Calendar.WEEK_OF_YEAR);

					if (!currentWeekKey.equals(weekKey)) {
						// beginning of a new week
						if (currentWeekKey.length() > 0) {
							weekTotals.put(firstSessionIdOfWeek,
									currentWeekTotal);
							weekTotalsWeeks.add(currentWeekKey);
						}
						currentWeekKey = weekKey;
						currentWeekTotal = 0f;
						firstSessionIdOfWeek = sessionId;
					}
					currentWeekTotal += getWorkHours(this, startTime,
							endTime);
				}
				if (currentWeekKey.length() > 0) {
					weekTotals.put(firstSessionIdOfWeek,
							currentWeekTotal);
					weekTotalsWeeks.add(currentWeekKey);
				}
			} finally {
				cursor.close();
			}
		}
	}
	
	/**
	 * Calculates week total for the week containing the specified time.
	 * @param time millisecond time to calculate total for
	 */
	public void calculateMonthTotals(long time) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		String timeMonthKey = cal.get(Calendar.YEAR) + "-"
				+ cal.get(Calendar.MONTH);
		if (!monthTotalsMonths.contains(timeMonthKey)) {
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			long fromTime = cal.getTimeInMillis();
			cal.add(Calendar.MONTH, 1);
			long toTime = cal.getTimeInMillis();
			Cursor cursor = getContentResolver().query(CONTENT_URI_SESSION,
				new String[] { "_id", "start", "end" },
				"project_id=? and end is not null and start between ? and ?",
				new String[] { Long.toString(currentProjectId), Long.toString(fromTime), Long.toString(toTime) }, "start desc");
			
			try {
				String currentMonthKey = "";
				float currentMonthTotal = 0f;
				long firstSessionIdOfMonth = 0;
				while (cursor.moveToNext()) {
					// week
					long sessionId = cursor.getLong(0);
					long startTime = cursor.getLong(1);
					long endTime = cursor.getLong(2);

					cal.setTimeInMillis(startTime);
					String monthKey = cal.get(Calendar.YEAR) + "-"
							+ cal.get(Calendar.MONTH);

					if (!currentMonthKey.equals(monthKey)) {
						// beginning of a new month
						if (currentMonthKey.length() > 0) {
							monthTotals.put(firstSessionIdOfMonth,
									currentMonthTotal);
							monthTotalsMonths.add(currentMonthKey);
						}
						currentMonthKey = monthKey;
						currentMonthTotal = 0f;
						firstSessionIdOfMonth = sessionId;
					}
					currentMonthTotal += getWorkHours(this, startTime,
							endTime);
		
				}
				if (currentMonthKey.length() > 0) {
					monthTotals.put(firstSessionIdOfMonth,
							currentMonthTotal);
					monthTotalsMonths.add(currentMonthKey);
				}
			} finally {
				cursor.close();
			}
		}
	}
	
	/**
	 * Clear both totals and add each totals for this project keyed by the id of
	 * the last session object for each period.
	 * 
	 * @param monthTotals
	 *            total hours by the id of last session this month
	 * @param weekTotals
	 *            total hours by the id of last session this week
	 */
	public static void calculateTotals(Context context, long projectId, TreeMap<Long, Float> monthTotals,
			TreeMap<Long, Float> weekTotals) {
		monthTotals.clear();
		weekTotals.clear();
		Cursor cursor = context.getContentResolver().query(
				CONTENT_URI_SESSION,
				new String[] { "_id", "start", "end" },
				"project_id=? and end is not null",
				new String[] { "" + projectId }, "start desc");
		try {
			if (true) {
				String currentMonthKey = "";
				float currentMonthTotal = 0f;
				long firstSessionIdOfMonth = 0;
				String currentWeekKey = "";
				float currentWeekTotal = 0f;
				long firstSessionIdOfWeek = 0;
				while (cursor.moveToNext()) {
					// month
					// week
					long sessionId = cursor.getLong(0);
					long startTime = cursor.getLong(1);
					long endTime = cursor.getLong(2);

					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(startTime);
					String monthKey = cal.get(Calendar.YEAR) + "-"
							+ cal.get(Calendar.MONTH);
					String weekKey = cal.get(Calendar.YEAR) + "-"
							+ cal.get(Calendar.WEEK_OF_YEAR);

					if (!currentMonthKey.equals(monthKey)) {
						// beginning of a new month
						if (currentMonthKey.length() > 0) {
							monthTotals.put(firstSessionIdOfMonth,
									currentMonthTotal);
						}
						currentMonthKey = monthKey;
						currentMonthTotal = 0f;
						firstSessionIdOfMonth = sessionId;
					}
					currentMonthTotal += getWorkHours(context, startTime,
							endTime);

					if (!currentWeekKey.equals(weekKey)) {
						// beginning of a new week
						if (currentWeekKey.length() > 0) {
							weekTotals.put(firstSessionIdOfWeek,
									currentWeekTotal);
						}
						currentWeekKey = weekKey;
						currentWeekTotal = 0f;
						firstSessionIdOfWeek = sessionId;
					}
					currentWeekTotal += getWorkHours(context, startTime,
							endTime);
				}
				if (currentMonthKey.length() > 0) {
					monthTotals.put(firstSessionIdOfMonth,
							currentMonthTotal);
				}
				if (currentWeekKey.length() > 0) {
					weekTotals.put(firstSessionIdOfWeek, currentWeekTotal);
				}
			}
		} finally {
			cursor.close();
		}
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
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.sessionsmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case R.id.addSession:
			addSession();
			return true;
		case R.id.shareSession:
			shareSession();
			return true;
		}
		return false;
	}

	/**
	 * 
	 */
	private void shareSession() {
		Intent i = new Intent(this, ShareProjectReport.class);
		i.setData(ContentUris.withAppendedId(CONTENT_URI_PROJECT, currentProjectId));
		startActivity(i);
	}

	/**
	 * 
	 */

	/**
	 * 
	 */
	private void addSession() {
		ContentValues values = new ContentValues();
		values.put("project_id", currentProjectId);
		long timeMillis = System.currentTimeMillis();
		values.put("start", timeMillis);
		values.put("end", timeMillis);
		Uri newSessionUri = getContentResolver().insert(CONTENT_URI_SESSION,
				values);

		Intent intent = new Intent(this, Session.class);
		intent.setData(newSessionUri);
		startActivity(intent);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.sessioncontextmenu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		super.onContextItemSelected(item);
		switch (item.getItemId()) {
		case R.id.delete_session:
			deleteSession(info.id);
			return true;
		}
		return false;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent intent = new Intent(this, Session.class);
		intent.setData(ContentUris.withAppendedId(CONTENT_URI_SESSION, id));
		startActivity(intent);
	}

	private void deleteSession(long id) {
		getContentResolver().delete(CONTENT_URI_SESSION, "_id=?",
				new String[] { "" + id });
		adjustButtonEnablement();
		clearTotals();
	}

	public class SessionListViewBinder implements
			SimpleCursorAdapter.ViewBinder {
		Calendar cal = Calendar.getInstance();
		
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if (columnIndex == 5 || columnIndex == 6 || columnIndex == 7 || columnIndex == 8) {
				long start = cursor.getLong(0);
				calculateWeekTotals(start);
				calculateMonthTotals(start);
			}
			
			if (columnIndex == 0 || columnIndex == 1) {
				// start or end
				TextView timeView = (TextView) view;
				long time = cursor.getLong(columnIndex);
				if (time == 0) {
					timeView.setText(getString(R.string.project_status_running));
				} else {
					if (columnIndex == 1) {
						// end
						// check if from-to same date
						if (isFromToSameDate(cursor)) {
							// same date - display only time
							timeView.setVisibility(View.VISIBLE);
							timeView.setMaxHeight(100);
							timeView.setText(DateFormat.getTimeFormat(
									Sessions.this).format(time));
						} else {
							// different dates - hide field as date-time will be displayed below
							timeView.setVisibility(View.INVISIBLE);
							timeView.setMaxHeight(0);
						}
					} else {
						// start
						Calendar cal = Calendar.getInstance();
						cal.setTimeInMillis(time);

						timeView.setText(WEEKDAY_MONTHDAY_FORMAT.format(time)
								+ " "
								+ DateFormat.getTimeFormat(Sessions.this)
										.format(time));
					}
				}
				return true;
			} else if (columnIndex == 2) {
				// end if not same date
				TextView timeView = (TextView) view;
				long time = cursor.getLong(1);
				if (time == 0) {
					timeView.setVisibility(View.INVISIBLE);
					timeView.setMaxHeight(0);
				} else {
					if (isFromToSameDate(cursor)) {
						timeView.setVisibility(View.INVISIBLE);
						timeView.setMaxHeight(0);
					} else {
						timeView.setVisibility(View.VISIBLE);
						timeView.setMaxHeight(100);
						timeView.setText(
								WEEKDAY_MONTHDAY_FORMAT.format(time)
								+ " "
								+ DateFormat.getTimeFormat(Sessions.this).format(time));
					}
				}
				return true;
			} else if (columnIndex == 3) {
				// comment
				TextView commentView = (TextView) view;
				String comment = cursor.getString(columnIndex);
				if (comment == null || comment.trim().length() == 0) {
					commentView.setVisibility(View.INVISIBLE);
					commentView.setMaxHeight(0);
				} else {
					commentView.setVisibility(View.VISIBLE);
					commentView.setText(comment);
					commentView.setMaxHeight(100);
				}
				return true;
			} else if (columnIndex == 4) {
				// hours
				TextView hoursView = (TextView) view;
				long startTime = cursor.getLong(0);
				long endTime = cursor.getLong(1);
				if (cursor.isNull(1)) {
					endTime = System.currentTimeMillis();
				}
				float workHours = getWorkHours(Sessions.this, startTime, endTime);
				hoursView.setText(hoursFormat.format(workHours) + getString(R.string.h));
				return true;
			} else if (columnIndex == 5) {
				// month total label
				TextView monthTotalLabelView = (TextView) view;
				long id = cursor.getLong(4);
				long start = cursor.getLong(0);
				if (monthTotals.containsKey(id)) {
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(start);
					monthTotalLabelView
							.setText(dateFormatSymbols.getMonths()[cal
									.get(Calendar.MONTH)]
									+ " " + cal.get(Calendar.YEAR)
									+ " " + getString(R.string.report_date_line_total)
									+ ":");
					monthTotalLabelView.setMaxHeight(1000);
				} else {
					monthTotalLabelView.setMaxHeight(0);
				}
				return true;
			} else if (columnIndex == 6) {
				// month total hours
				TextView monthTotalView = (TextView) view;
				long id = cursor.getLong(4);
				Float total = monthTotals.get(id);
				if (total != null) {
					monthTotalView.setText(hoursFormat.format(total) + getString(R.string.h));
					monthTotalView.setMaxHeight(1000);
				} else {
					monthTotalView.setMaxHeight(0);
				}
				return true;
			} else if (columnIndex == 7) {
				// week total label
				TextView weekTotalLabelView = (TextView) view;
				long id = cursor.getLong(4);
				long start = cursor.getLong(0);
				if (weekTotals.containsKey(id)) {
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(start);
					weekTotalLabelView.setText(getString(R.string.report_date_line_week) 
							+ " " 
							+ cal.get(Calendar.WEEK_OF_YEAR) + " " 
							+ getString(R.string.report_date_line_total)
							+ ":");
					weekTotalLabelView.setMaxHeight(1000);
				} else {
					weekTotalLabelView.setMaxHeight(0);
				}
				return true;
			} else if (columnIndex == 8) {
				// week total hours
				TextView weekTotalView = (TextView) view;
				long id = cursor.getLong(4);
				Float total = weekTotals.get(id);
				if (total != null) {
					weekTotalView.setText(hoursFormat.format(total) + getString(R.string.h));
					weekTotalView.setMaxHeight(1000);
				} else {
					weekTotalView.setMaxHeight(0);
				}
				return true;
			}

			return true;
		}

		private boolean isFromToSameDate(Cursor cursor) {
			cal.setTimeInMillis(cursor.getLong(0));
			int year = cal.get(Calendar.YEAR);
			int month = cal.get(Calendar.MONTH);
			int day = cal.get(Calendar.DAY_OF_MONTH);
			cal.setTimeInMillis(cursor.getLong(1));
			boolean sameDate = year == cal.get(Calendar.YEAR)
					&& month == cal.get(Calendar.MONTH)
					&& day == cal.get(Calendar.DAY_OF_MONTH);
			return sameDate;
		}
	}

	private final SessionListViewBinder viewBinder = new SessionListViewBinder();

	private void showSessions(Cursor cursor) {
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.sessions_list_item, cursor, FROM, TO);
		adapter.setViewBinder(viewBinder);
		setListAdapter(adapter);
	}

	private Cursor getSessions(long projectId) {
		return managedQuery(CONTENT_URI_SESSION, FROM, "project_id=?",
				new String[] { "" + projectId }, "start desc");
	}

	public static float getWorkHours(Context context, long startTime, long endTime) {
		float precision = Settings.getPrecision(context);
		long lunchMsExclusion = 0;
		if (Settings.isExcludeLunchTime(context)) {
			long lunchStart = Settings.getLunchStart(context);
			long lunchEnd = Settings.getLunchEnd(context);

			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(startTime);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			long dayStart = cal.getTimeInMillis();
			while (true) {
				long start = Math.max(startTime, dayStart + lunchStart);
				long end = Math.min(endTime, dayStart + lunchEnd);
				long overlap = Math.max(0, end - start);
				if (overlap == 0) {
					break;
				}
				lunchMsExclusion += overlap;
				dayStart += 24 * 3600 * 1000; // to next day start
			}
		}
		long msTime = endTime - startTime - lunchMsExclusion;
		int roundedTime = (int) ((float) msTime / precision / 3600000f + 0.5f);
		float workHours = roundedTime * precision;
		return workHours;
	}

	private View startButton;
	private View stopButton;
	private String projectName;

	public void onClick(View v) {
			switch (v.getId()) {
			case R.id.StartButton:
				if (startSession(this, currentProjectId)) {
					startButton.setEnabled(false);
					stopButton.setEnabled(true);
				}
				break;
			case R.id.StopButton:
				if (stopSession(this, currentProjectId)) {
					startButton.setEnabled(true);
					stopButton.setEnabled(false);

					clearTotals();
				}
				break;
			}
	}

	/**
	 * start a new session if not any session in progress
	 * @return 
	 */
	public static boolean startSession(Context context, long currentProjectId) {
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(CONTENT_URI_SESSION,
					new String[] { "end" }, "end is null", null, null);
			if (!cursor.moveToNext()) {
				// did not find any session in progress for any project
				ContentValues values = new ContentValues();
				values.put("project_id", currentProjectId);
				values.put("start", System.currentTimeMillis());
				context.getContentResolver().insert(CONTENT_URI_SESSION, values);
				return true;
			}
			return false;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 *  end session in progress for this project, if any
	 * @return
	 */
	public static boolean stopSession(Context context, long currentProjectId) {
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(CONTENT_URI_SESSION,
					new String[] { "_id" }, "project_id=? and end is null",
					new String[] { "" + currentProjectId }, null);
			while (cursor.moveToNext()) {
				// found a session in progress
				long sessionId = cursor.getLong(0);
				ContentValues values = new ContentValues();
				values.put("end", System.currentTimeMillis());
				context.getContentResolver().update(CONTENT_URI_SESSION, values,
						"_id=?", new String[] { "" + sessionId });
				return true;
			}
			return false;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
	
	/**
	 * 
	 */
	private void adjustButtonEnablement() {
		Cursor cursor = getContentResolver().query(CONTENT_URI_SESSION,
				new String[] { "project_id" }, "end is null", null, null);
		try {
			boolean isRunning = false;
			long projectId = 0;
			if (cursor.moveToNext()) {
				isRunning = true;
				projectId = cursor.getLong(0);
			}
			startButton.setEnabled(!isRunning);
			stopButton.setEnabled(isRunning && projectId == currentProjectId);
		} finally {
			cursor.close();
		}
	}
	
	private BroadcastReceiver dbUpdateReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
			adjustButtonEnablement();
			clearTotals();
	    }
	};

}
