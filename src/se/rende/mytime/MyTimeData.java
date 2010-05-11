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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Definies the SQLite database.
 *
 * @author Dag Rende
 */
public class MyTimeData extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "mytime.db";
	private static final int DATABASE_VERSION = 3;

	public MyTimeData(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table project (" +
				"_id integer primary key autoincrement," +
				"name text" +
				");");
		createSessionTable(db);
	}

	private void createSessionTable(SQLiteDatabase db) {
		db.execSQL("create table session (" +
				"_id integer primary key autoincrement," +
				"project_id integer not null REFERENCES artist(artistid)," +
				"start integer," +
				"end integer," +
				"comment text" +
				");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion != newVersion) {
			for (int i = oldVersion; i < newVersion; i++) {
				switch (oldVersion) {
				case 1:
					// no changes
					break;
				case 2:
					createSessionTable(db);
					break;
				default:
					break;
				}
			}
		}
	}

}
