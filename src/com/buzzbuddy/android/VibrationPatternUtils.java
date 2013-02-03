package com.buzzbuddy.android;

public class VibrationPatternUtils {

	static long totalPatternTime(final long[] pattern) {
		long toReturn = 0;
		for (final long elem : pattern) {
			toReturn += elem;
		}
		return toReturn;
	}

	static long[] deserializePattern(final String patternString) {
		final String[] temp = patternString.split("-");
		final long[] toReturn = new long[temp.length];
		for (int i = 0; i < temp.length; ++i) {
			toReturn[i] = Long.parseLong(temp[i]);
		}
		return toReturn;
	}

	static String serializePattern(final long[] finalPattern) {
		String toReturn = "" + finalPattern[0];
		for (int i = 1; i < finalPattern.length; ++i) {
			toReturn += "-" + finalPattern[i];
		}
		return toReturn;
	}

}
