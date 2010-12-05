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


import android.net.Uri;
import android.provider.BaseColumns;

/**
 * General constants.
 *
 * @author Dag Rende
 */
public interface Constants extends BaseColumns {
	public static final String TABLE_NAME = "events";

	public static final String AUTHORITY = "se.rende.mytime";
	public static final Uri CONTENT_URI_PROJECT = Uri.parse("content://"
			+ AUTHORITY + "/project");
	public static final Uri CONTENT_URI_SESSION = Uri.parse("content://"
			+ AUTHORITY + "/session");
}
