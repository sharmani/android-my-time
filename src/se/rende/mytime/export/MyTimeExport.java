package se.rende.mytime.export;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;

import se.rende.mytime.BackupFormatter;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class MyTimeExport extends Activity {
	private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yy-MM-dd HH-mm-ss");

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		exportDatabase();
	}

	private void exportDatabase() {
		try {
			BackupFormatter backupFormatter = new BackupFormatter(
					getContentResolver());
			String fileName = "My Time export "
				+ DATE_TIME_FORMAT.format(
						System.currentTimeMillis()).replace('/', '-');
			File backupFile = new File(Environment
					.getExternalStorageDirectory(), fileName + ".xml");
			FileOutputStream os = new FileOutputStream(backupFile);
			backupFormatter.writeXml(os);
			os.close();
			Toast.makeText(this, "Saved '" + backupFile + "'", Toast.LENGTH_LONG).show();

			Intent i = new Intent(android.content.Intent.ACTION_SEND);
			i.putExtra(Intent.EXTRA_SUBJECT, fileName);
			i.setType("text/xml");
			i.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + backupFile));

//			startActivity(Intent.createChooser(i,
//					getString(R.string.export_title)));
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "export error", e);
			new AlertDialog.Builder(this).setMessage("backup error " + e).show();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		// don't show this view when back from My Time app
		finish();
	}

}