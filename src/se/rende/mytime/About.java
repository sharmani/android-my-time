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

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * Handles the about box.
 * 
 * @author Dag Rende
 */
public class About extends Activity { 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);

		TextView versionTextView = (TextView) findViewById(R.id.about_version);
		String rev = "$Revision$";
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			versionTextView.setText(getString(R.string.about_version) + " "
					+ packageInfo.versionName + "." + rev.split(" ")[1]);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		TextView srcLinkTextView = (TextView) findViewById(R.id.about_src_link);
		final String srcLinkText = srcLinkTextView.getText().toString();
		SpannableString str = SpannableString
				.valueOf(srcLinkTextView.getText());
		Linkify.addLinks(str, Linkify.ALL);
		srcLinkTextView.setText(str);
		srcLinkTextView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(srcLinkText));
				startActivity(myIntent);
			}
		});
	}
}
