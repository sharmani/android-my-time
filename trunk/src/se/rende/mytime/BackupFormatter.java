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
package se.rende.mytime;

import static se.rende.mytime.Constants.CONTENT_URI_PROJECT;
import static se.rende.mytime.Constants.CONTENT_URI_SESSION;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.xmlpull.v1.XmlSerializer;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Xml;

/**
 * Formats the backup text to a stream.
 * @author Dag Rende
 */
public class BackupFormatter {
	private final ContentResolver contentResolver;

	/**
	 * @param contentResolver
	 */
	public BackupFormatter(ContentResolver contentResolver) {
		this.contentResolver = contentResolver;
	}
	
	/**
	 * Writes backup as xml in UTF-8 encoding to the output stream.
	 * @param os where xml is written
	 * @throws IOException
	 */
	public void writeXml(OutputStream os) throws IOException {
		XmlSerializer serializer = Xml.newSerializer();
        serializer.setOutput(os, "UTF-8");
        serializer.startDocument("UTF-8", true);
        serializer.text("\n");
        serializer.startTag("", "my-time-backup");
        serializer.attribute("", "ver", "1");
        serializer.text("\n");
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(CONTENT_URI_PROJECT,
					new String[] { "_id", "name" }, null, null,	"name asc");
			while (cursor.moveToNext()) {
			    serializer.text("  ");
			    serializer.startTag("", "project");
			    long projectId = cursor.getLong(0);
				serializer.attribute("", "id", "" + projectId);
			    serializer.attribute("", "name", cursor.getString(1));
			    serializer.text("\n");
			    
			    Cursor sessionCursor = null;
				try {
					sessionCursor = contentResolver.query(CONTENT_URI_SESSION,
							new String[] { "_id", "start", "end", "comment" }, "project_id=?", new String[] {"" + projectId},	"start asc");
					while (sessionCursor.moveToNext()) {
					    serializer.text("    ");
					    serializer.startTag("", "session");
					    serializer.attribute("", "id", sessionCursor.getString(0));
					    serializer.attribute("", "start", sessionCursor.getString(1));
					    if (!sessionCursor.isNull(2)) {
							serializer.attribute("", "end", sessionCursor.getString(2));
						}
						String comment = sessionCursor.getString(3);
					    if (comment != null) {
							serializer.attribute("", "comment", comment);
						}
					    serializer.endTag("", "session");
					    serializer.text("\n");
					}
				} finally {
					if (sessionCursor != null) {
						sessionCursor.close();
					}
				}
			    
			    serializer.text("  ");
			    serializer.endTag("", "project");
			    serializer.text("\n");
			}
			serializer.endTag("", "my-time-backup");
			serializer.text("\n");
			serializer.endDocument();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * @return
	 */
	public String getXml1() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter bw = new PrintWriter(new OutputStreamWriter(baos));
		
		Cursor cursor = contentResolver.query(CONTENT_URI_PROJECT,
				new String[] { "_id", "name" }, null, null,	"name asc");
		bw.println("<mytime>");
		while (cursor.moveToNext()) {
			bw.println("  <project name='" + cursor.getString(1) + "'>");
			bw.println("  </project>");
		}
		bw.println("</mytime>");
		cursor.close();
		bw.close();
		
		return baos.toString();
	}

}
