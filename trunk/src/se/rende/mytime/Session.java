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

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;

/**
 * Handles one session and let user edit start/stop time and What string.
 *
 * @author Dag Rende
 */
public class Session extends Activity implements OnClickListener {
	private long currentSessionId;
	private AutoCompleteTextView commentView;
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
	private Cursor suggestionCursor;

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
		commentView = (AutoCompleteTextView) findViewById(R.id.SessionCommentEditText);
		
		Cursor cursor = getContentResolver().query(CONTENT_URI_SESSION,
				new String[] { "start", "end", "comment", "project_id" }, "_id=?",
				new String[] { "" + currentSessionId }, null);
		try {
			if (cursor.moveToNext()) {
				// found a session in progress
				startDateTime = cursor.getLong(0);
				endDateTime = cursor.getLong(1);
				comment = cursor.getString(2);
				currentProjectId = cursor.getLong(3);
				projectName = getProjectName(currentProjectId);
			}
		} finally {
			cursor.close();
		}

		setupCommentFieldAutoCompletion();
		showSession();
	}

	/**
	 * Add drop-down to comment field with suggestions from sessions around this one.
	 */
	private void setupCommentFieldAutoCompletion() {
		setSuggestionCursor();
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.autocomplete_listitem, suggestionCursor, new String[] {"comment"}, 
				new int[] {R.id.text1});
		adapter.setCursorToStringConverter(new CursorToStringConverter() {

			public CharSequence convertToString(Cursor cursor) {
				return cursor.getString(1);
			}
		});
		commentView.setThreshold(1);
		commentView.setAdapter(adapter);
	}

	/**
	 * Sets suggestionCursor to the list of comments closest to current session in time.
	 */
	private void setSuggestionCursor() {
		suggestionCursor = new MyTimeData(this).getReadableDatabase().rawQuery(
				"select 0 _id, comment, min(abs(start - ?)) timedist " +
				"from session " +
				"where project_id=? and comment is not null and comment <> '' " +
				"group by comment " +
				"order by timedist", 
				new String[] {"" + startDateTime, "" + currentProjectId});
        if (suggestionCursor != null) {
            startManagingCursor(suggestionCursor);
        }
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		setSuggestionCursor();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		saveSession();
	}
	
	private String getProjectName(long projectId) {
		Cursor projectCursor = getContentResolver().query(CONTENT_URI_PROJECT,
				new String[] { "name" }, "_id=" + projectId, null, null);
		try {
			if (projectCursor.moveToNext()) {
				return projectCursor.getString(0);
			}
			return "";
		} finally {
			projectCursor.close();
		}
	}

	private void showSession() {
		projectNameView.setText(projectName);
		showDateTime();
		commentView.setText(comment);
	}

	private void showDateTime() {
		startDateView.setText(DateFormat.getDateFormat(this).format(startDateTime));
		startTimeView.setText(DateFormat.getTimeFormat(this).format(startDateTime));
		isRunning = (endDateTime == 0);
		endDateView.setText(isRunning ? getString(R.string.project_status_running) : DateFormat.getDateFormat(this).format(endDateTime));
		endTimeView.setText(isRunning ? "" : DateFormat.getTimeFormat(this).format(endDateTime));
	}

	private void saveSession() {
		ContentValues values = new ContentValues();
		values.put("start", startDateTime);
		if (endDateTime != 0) {
			values.put("end", endDateTime);
		}
		values.put("comment", commentView.getText().toString());
		getContentResolver().update(CONTENT_URI_SESSION, values, "_id=?",
				new String[] { "" + currentSessionId });
	}

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
			TimePickerDialog timePickerDialog = new TimePickerDialog(this, new SessionTimeSetListener(true), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), DateFormat.is24HourFormat(this));
			timePickerDialog.show();
		} else if (v == endTimeView) {
			cal.setTimeInMillis(endDateTime);
			TimePickerDialog timePickerDialog = new TimePickerDialog(this, new SessionTimeSetListener(false), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), DateFormat.is24HourFormat(this));
			timePickerDialog.show();
		}
	}
	
	private class SessionDateSetListener implements DatePickerDialog.OnDateSetListener {
		private final boolean isStart;

		public SessionDateSetListener(boolean isStart) {
			this.isStart = isStart;
		}

		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTimeInMillis(isStart ? startDateTime : endDateTime);
			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, monthOfYear);
			cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			if (isStart) {
				if (sameDate(startDateTime, endDateTime)) {
					endDateTime = cal.getTimeInMillis();
				}
				startDateTime = cal.getTimeInMillis();
			} else {
				endDateTime = cal.getTimeInMillis();
			}
			showDateTime();
			setupCommentFieldAutoCompletion();
		}

		/**
		 * true if the two times is on the same date.
		 * @param startDateTime
		 * @param endDateTime
		 * @return true if on same date
		 */
		private boolean sameDate(long startDateTime, long endDateTime) {
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTimeInMillis(startDateTime);
			int startYear = cal.get(Calendar.YEAR);
			int startMonth = cal.get(Calendar.MONTH);
			int startDay = cal.get(Calendar.DAY_OF_MONTH);
			cal.setTimeInMillis(endDateTime);
			return startYear == cal.get(Calendar.YEAR) 
					&& startMonth == cal.get(Calendar.MONTH) 
					&& startDay == cal.get(Calendar.DAY_OF_MONTH);
		}
	}
	
	private class SessionTimeSetListener implements TimePickerDialog.OnTimeSetListener {
		private final boolean isStart;

		public SessionTimeSetListener(boolean isStart) {
			this.isStart = isStart;
		}

		public void onTimeSet(TimePicker view, int hour, int minute) {
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTimeInMillis(isStart ? startDateTime : endDateTime);
			cal.set(Calendar.HOUR_OF_DAY, hour);
			cal.set(Calendar.MINUTE, minute);
			if (isStart) {
				if (sameHour(startDateTime, endDateTime)) {
					endDateTime = cal.getTimeInMillis();
				}
				startDateTime = cal.getTimeInMillis();
			} else {
				endDateTime = cal.getTimeInMillis();
			}
			showDateTime();
			setupCommentFieldAutoCompletion();
		}

		/**
		 * true if the two times is on the same hour.
		 * @param startDateTime
		 * @param endDateTime
		 * @return true if on same hour
		 */
		private boolean sameHour(long startDateTime, long endDateTime) {
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTimeInMillis(startDateTime);
			int startHour = cal.get(Calendar.HOUR_OF_DAY);
			int startMinute = cal.get(Calendar.MINUTE);
			int startSecond = cal.get(Calendar.SECOND);
			cal.setTimeInMillis(endDateTime);
			return startHour == cal.get(Calendar.HOUR_OF_DAY) 
					&& startMinute == cal.get(Calendar.MINUTE) 
					&& startSecond == cal.get(Calendar.SECOND);
		}
	}
}
