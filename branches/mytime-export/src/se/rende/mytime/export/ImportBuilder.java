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

/**
 *
 * @author Dag Rende
 */
public interface ImportBuilder {

	/**
	 * Creates a project and returns the database id of it. The name is used if no existing project have this name. Then a different name is used.
	 * @param documentProjectName name of the project
	 * @return database id of the new project
	 */
	long createProject(String documentProjectName);

	/**
	 * Creates a new session in the specified project.
	 * @param projectId
	 * @param startTime
	 * @param endTime
	 * @param comment
	 */
	void createSession(long projectId, String startTime, String endTime,
			String comment);
	
}
