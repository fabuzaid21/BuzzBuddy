package com.notificationbuzzer.fabuzaid21;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class NotificationDetectorService extends AccessibilityService {

	private static final String TAG = NotificationDetectorService.class.getSimpleName();
	private Vibrator vibrator;
	private AudioManager audioManager;
	private BuzzDB base;

	@SuppressWarnings("deprecation")
	@Override
	public void onAccessibilityEvent(final AccessibilityEvent event) {
		Log.d(TAG, "onAccessibilityEvent");
		if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
			return;
		}
		if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
			final String packageName = String.valueOf(event.getPackageName());
			Log.d(TAG, packageName);

			base.open();
			final Cursor resultSet = base.queryByPackageName(packageName);
			resultSet.moveToFirst();
			if (resultSet.getCount() > 0) {
				final int vibrateSetting = audioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION);
				audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_OFF);
				final String patternString = resultSet.getString(BuzzDB.APP_INDEX_VIBRATION);
				final long[] vibrationPattern = deserializePattern(patternString);
				Log.d(TAG, "playing vibration pattern!");
				vibrator.vibrate(vibrationPattern, -1);
				audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, vibrateSetting);
			}
		}
	}

	protected static long[] deserializePattern(final String patternString) {
		final String[] temp = patternString.split("-");
		final long[] toReturn = new long[temp.length];
		for (int i = 0; i < temp.length; ++i) {
			toReturn[i] = Long.parseLong(temp[i]);
		}
		return toReturn;
	}

	@Override
	protected void onServiceConnected() {
		Log.d(TAG, "onServiceConnected");
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		base = ((NotificationBuzzerApp) getApplication()).getDatabase();

		final AccessibilityServiceInfo info = new AccessibilityServiceInfo();
		info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
		info.notificationTimeout = 0;
		info.feedbackType = AccessibilityServiceInfo.FEEDBACK_HAPTIC;
		setServiceInfo(info);
	}

	@Override
	public void onInterrupt() {
		Log.d(TAG, "onInterrupt");
		base.close();
	}
}
