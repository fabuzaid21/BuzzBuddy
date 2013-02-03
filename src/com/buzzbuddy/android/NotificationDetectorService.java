package com.buzzbuddy.android;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class NotificationDetectorService extends AccessibilityService {

	private static final String GOOGLE_TALK_PACKAGE = "com.google.android.talk";
	private static final String GOOGLE_SERVICES_FRAMEWORK_PACKAGE = "com.google.android.gsf";
	private static final String TAG = NotificationDetectorService.class.getSimpleName();
	private Vibrator vibrator;
	private AudioManager audioManager;
	private BuzzDB base;

	@SuppressWarnings("deprecation")
	@Override
	public void onAccessibilityEvent(final AccessibilityEvent event) {
		Log.i(TAG, "onAccessibilityEvent");

		if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
			return;
		}

		final int vibrateSetting = audioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION);
		audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, AudioManager.VIBRATE_SETTING_OFF);

		String packageName = String.valueOf(event.getPackageName());
		if (BuildConfig.DEBUG) {
			Log.i(TAG, packageName);
		}
		if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
			Log.d(TAG, "Notification Event");
			final Notification notification = (Notification) event.getParcelableData();
			if (notification == null) {
				Log.e(TAG, "notification is null");
				return;
			}
			if ((notification.flags & Notification.FLAG_ONGOING_EVENT) != 0) {
				Log.i(TAG, "ongoing event, do not play vibration pattern");
				return;
			}

			if (packageName.equals(GOOGLE_SERVICES_FRAMEWORK_PACKAGE)) {
				packageName = GOOGLE_TALK_PACKAGE;
			}
			final Cursor resultSet = base.queryByPackageName(packageName);
			resultSet.moveToFirst();
			if (resultSet.getCount() > 0) {

				final String patternString = resultSet.getString(BuzzDB.APP_INDEX_VIBRATION);
				final long[] vibrationPattern = VibrationPatternUtils.deserializePattern(patternString);
				Log.i(TAG, "playing vibration pattern!");
				vibrator.vibrate(vibrationPattern, -1);

			}
			resultSet.close();
			audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, vibrateSetting);
		}
	}

	@Override
	protected void onServiceConnected() {
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "onServiceConnected");
		}
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		base = ((BuzzBuddyApp) getApplication()).getDatabase();

		final AccessibilityServiceInfo info = new AccessibilityServiceInfo();
		info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
		info.notificationTimeout = 0;
		info.feedbackType = AccessibilityServiceInfo.FEEDBACK_HAPTIC;
		setServiceInfo(info);
	}

	@Override
	public void onInterrupt() {
		Log.e(TAG, "onInterrupt");
	}
}
