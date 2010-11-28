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

import java.util.ArrayList;
import java.util.List;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Timesheet application for android. 
 * Project list. Click a project to open it in Sessions activity.
 * 
 * @author Dag Rende April 1, 2010
 */
public class MyTime extends ListActivity {
	private static final String[] FROM = { "name", "_id" };
	private static final int[] TO = { R.id.name, R.id.run_indicator };
	private long runningProjectId = 0;
	private final ProjectListViewBinder viewBinder = new ProjectListViewBinder();
	List<ResolveInfo> plugIns = new ArrayList<ResolveInfo>();
	private GoogleAnalyticsTracker tracker = null;
	private IntentFilter dbUpdateFilter;
	private Cursor projectListCursor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	    setupGATracker();
	    
	    setContentView(R.layout.main);
		getListView().setOnCreateContextMenuListener(this);
		runningProjectId = getRunningProjectId();
		projectListCursor = getProjects();
		showProjects(projectListCursor);

		scanPlugIns();
	    dbUpdateFilter = new IntentFilter(Constants.INTENT_DB_UPDATE_ACTION);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		tracker.stop();
	}

	/**
	 * Returns a cursor to the projects in alphabetical order.
	 * @return the project cirsor
	 */
	private Cursor getProjects() {
		return managedQuery(CONTENT_URI_PROJECT, FROM, null, null, "name asc");
	}

	/**
	 * Shows the projects by adapting the provided cursor to the list of this activity. 
	 * It also makes the running project line marked using a view binder.
	 * @param cursor
	 */
	private void showProjects(Cursor cursor) {
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.project_list_item, cursor, FROM, TO);
		adapter.setViewBinder(viewBinder);
		setListAdapter(adapter);
	}
		

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.projmenu, menu);    
		
		// add a menu for each plug-in
        SubMenu subMenu = null;
        for (ResolveInfo info : plugIns) {
        	if (subMenu == null) {
        		subMenu = menu.addSubMenu(getString(R.string.projects_menu_other));
        	}
			Intent launch = new Intent(Intent.ACTION_MAIN);
			launch.addCategory(Intent.CATEGORY_LAUNCHER);
			ActivityInfo activityInfo = info.activityInfo;
			launch.setComponent(new ComponentName(activityInfo.applicationInfo.packageName, activityInfo.name));
			CharSequence title = info.loadLabel(getPackageManager());
			String appName = getString(R.string.app_name);
			if (title.toString().startsWith(appName)) {
				title = title.toString().substring(appName.length()).trim();
			}
			subMenu.add(Menu.NONE, plugIns.size(), Menu.NONE, title).setIntent(launch);
		}
        return true;
	}

	private void scanPlugIns() {
		Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("se.rende.mytime.PLUGIN");
        plugIns = getPackageManager().queryIntentActivities(intent, 0);
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

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent intent = new Intent(this, Sessions.class);
		intent.setData(ContentUris.withAppendedId(CONTENT_URI_PROJECT, id));
		startActivity(intent);
	}
	
	/**
	 * Returns the id of the project that has a sesions that is running (has null end time).
	 * 
	 * @return if of running project or 0 if none.
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
	protected void onPause() {
		unregisterReceiver(dbUpdateReceiver);
		super.onPause();
	}

	@Override
	protected void onResume() {
		registerReceiver(dbUpdateReceiver, dbUpdateFilter);
		refreshList();
		super.onResume();
	}

	private void refreshList() {
		runningProjectId = getRunningProjectId();
		projectListCursor.requery();
	}

	public class ProjectListViewBinder implements
			SimpleCursorAdapter.ViewBinder {
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if (columnIndex == 1) {
				TextView runIndicatorView = (TextView) view;
				runIndicatorView.setText(cursor.getLong(1) == runningProjectId ? getString(R.string.project_status_running) : "");
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
					public void onClick(DialogInterface dialog, int which) {
						addProject(nameEditText.getText().toString());
					}
				}).setNegativeButton(getString(R.string.cancel_button_label),
				new OnClickListener() {
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
							public void onClick(DialogInterface dialog, int which) {
								setProjectName(id, nameEditText.getText()
										.toString());
							}
						}).setNegativeButton(
						getString(R.string.cancel_button_label),
						new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						}).show();

			}
		} finally {
			cursor.close();
		}
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

	private void setupGATracker() {
		tracker = GoogleAnalyticsTracker.getInstance();
	    tracker.start("UA-17614355-1", this);
	    tracker.trackPageView("/start");
	    tracker.dispatch();
	}
		
	private BroadcastReceiver dbUpdateReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
			refreshList();
	    }
	};
}