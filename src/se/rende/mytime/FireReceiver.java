package se.rende.mytime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class FireReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(final Context context, final Intent intent) {
		if (com.twofortyfouram.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
			final boolean isStart = intent.getBooleanExtra(
					Constants.INTENT_EXTRA_LOCALE_PROJECT_ID, true);
			final long projectId = intent.getLongExtra(
					Constants.INTENT_EXTRA_LOCALE_PROJECT_ID, 0);
			
			boolean changed;
			if (isStart) {
				changed = Sessions.startSession(context, projectId);
			} else {
				changed = Sessions.stopSession(context, projectId);
			}
			
			if (changed) {
				context.sendBroadcast(new Intent(Constants.INTENT_DB_UPDATE_ACTION));
			}
		}
	}
}
