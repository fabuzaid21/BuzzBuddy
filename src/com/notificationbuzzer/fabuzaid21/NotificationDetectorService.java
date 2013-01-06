package com.notificationbuzzer.fabuzaid21;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.os.Vibrator;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class NotificationDetectorService extends AccessibilityService {

	private static final String TAG = NotificationDetectorService.class.getSimpleName();
	private Vibrator vibrator;

	@Override
	public void onAccessibilityEvent(final AccessibilityEvent event) {
		if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
			final String packageName = String.valueOf(event.getPackageName());
			Log.d(TAG, packageName);
			// Log.d(TAG, "" + event.getEventTime());
			// List<CharSequence> list = event.getText();
			// for (CharSequence str : list) {
			// Log.d(TAG, String.valueOf(str));
			// }
			// TODO change this to vibrate to the actual pattern
			final BuzzDB base = ((NotificationBuzzerApp) getApplication()).getDatabase();
			vibrator.vibrate(1000);
		}
	}

	@Override
	protected void onServiceConnected() {
		Log.d(TAG, "onServiceConnected");
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	}

	@Override
	public void onInterrupt() {
		Log.d(TAG, "onInterrupt");
	}
}
