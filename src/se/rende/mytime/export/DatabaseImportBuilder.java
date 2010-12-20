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
package se.rende.mytime.export;

import static se.rende.mytime.Constants.CONTENT_URI_PROJECT;
import static se.rende.mytime.Constants.CONTENT_URI_SESSION;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * Bulder that creates projects and session in the My Time database.
 * @author Dag Rende
 */
public class DatabaseImportBuilder implements ImportBuilder {

	private int projectId = 0;
	private final ContentResolver contentResolver;

	public DatabaseImportBuilder(ContentResolver contentResolver) {
		this.contentResolver = contentResolver;
	}

	@Override
	public long createProject(String projectName) {
		Log.d("MyTimeLoggingImportBuilder", "MyTimeLoggingImportBuilder.createProject(" + projectName + ")");
		ContentValues values = new ContentValues();
		values.put("name", "imported " + projectName);
		Uri newProject = contentResolver.insert(CONTENT_URI_PROJECT, values);
		Cursor cursor = contentResolver.query(newProject, new String[] {"_id"}, null, null, null);
		if (cursor.moveToNext()) {
			return cursor.getLong(0);
		}
		return -1;
	}

	@Override
	public void createSession(long projectId, String startTime, String endTime,
			String comment) {
		Log.d("MyTimeLoggingImportBuilder", "MyTimeLoggingImportBuilder.createSession(" 
				+ projectId + ", " + startTime + ", " + endTime + ", " + comment + ")");
		ContentValues values = new ContentValues();
		values.put("project_id", projectId);
		values.put("start", startTime);
		if (endTime != null) {
			values.put("end", endTime);
		}
		if (comment != null) {
			values.put("comment", comment);
		}
		contentResolver.insert(CONTENT_URI_SESSION,	values);
	}

}
