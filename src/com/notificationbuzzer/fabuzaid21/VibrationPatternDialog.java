package com.notificationbuzzer.fabuzaid21;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

public class VibrationPatternDialog extends Dialog implements OnClickListener {

	private static final String TAG = VibrationPatternDialog.class.getSimpleName();
	private static final int STOP_ID = 0x10;

	private ImageButton record;
	private final Resources res;
	private Drawable stopDrawable = null;
	private Drawable recordDrawable = null;

	private static final long[] PATTERN = { 0, 5 };

	private final Vibrator vibrator;
	private boolean isRecording = false;
	private TextView instructions;
	private final String generalInstructions;
	private final String recordingText;
	private VibrationPattern vibrationPattern;
	private ImageButton accept;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setContentView(R.layout.vibration_pattern);
		final ImageButton cancel = (ImageButton) findViewById(R.id.cancel);
		accept = (ImageButton) findViewById(R.id.accept);
		record = (ImageButton) findViewById(R.id.record);

		cancel.setOnClickListener(this);
		accept.setOnClickListener(this);
		record.setOnClickListener(this);

		instructions = (TextView) findViewById(R.id.vibration_instructions);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart");
		setInitialMode();
	}

	private void setInitialMode() {
		instructions.setText(generalInstructions);
		record.setImageDrawable(getRecordDrawable());
		isRecording = false;
		setAcceptButtonEnabled(vibrationPattern.isPatternPresent());
		record.setId(R.id.record);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "onStop");
	}

	private void setAcceptButtonEnabled(final boolean isEnabled) {
		accept.setEnabled(isEnabled);
		accept.setClickable(isEnabled);
	}

	public VibrationPatternDialog(final Context context, final int theme) {
		super(context, theme);
		Log.d(TAG, "constructor");
		res = context.getResources();
		generalInstructions = res.getString(R.string.vibration_pattern_explanation);
		recordingText = res.getString(R.string.vibration_pattern_tapping);
		vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

	}

	public void setVibrationPattern(final VibrationPattern vibPattern) {
		vibrationPattern = vibPattern;
	}

	@Override
	public void onClick(final View v) {
		switch (v.getId()) {
		case R.id.cancel:
			Log.w(TAG, "cancel clicked");
			cancel();
			break;
		case R.id.accept:
			Log.w(TAG, "accept clicked");
			dismiss();
			break;
		case R.id.record:
			Log.w(TAG, "recording");
			setRecordingMode();
			record.setId(STOP_ID);
			vibrationPattern.initializePattern();
			break;
		case STOP_ID:
			Log.w(TAG, "stopping");
			setInitialMode();
			break;
		}

	}

	private void setRecordingMode() {
		instructions.setText(recordingText);
		record.setImageDrawable(getStopDrawable());
		setAcceptButtonEnabled(false);
		isRecording = true;
	}

	private Drawable getStopDrawable() {
		if (stopDrawable == null) {
			stopDrawable = res.getDrawable(R.drawable.stop);
		}
		return stopDrawable;
	}

	private Drawable getRecordDrawable() {
		if (recordDrawable == null) {
			recordDrawable = res.getDrawable(R.drawable.record);
		}
		return recordDrawable;
	}

	@Override
	public boolean onTouchEvent(final MotionEvent e) {
		if (isRecording) {
			Log.d(TAG, "onTouchEvent");
			switch (e.getAction()) {
			case MotionEvent.ACTION_DOWN:
				Log.d(TAG, "Action down");
				vibrator.vibrate(PATTERN, 0);
				vibrationPattern.updateLastTouched();
				return true;

			case MotionEvent.ACTION_UP:
				Log.d(TAG, "Action up");
				vibrator.cancel();
				vibrationPattern.updateLastTouched();
				return true;
			}
		}
		return false;
	}

}
