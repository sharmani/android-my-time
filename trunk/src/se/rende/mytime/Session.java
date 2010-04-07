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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

/**
 * Handles one session and let user edit start/stop time and What string.
 *
 * @author Dag Rende
 */
public class Session extends Activity implements OnClickListener {
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
	"yy-MM-dd");
	private static final SimpleDateFormat timeFormat = new SimpleDateFormat(
	"HH:mm");
	private long currentSessionId;
	private EditText commentView;
	private boolean isRunning;
	private Button startDateView;
	private Button startTimeView;
	private Button endDateView;
	private Button endTimeView;
	private long startDateTime;
	private long endDateTime;
	private String comment;
	private long currentProjectId;
	private String projectName;
	private TextView projectNameView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Uri intentData = getIntent().getData();
		currentSessionId = Long.parseLong(intentData.getLastPathSegment());
		setContentView(R.layout.session);

		projectNameView = (TextView) findViewById(R.id.sessionProjectName);
		startDateView = (Button) findViewById(R.id.SessionStartDate);
		startDateView.setOnClickListener(this);
		startTimeView = (Button) findViewById(R.id.SessionStartTime);
		startTimeView.setOnClickListener(this);
		endDateView = (Button) findViewById(R.id.SessionEndDate);
		endDateView.setOnClickListener(this);
		endTimeView = (Button) findViewById(R.id.SessionEndTime);
		endTimeView.setOnClickListener(this);
		commentView = (EditText) findViewById(R.id.SessionCommentEditText);

		Cursor cursor = getContentResolver().query(CONTENT_URI_SESSION,
				new String[] { "start", "end", "comment", "project_id" }, "_id=?",
				new String[] { "" + currentSessionId }, null);
		if (cursor.moveToNext()) {
			// found a session in progress
			startDateTime = cursor.getLong(0);
			endDateTime = cursor.getLong(1);
			comment = cursor.getString(2);
			currentProjectId = cursor.getLong(3);
			projectName = getProjectName(currentProjectId);
		}
		showSession();
		
	}
	
	private String getProjectName(long projectId) {
		Cursor projectCursor = getContentResolver().query(CONTENT_URI_PROJECT,
				new String[] { "name" }, "_id=" + projectId, null, null);
		if (projectCursor.moveToNext()) {
			return projectCursor.getString(0);
		}
		return "";
	}

	private void showSession() {
		projectNameView.setText(projectName);
		startDateView.setText(dateFormat.format(startDateTime));
		startTimeView.setText(timeFormat.format(startDateTime));
		isRunning = (endDateTime == 0);
		endDateView.setText(isRunning ? "running" : dateFormat
				.format(endDateTime));
		endTimeView.setText(isRunning ? "" : timeFormat
				.format(endDateTime));
		commentView.setText(comment);
	}

	@Override
	protected void onPause() {
		super.onPause();
		ContentValues values = new ContentValues();
		values.put("start", startDateTime);
		if (endDateTime != 0) {
			values.put("end", endDateTime);
		}
		values.put("comment", commentView.getText().toString());
		getContentResolver().update(CONTENT_URI_SESSION, values, "_id=?",
				new String[] { "" + currentSessionId });
	}

	@Override
	public void onClick(View v) {
		if ((v == endDateView || v == endTimeView) && isRunning) {
			return;
		}
		Calendar cal = GregorianCalendar.getInstance();
		if (v == startDateView) {
			cal.setTimeInMillis(startDateTime);
			DatePickerDialog datePickerDialog = new DatePickerDialog(this, new SessionDateSetListener(true), cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
			datePickerDialog.show();
		} else if (v == endDateView) {
			cal.setTimeInMillis(endDateTime);
			DatePickerDialog datePickerDialog = new DatePickerDialog(this, new SessionDateSetListener(false), cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
			datePickerDialog.show();
		} else if (v == startTimeView) {
			cal.setTimeInMillis(startDateTime);
			TimePickerDialog timePickerDialog = new TimePickerDialog(this, new SessionTimeSetListener(true), cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE), true);
			timePickerDialog.show();
		} else if (v == endTimeView) {
			cal.setTimeInMillis(endDateTime);
			TimePickerDialog timePickerDialog = new TimePickerDialog(this, new SessionTimeSetListener(false), cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE), true);
			timePickerDialog.show();
		}
	}
	
	private class SessionDateSetListener implements DatePickerDialog.OnDateSetListener {
		private final boolean isStart;

		public SessionDateSetListener(boolean isStart) {
			this.isStart = isStart;
		}

		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTimeInMillis(isStart ? startDateTime : endDateTime);
			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, monthOfYear);
			cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			if (isStart) {
				startDateTime = cal.getTimeInMillis();
			} else {
				endDateTime = cal.getTimeInMillis();
			}
			showSession();
		}
	}
	
	private class SessionTimeSetListener implements TimePickerDialog.OnTimeSetListener {
		private final boolean isStart;

		public SessionTimeSetListener(boolean isStart) {
			this.isStart = isStart;
		}

		@Override
		public void onTimeSet(TimePicker view, int hour, int minute) {
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTimeInMillis(isStart ? startDateTime : endDateTime);
			cal.set(Calendar.HOUR, hour);
			cal.set(Calendar.MINUTE, minute);
			if (isStart) {
				startDateTime = cal.getTimeInMillis();
			} else {
				endDateTime = cal.getTimeInMillis();
			}
			showSession();
		}
	}
}
