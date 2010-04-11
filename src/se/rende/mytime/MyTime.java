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
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Timesheet application for android. Add a number of projects, and then
 * start/stop session timer on a project and get list of sessions with week and
 * month sums.
 * 
 * @author Dag Rende April 1, 2010
 */
public class MyTime extends ListActivity {
	private static final String[] FROM = { "name", "_id" };
	private static final int[] TO = { R.id.name, R.id.run_indicator };
	private long runningProjectId = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		getListView().setOnCreateContextMenuListener(this);
		runningProjectId = getRunningProjectId();
		showProjects(getProjects());
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		runningProjectId = getRunningProjectId();
	}

	/**
	 * @return
	 */
	private long getRunningProjectId() {
		Cursor cursor = getContentResolver().query(CONTENT_URI_SESSION,
				new String[] { "project_id" }, "end is null", null, null);
		try {
			if (cursor.moveToNext()) {
				return cursor.getLong(0);
			}
			
			return 0;
		} finally {
			cursor.close();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.projmenu, menu);
		return true;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent intent = new Intent(this, Sessions.class);
		intent.setData(ContentUris.withAppendedId(CONTENT_URI_PROJECT, id));
		startActivity(intent);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case R.id.addProj:
			addProject();
			return true;
		case R.id.settings_menu:
			startActivity(new Intent(this, Settings.class));
			return true;
		case R.id.about:
			startActivity(new Intent(this, About.class));
			return true;
		}
		return false;
	}

	private final ProjectListViewBinder viewBinder = new ProjectListViewBinder();

	public class ProjectListViewBinder implements
			SimpleCursorAdapter.ViewBinder {
		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if (columnIndex == 1) {
				TextView runIndicatorView = (TextView) view;
				runIndicatorView.setText(cursor.getLong(1) == runningProjectId ? "running" : "");
				return true;
			}
			return false;
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.projcontextmenu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		super.onContextItemSelected(item);
		switch (item.getItemId()) {
		case R.id.delete_project:
			deleteProject(info.id);
			return true;
		case R.id.edit_project:
			editProjectName(info.id);
			return true;
		}
		return false;
	}

	private void addProject() {
		final EditText nameEditText = new EditText(this);

		new AlertDialog.Builder(this).setMessage(
				getString(R.string.new_project_name_label)).setView(
				nameEditText).setPositiveButton(
				getString(R.string.ok_button_label), new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						addProject(nameEditText.getText().toString());
					}
				}).setNegativeButton(getString(R.string.cancel_button_label),
				new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).show();

	}

	private void deleteProject(long id) {
		Uri newUri = ContentUris.withAppendedId(CONTENT_URI_PROJECT, id);
		getContentResolver().delete(newUri, null, null);
	}

	private void editProjectName(final long id) {
		Cursor cursor = getContentResolver().query(CONTENT_URI_PROJECT,
				new String[] { "name" }, "_id=?", new String[] { "" + id },
				null);
		try {
			if (cursor.moveToNext()) {
				String projectName = cursor.getString(0);
				final EditText nameEditText = new EditText(this);
				nameEditText.setText(projectName);

				new AlertDialog.Builder(this).setMessage(
						getString(R.string.new_project_name_label)).setView(
						nameEditText).setPositiveButton(
						getString(R.string.ok_button_label), new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								setProjectName(id, nameEditText.getText()
										.toString());
							}
						}).setNegativeButton(
						getString(R.string.cancel_button_label),
						new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						}).show();

			}
		} finally {
			cursor.close();
		}
	}

	private Cursor getProjects() {
		return managedQuery(CONTENT_URI_PROJECT, FROM, null, null, "name asc");
	}

	private void showProjects(Cursor cursor) {
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.project_list_item, cursor, FROM, TO);
		adapter.setViewBinder(viewBinder);
		setListAdapter(adapter);
	}

	private void addProject(String projName) {
		ContentValues values = new ContentValues();
		values.put("name", projName);
		getContentResolver().insert(CONTENT_URI_PROJECT, values);
	}

	private void setProjectName(long id, String projectName) {
		ContentValues values = new ContentValues();
		values.put("name", projectName);
		getContentResolver().update(CONTENT_URI_PROJECT, values, "_id=?",
				new String[] { "" + id });
	}
}