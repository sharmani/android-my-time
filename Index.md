# My Time - Time Reporting for Android #

## Introduction ##

Simplistic timesheet application.
Keeps a list of projects and a list of sessions with date/times and the number of hours worked. Just click start to add a new session, and stop to end it. Send report by mail etc.
The program can be downloaded from Android Market under the name **My Time**.

## User Interface ##

### Main Page - Projects ###

|<img src='http://android-my-time.googlecode.com/svn/wiki/images/projects.png' width='300' />|
|:-------------------------------------------------------------------------------------------|

Displays the list of projects to report time for.

  * tap project to open the sessions page for this project
  * hold finger on project to open context menu:
    * delete - deletes this project with all sessions
    * edit - opens project name edit dialog
  * panel menu:
    * Add project - opens the project name dialog to let you name the new project
    * Settings - opens the settings page
    * About - opens the about page with version info and link to this web site

### Sessions page ###

|<img src='http://android-my-time.googlecode.com/svn/wiki/images/sessions.png' width='300' />|
|:-------------------------------------------------------------------------------------------|

Lists the sessions for a project. The times are summed to months and weeks (obeying the week numbering rules of the current locale).

  * tap start to add a session starting at current time, and running as the end time.
  * tap stop to end the running session
  * tap a session to edit its start and end date and time and optional comment
  * hold finger on session to get the context menu:
    * delete - deletes this session
  * panel menu:
    * Add session - adds a session and opens the session edit dialog with start/end time/date and comment
    * Share - to send a time report by mail or other media, after selecting period and other options.