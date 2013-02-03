package com.buzzbuddy.android;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class VibrationPattern {

	private static final String TAG = VibrationPattern.class.getSimpleName();

	private long lastTimeTouched;
	private final List<Long> vibrationPattern;

	public VibrationPattern() {
		vibrationPattern = new ArrayList<Long>();
		lastTimeTouched = 0;
	}

	public Long[] getFinalizedPattern() {
		if (vibrationPattern.size() == 0) {
			return null;
		}
		final Long[] finalPattern = vibrationPattern.toArray(new Long[vibrationPattern.size()]);
		finalPattern[0] = 0L;
		Log.d(TAG, "printing out pattern");
		for (final Long elem : finalPattern) {
			Log.d(TAG, "" + elem);
		}
		return finalPattern;
	}

	public void initializePattern() {
		vibrationPattern.clear();
	}

	public void updateLastTouched() {
		Log.d(TAG, "updateLastTouched");
		vibrationPattern.add(currentTime() - lastTimeTouched);
		lastTimeTouched = currentTime();
	}

	private long currentTime() {
		return System.currentTimeMillis();
	}

	public boolean isPatternPresent() {
		return vibrationPattern.size() != 0;
	}

}
