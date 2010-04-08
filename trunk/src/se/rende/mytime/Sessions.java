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

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
	private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yy-MM-dd HH:mm");
	private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
	private static final NumberFormat hoursFormat = NumberFormat.getInstance();
	private static final String[] FROM = { "start", "end", "comment", "_id" };
	private static final int[] TO = { R.id.start_date_time, R.id.end_date_time,
			R.id.comment, R.id.hours };
	private long currentProjectId = 1;
	float precision = 0.25f;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Uri intentData = getIntent().getData();
		currentProjectId = Long.parseLong(intentData.getLastPathSegment());	

		setContentView(R.layout.sessions);
		
		TextView projectNameView = (TextView) findViewById(R.id.sessionsProjectName);
		projectNameView.setText(getProjectName());
		
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

	/**
	 * @return
	 */
	private String getProjectName() {
		Cursor cursor = getContentResolver().query(CONTENT_URI_PROJECT,
				new String[] { "name" }, "_id=" + currentProjectId, null, null);
		if (cursor.moveToNext()) {
			return cursor.getString(0);
		}
		return "";
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
		}
		return false;
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
		Uri newSessionUri = getContentResolver().insert(CONTENT_URI_SESSION, values);
		
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
							timeView.setText(timeFormat.format(time));
						} else {
							timeView.setText(dateTimeFormat.format(time));
						}
					} else {
						timeView.setText(dateTimeFormat.format(time));
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
				long endTime = cursor.getLong(1);
				if (cursor.isNull(1)) {
					endTime = System.currentTimeMillis();
				}
				long msTime = endTime - cursor.getLong(0);
				TextView hoursView = (TextView) view;
				int roundedTime = (int)((float)msTime / precision / 3600000f + 0.5f);
				hoursView.setText(hoursFormat.format(roundedTime * precision) + "h");
				return true;
			}

			return false;
		}

	}
	
	private final SessionListViewBinder viewBinder = new SessionListViewBinder();
	private View startButton;
	private View stopButton;

	private void showSessions(Cursor cursor) {
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.session_list_item, cursor, FROM, TO);
		adapter.setViewBinder(viewBinder);
		setListAdapter(adapter);
	}
	
	private Cursor getSessions(long projectId) {
		return managedQuery(CONTENT_URI_SESSION, FROM, "project_id=?",
				new String[] { "" + projectId }, "start desc");
	}

	@Override
	public void onClick(View v) {
		Cursor cursor = null;
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
			}
			break;
		}
	}
	
	/**
	 * 
	 */
	private void adjustButtonEnablement() {
		Cursor cursor = getContentResolver().query(CONTENT_URI_SESSION,
				new String[] { "project_id" }, "end is null", null, null);
		boolean isRunning = false;
		long projectId = 0;
		if (cursor.moveToNext()) {
			isRunning = true;
			projectId = cursor.getLong(0);
		}
		startButton.setEnabled(!isRunning);
		stopButton.setEnabled(isRunning && projectId == currentProjectId);
	}
}
