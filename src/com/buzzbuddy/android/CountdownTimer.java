package com.buzzbuddy.android;

import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;

public class CountdownTimer {

	private static final int SECOND = 1000;
	protected static final String TAG = CountdownTimer.class.getSimpleName();
	private boolean mRunning;
	private int mTime;
	private final Handler mHandler;
	private final Callback callback = new Callback() {

		@Override
		public boolean handleMessage(final Message msg) {
			--mTime;
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Time remaining: " + mTime + " seconds");
			}
			if (mDelegate != null) {
				mDelegate.onDecrement(mTime);
			}
			if (mTime <= 0) {
				stop();
				return true;
			}
			mHandler.sendMessageDelayed(mHandler.obtainMessage(), SECOND);
			return true;
		}
	};
	private CountdownCallback mDelegate;
	private final int mOriginalTime;

	public CountdownTimer(final int time) {
		mOriginalTime = time + 1; // plus 1 to solve off-by-one error
		reset();
		mHandler = new Handler(callback);
	}

	public void start() {
		if (mDelegate != null) {
			mDelegate.onTimerStart();
		}
		mRunning = true;
		mHandler.sendMessage(mHandler.obtainMessage());
	}

	public void stop() {
		if (mDelegate != null) {
			mDelegate.onTimerStop();
		}
		mRunning = false;
		mHandler.removeCallbacksAndMessages(null);
	}

	public void setCountdownCallback(final CountdownCallback delegate) {
		mDelegate = delegate;
	}

	public boolean isRunning() {
		return mRunning;
	}

	public void reset() {
		mTime = mOriginalTime;
	}

	public interface CountdownCallback {
		void onTimerStop();

		void onDecrement(int time);

		void onTimerStart();
	}
}
