package com.buzzbuddy.android;

import java.util.LinkedList;
import java.util.List;

import android.util.Log;

public class VibrationPattern {

	private static final String TAG = VibrationPattern.class.getSimpleName();

	private long lastTimeTouched;
	private List<Long> vibrationPattern;

	private long[] currentPattern;

	public VibrationPattern() {
		initialize();
	}

	public VibrationPattern(final long[] pattern) {
		initialize();
		currentPattern = pattern;
	}

	private void initialize() {
		vibrationPattern = new LinkedList<Long>();
		lastTimeTouched = 0;
	}

	public long[] getFinalizedPattern() {
		if (currentPattern != null && currentPattern.length > 0) {
			return currentPattern;
		}
		if (vibrationPattern.size() == 0) {
			return null;
		}
		currentPattern = listToArray();
		// replace first elem with 0
		currentPattern[0] = 0L;
		if (BuildConfig.DEBUG) {
			Log.i(TAG, "printing out pattern");
			for (final Long elem : currentPattern) {
				Log.i(TAG, "" + elem);
			}
		}
		return currentPattern;
	}

	private long[] listToArray() {
		final long[] toReturn = new long[vibrationPattern.size()];
		for (int i = 0; i < vibrationPattern.size(); ++i) {
			toReturn[i] = vibrationPattern.get(i);
		}
		return toReturn;
	}

	public void initializePattern() {
		vibrationPattern.clear();
		currentPattern = null;
	}

	public void updateLastTouched() {
		if (BuildConfig.DEBUG) {
			Log.i(TAG, "updateLastTouched");
		}
		vibrationPattern.add(currentTime() - lastTimeTouched);
		lastTimeTouched = currentTime();
	}

	private long currentTime() {
		return System.currentTimeMillis();
	}

	public boolean isPatternPresent() {
		return (currentPattern != null && currentPattern.length > 0) || isNewPatternPresent();
	}

	public boolean isNewPatternPresent() {
		return vibrationPattern.size() > 0;
	}
}
