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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.TreeMap;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Displays sessions for the project described by the item Url in intent data.
 * let user start stop new sessions and edit or remove listed sessions.
 * 
 * @author Dag Rende
 */
public class Sessions extends ListActivity implements OnClickListener {
	private static final NumberFormat hoursFormat = NumberFormat.getInstance();
	private static final String[] FROM = { "start", "end", "comment", "_id",
			"_id+1", "_id+2", "_id+3", "_id+4" };
	private static final int[] TO = { R.id.start_date_time, R.id.end_date_time,
			R.id.comment, R.id.hours, R.id.month_total_label, R.id.month_total,
			R.id.week_total_label, R.id.week_total };
	private long currentProjectId = 1;
	private float precision = 0.25f;
	private TreeMap<Long, Float> monthTotals = new TreeMap<Long, Float>();
	private TreeMap<Long, Float> weekTotals = new TreeMap<Long, Float>();
	private DateFormatSymbols dateFormatSymbols = new DateFormatSymbols();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
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

		precision = Settings.getPrecision(this);
		hoursFormat.setMaximumFractionDigits(5);

		showSessions(getSessions(currentProjectId));

		adjustButtonEnablement();
	}

	@Override
	protected void onResume() {
		super.onResume();
		calculateTotals(monthTotals, weekTotals);
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
	private void calculateTotals(TreeMap<Long, Float> monthTotals,
			TreeMap<Long, Float> weekTotals) {
		monthTotals.clear();
		weekTotals.clear();
		Cursor cursor = getContentResolver().query(CONTENT_URI_SESSION,
				new String[] { "_id", "start", "end" },
				"project_id=? and end is not null",
				new String[] { "" + currentProjectId }, "start desc");
		try {
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
				currentMonthTotal += getWorkHours(startTime, endTime);

				if (!currentWeekKey.equals(weekKey)) {
					// beginning of a new week
					if (currentWeekKey.length() > 0) {
						weekTotals.put(firstSessionIdOfWeek, currentWeekTotal);
					}
					currentWeekKey = weekKey;
					currentWeekTotal = 0f;
					firstSessionIdOfWeek = sessionId;
				}
				currentWeekTotal += getWorkHours(startTime, endTime);
			}
			if (currentMonthKey.length() > 0) {
				monthTotals.put(firstSessionIdOfMonth, currentMonthTotal);
			}
			if (currentWeekKey.length() > 0) {
				weekTotals.put(firstSessionIdOfWeek, currentWeekTotal);
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
		try {
			String fileName = getString(R.string.time_report) + " " + projectName + " " + DateFormat.getDateFormat(this).format(System.currentTimeMillis()).replace('/', '-');
			File reportFile = new File(Environment.getExternalStorageDirectory(), fileName + ".html");
			FileOutputStream os = new FileOutputStream(reportFile);
			writeHtmlReport(os);
			os.close();

			Intent i=new Intent(android.content.Intent.ACTION_SEND);
			i.putExtra(Intent.EXTRA_SUBJECT, fileName);
			i.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + reportFile));
			i.setType("text/html");
			startActivity(Intent.createChooser(i, getString(R.string.share_title)));
		} catch (Exception e) {
			new AlertDialog.Builder(this)
		      .setMessage("backup error " + e)
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
		Cursor sessionCursor = null;
		try {
			sessionCursor = getContentResolver().query(CONTENT_URI_SESSION,
					new String[] { "_id", "start", "end", "comment" }, 
					"project_id=? and end is not null", new String[] {"" + currentProjectId}, "start asc");
			String lastDate = null;
			long lastId = -1;
			float daySum = 0f;
			StringBuilder comments = new StringBuilder();
			pw.println("<html><body><table>");
			pw.println("<tr><td>date</td><td>day</td><td>comment</td><td>week</td><td>month</td></tr>");
			while (sessionCursor.moveToNext()) {
				long id = sessionCursor.getLong(0);
				long startTime = sessionCursor.getLong(1);
				long endTime = sessionCursor.getLong(2);
				String comment = sessionCursor.getString(3);
				
				String dateString = DateFormat.getDateFormat(this).format(startTime);
				if (lastDate == null) {
					lastDate = dateString;
				} else if (!lastDate.equals(dateString)) {
			    	printDateLine(pw, comments.toString(), lastDate, daySum, lastId);
					lastDate = dateString;
					daySum = 0f;
					comments.setLength(0);
			    }
			    daySum += getWorkHours(startTime, endTime);
		    	if (comment != null && comment.length() > 0) {
		    		if (comments.length() > 0 && comment.length() > 0) {
		    			comments.append(", ");
		    		}
		    		comments.append(comment);
		    	}
		    	lastId = id;
			}
			if (lastId != -1) {
		    	printDateLine(pw, comments.toString(), lastDate, daySum, lastId);
			}
			pw.println("</table></body></html>");
		} finally {
			if (sessionCursor != null) {
				sessionCursor.close();
			}
		}
		pw.flush();
	}

	private void printDateLine(PrintWriter pw, String comments, String dateString, float workHours, long lastId) {
		pw.print("<tr><td>" + dateString + "</td><td>" + hoursFormat.format(workHours) + "</td><td>");
		if (comments.length() > 0) {
			pw.print(comments);
		}
		pw.print("</td>");
		
		Float weekTotal = weekTotals.get(lastId);
		pw.print("<td>");
		if (weekTotal != null) {
			pw.print(hoursFormat.format(weekTotal));
		}
		pw.print("</td>");
		
		Float monthTotal = monthTotals.get(lastId);
		pw.print("<td>");
		if (monthTotal != null) {
			pw.print(hoursFormat.format(monthTotal));
		}
		pw.println("</td>");
	}

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
		calculateTotals(monthTotals, weekTotals);
	}

	public class SessionListViewBinder implements
			SimpleCursorAdapter.ViewBinder {
		Calendar cal = Calendar.getInstance();

		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if (columnIndex == 0 || columnIndex == 1) {
				TextView timeView = (TextView) view;
				long time = cursor.getLong(columnIndex);
				if (time == 0) {
					timeView.setText("running");
				} else {
					if (columnIndex == 1) {
						// check if from-to same date
						cal.setTimeInMillis(cursor.getLong(0));
						int year = cal.get(Calendar.YEAR);
						int month = cal.get(Calendar.MONTH);
						int day = cal.get(Calendar.DAY_OF_MONTH);
						cal.setTimeInMillis(cursor.getLong(1));
						if (year == cal.get(Calendar.YEAR)
								&& month == cal.get(Calendar.MONTH)
								&& day == cal.get(Calendar.DAY_OF_MONTH)) {
							// same date - display only time
							timeView.setText(DateFormat.getTimeFormat(
									Sessions.this).format(time));
						} else {
							timeView.setText(DateFormat.getDateFormat(
									Sessions.this).format(time)
									+ " "
									+ DateFormat.getTimeFormat(Sessions.this)
											.format(time));
						}
					} else {
						timeView.setText(DateFormat
								.getDateFormat(Sessions.this).format(time)
								+ " "
								+ DateFormat.getTimeFormat(Sessions.this)
										.format(time));
					}
				}
				return true;
			} else if (columnIndex == 2) {
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
			} else if (columnIndex == 3) {
				TextView hoursView = (TextView) view;
				long startTime = cursor.getLong(0);
				long endTime = cursor.getLong(1);
				if (cursor.isNull(1)) {
					endTime = System.currentTimeMillis();
				}
				float workHours = getWorkHours(startTime, endTime);
				hoursView.setText(hoursFormat.format(workHours) + getString(R.string.h));
				return true;
			} else if (columnIndex == 4) {
				TextView monthTotalLabelView = (TextView) view;
				long id = cursor.getLong(3);
				long start = cursor.getLong(0);
				if (monthTotals.containsKey(id)) {
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(start);
					monthTotalLabelView
							.setText(dateFormatSymbols.getMonths()[cal
									.get(Calendar.MONTH)]
									+ " total:");
					monthTotalLabelView.setMaxHeight(1000);
				} else {
					monthTotalLabelView.setMaxHeight(0);
				}
				return true;
			} else if (columnIndex == 5) {
				TextView monthTotalView = (TextView) view;
				long id = cursor.getLong(3);
				Float total = monthTotals.get(id);
				if (total != null) {
					monthTotalView.setText(hoursFormat.format(total) + getString(R.string.h));
					monthTotalView.setMaxHeight(1000);
				} else {
					monthTotalView.setMaxHeight(0);
				}
				return true;
			} else if (columnIndex == 6) {
				TextView weekTotalLabelView = (TextView) view;
				long id = cursor.getLong(3);
				long start = cursor.getLong(0);
				if (weekTotals.containsKey(id)) {
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(start);
					weekTotalLabelView.setText("Week "
							+ cal.get(Calendar.WEEK_OF_YEAR) + " total:");
					weekTotalLabelView.setMaxHeight(1000);
				} else {
					weekTotalLabelView.setMaxHeight(0);
				}
				return true;
			} else if (columnIndex == 7) {
				TextView weekTotalView = (TextView) view;
				long id = cursor.getLong(3);
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

	private float getWorkHours(long startTime, long endTime) {
		long lunchMsExclusion = 0;
		if (Settings.isExcludeLunchTime(this)) {
			long lunchStart = Settings.getLunchStart(this);
			long lunchEnd = Settings.getLunchEnd(this);

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

	@Override
	public void onClick(View v) {
		Cursor cursor = null;
		try {
			switch (v.getId()) {
			case R.id.StartButton:
				// start a new session if not any session in progress
				cursor = getContentResolver().query(CONTENT_URI_SESSION,
						new String[] { "end" }, "end is null", null, null);
				if (!cursor.moveToNext()) {
					// did not find any session in progress for any project
					ContentValues values = new ContentValues();
					values.put("project_id", currentProjectId);
					values.put("start", System.currentTimeMillis());
					getContentResolver().insert(CONTENT_URI_SESSION, values);
					startButton.setEnabled(false);
					stopButton.setEnabled(true);
				}
				break;

			case R.id.StopButton:
				// end session in progress for this project, if any
				cursor = getContentResolver().query(CONTENT_URI_SESSION,
						new String[] { "_id" }, "project_id=? and end is null",
						new String[] { "" + currentProjectId }, null);
				while (cursor.moveToNext()) {
					// found a session in progress
					long sessionId = cursor.getLong(0);
					ContentValues values = new ContentValues();
					values.put("end", System.currentTimeMillis());
					getContentResolver().update(CONTENT_URI_SESSION, values,
							"_id=?", new String[] { "" + sessionId });
					startButton.setEnabled(true);
					stopButton.setEnabled(false);

					calculateTotals(monthTotals, weekTotals);
				}
				break;
			}
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
}
