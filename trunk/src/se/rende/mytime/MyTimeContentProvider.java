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

import static android.provider.BaseColumns._ID;
import static se.rende.mytime.Constants.AUTHORITY;
import static se.rende.mytime.Constants.CONTENT_URI_PROJECT;
import static se.rende.mytime.Constants.CONTENT_URI_SESSION;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Defines the database data.
 *
 * @author Dag Rende
 */
public class MyTimeContentProvider extends ContentProvider {
	private static final int PROJECTS = 1;
	private static final int PROJECTS_ID = 2;
	private static final int SESSIONS = 3;
	private static final int SESSIONS_ID = 4;
	private static final String PROJECT_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.se.rende.mytime.project";
	private static final String PROJECT_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.se.rende.mytime.project";
	private static final String SESSION_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.se.rende.mytime.session";
	private static final String SESSION_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.se.rende.mytime.session";

	private MyTimeData myTimeData;
	private UriMatcher uriMatcher;

	@Override
	public boolean onCreate() {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, "project", PROJECTS);
		uriMatcher.addURI(AUTHORITY, "project/#", PROJECTS_ID);
		uriMatcher.addURI(AUTHORITY, "session", SESSIONS);
		uriMatcher.addURI(AUTHORITY, "session/#", SESSIONS_ID);
		myTimeData = new MyTimeData(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String orderBy) {
		Cursor cursor = null;
		int match = uriMatcher.match(uri);
		if (match == PROJECTS_ID || match == PROJECTS) {
			if (match == PROJECTS_ID) {
				long id = Long.parseLong(uri.getPathSegments().get(1));
				selection = appendRowId(selection, id);
			}
			// Get the database and run the query
			SQLiteDatabase db = myTimeData.getReadableDatabase();
			cursor = db.query("project", projection, selection,
					selectionArgs, null, null, orderBy);
		} else if (match == SESSIONS_ID || match == SESSIONS) {
			if (match == SESSIONS_ID) {
				long id = Long.parseLong(uri.getPathSegments().get(1));
				selection = appendRowId(selection, id);
			}
			// Get the database and run the query
			SQLiteDatabase db = myTimeData.getReadableDatabase();
			cursor = db.query("session", projection, selection,
					selectionArgs, null, null, orderBy);
		} else {
			throw new IllegalArgumentException("Unknown URI " + uri + " match=" + match);
		}

		// Tell the cursor what uri to watch, so it knows when its
		// source data changes
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case PROJECTS:
			return PROJECT_CONTENT_TYPE;
		case PROJECTS_ID:
			return PROJECT_CONTENT_ITEM_TYPE;
		case SESSIONS:
			return SESSION_CONTENT_TYPE;
		case SESSIONS_ID:
			return SESSION_CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Uri newUri = null;
		SQLiteDatabase db = myTimeData.getWritableDatabase();

		// Validate the requested uri
		if (uriMatcher.match(uri) == PROJECTS) {
			// Insert into database
			long id = db.insertOrThrow("project", null, values);
			
			// Notify any watchers of the change
			newUri = ContentUris.withAppendedId(CONTENT_URI_PROJECT, id);
		} else if (uriMatcher.match(uri) == SESSIONS) {	
			// Insert into database
			long id = db.insertOrThrow("session", null, values);
			
			// Notify any watchers of the change
			newUri = ContentUris.withAppendedId(CONTENT_URI_SESSION, id);
		} else {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(newUri, null);
		return newUri;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = myTimeData.getWritableDatabase();
		int count;
		long id;
		switch (uriMatcher.match(uri)) {
		case PROJECTS:
			count = db.delete("project", selection, selectionArgs);
			break;
		case PROJECTS_ID:
			id = Long.parseLong(uri.getPathSegments().get(1));
			db.delete("session", "project_id=" + id, null);
			String whereClause = appendRowId(selection, id);
			count = db.delete("project", whereClause, selectionArgs);
			break;
		case SESSIONS:
			count = db.delete("session", selection, selectionArgs);
			break;
		case SESSIONS_ID:
			id = Long.parseLong(uri.getPathSegments().get(1));
			count = db.delete("session", appendRowId(selection, id), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// Notify any watchers of the change
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = myTimeData.getWritableDatabase();
		int count;
		long id;
		switch (uriMatcher.match(uri)) {
		case PROJECTS:
			count = db.update("project", values, selection, selectionArgs);
			break;
		case PROJECTS_ID:
			id = Long.parseLong(uri.getPathSegments().get(1));
			count = db.update("project", values, appendRowId(selection, id),
					selectionArgs);
			break;
		case SESSIONS:
			count = db.update("session", values, selection, selectionArgs);
			break;
		case SESSIONS_ID:
			id = Long.parseLong(uri.getPathSegments().get(1));
			count = db.update("session", values, appendRowId(selection, id),
					selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// Notify any watchers of the change
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	/** Append an id test to a SQL selection expression */
	private String appendRowId(String selection, long id) {
		return _ID
				+ "="
				+ id
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')'
						: "");
	}

}
