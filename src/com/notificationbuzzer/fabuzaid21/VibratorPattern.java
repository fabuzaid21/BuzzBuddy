package com.notificationbuzzer.fabuzaid21;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class VibratorPattern extends RelativeLayout {

	private static final String TAG = VibratorPattern.class.getSimpleName();
	private static final long[] PATTERN = {0, 100};
	
	private Vibrator vibrator;
	private long lastTimeTouched = 0;
	private List<Long> vibrationPattern = new ArrayList<Long>();

	public VibratorPattern(Context context, AttributeSet attrs) {
		super(context, attrs);
		vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
	}

	@Override
	public boolean onTouchEvent(final MotionEvent e) {
		switch(e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			Log.d(TAG, "Action down");
			vibrator.vibrate(PATTERN, 0);
			updateLastTouched();
			return true;
			
		case MotionEvent.ACTION_UP:
			Log.d(TAG, "Action up");
			vibrator.cancel();
			updateLastTouched();
			return true;
		}
		return false;
	}

	private void updateLastTouched() {
		vibrationPattern.add(currentTime() - lastTimeTouched);
		lastTimeTouched = currentTime();
	}
	
	private long currentTime() {
		return System.currentTimeMillis();
	}

}
