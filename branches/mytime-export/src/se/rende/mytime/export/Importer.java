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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

/**
 * Import an exported file.
 *
        <activity android:name=".Importer"
                  android:label="@string/activity_name_import" 
                  android:theme="@android:style/Theme.Light">
            <intent-filter>
                <category android:name="android.intent.category.DEFAULT" />
                <action android:name="android.intent.action.VIEW" />
                <data android:mimeType="text/xml" />
            </intent-filter>
        </activity>

 * 
 * @author Dag Rende
 */
public class Importer extends Activity {
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	/**
	 *
	 * @author Dag Rende
	 */
	public class DatabaseSyntaxError extends Exception {
		public DatabaseSyntaxError(String msg) {
			super(msg);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.importer);
		InputStream is = null;
		try {
		String scheme = getIntent().getScheme();
		if (scheme != null && scheme.equals("content")) {
				is = getContentResolver().openInputStream(getIntent().getData());
		} else {
			// assume we started from home page
			File backupFile = new File(Environment
					.getExternalStorageDirectory(), "my-time-export.xml");
			is = new FileInputStream(backupFile);
		}
		
		ImportBuilder ib = new DatabaseImportBuilder(getContentResolver());
		importFromStream(is, ib);
		} catch (Exception e) {
			new AlertDialog.Builder(this)
			.setMessage("Import error " + e)
			.show();
			Log.d("importer", "input", e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public void importFromStream(InputStream is, ImportBuilder ib) throws XmlPullParserException,
			DatabaseSyntaxError, IOException {
		XmlPullParser parser = Xml.newPullParser();
		// auto-detect the encoding from the stream
		parser.setInput(is, null);
		parser.next();
		int eventType = parser.getEventType();
		if (eventType == XmlPullParser.START_TAG) {
			
			String documentTag = "my-time-backup";
			if (!documentTag.equals(parser.getName())) {
				throw new DatabaseSyntaxError("invalid document element '" + parser.getName() + "', should be '" + documentTag + "'");
			}
			String ver = parser.getAttributeValue(null, "ver");
			if (!"1".equals(ver)) {
				throw new DatabaseSyntaxError("wrong format version '" + ver + "', should be '1'");
			}
			while (true) {
				parser.next();
				if (parser.getEventType() == XmlPullParser.START_TAG
						&& "project".equals(parser.getName())) {
					parseProjectElement(parser, ib);
				} else if (parser.getEventType() == XmlPullParser.END_TAG
						&& "my-time-backup".equals(parser.getName())) {
					break;
				} else if (parser.getEventType() == XmlPullParser.TEXT) {
				} else {
					throw new DatabaseSyntaxError(
							"invalid document expected START_TAG 'project' or END_TAG 'my-time-backup' but got "
							+ XmlPullParser.TYPES[parser.getEventType()] 
							+ " '" + parser.getName() + "'");
				}
			}
		}
	}

	private void parseProjectElement(XmlPullParser parser, ImportBuilder ib) throws XmlPullParserException, IOException, DatabaseSyntaxError {
		String documentProjectId = parser.getAttributeValue(null, "id");
		String documentProjectName = parser.getAttributeValue(null, "name");
		long projectId = ib.createProject(documentProjectName);
		while (true) {
			parser.next();
			if (parser.getEventType() == XmlPullParser.START_TAG
					&& "session".equals(parser.getName())) {
				parseSessionElement(projectId, parser, ib);
			} else if (parser.getEventType() == XmlPullParser.END_TAG
					&& "project".equals(parser.getName())) {
				break;
			} else if (parser.getEventType() == XmlPullParser.TEXT) {
			} else {
				throw new DatabaseSyntaxError(
						"invalid document expected START_TAG session or END_TAG project but got "
						+ XmlPullParser.TYPES[parser.getEventType()] 
						+ " '" + parser.getName() + "'");
			}
		}
	}

	private void parseSessionElement(long projectId, XmlPullParser parser, ImportBuilder ib) throws XmlPullParserException, IOException {
		String startTime = parser.getAttributeValue(null, "start");
		String endTime = parser.getAttributeValue(null, "end");
		String comment = parser.getAttributeValue(null, "comment");
		ib.createSession(projectId, startTime, endTime, comment);
		parser.next();
	}
}
