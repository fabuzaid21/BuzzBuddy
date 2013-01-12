package com.notificationbuzzer.fabuzaid21;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.notificationbuzzer.fabuzaid21.CountdownTimer.CountdownCallback;

public class VibrationPatternDialog extends Dialog implements OnClickListener, CountdownCallback {

	private static final int TEN_SECONDS = 10;
	private static final String TAG = VibrationPatternDialog.class.getSimpleName();

	private ImageButton record;
	private final Resources res;

	private static final long[] PATTERN = { 0, 5 };

	private final Vibrator vibrator;
	private boolean isRecording = false;
	private TextView instructions;
	private final String generalInstructions;
	private final String recordingText;
	private VibrationPattern vibrationPattern;
	private ImageButton accept;
	private final CountdownTimer timer;
	private TextView timerText;

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
		timerText = (TextView) findViewById(R.id.timer);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart");
		setInitialMode();
	}

	private void setInitialMode() {
		instructions.setText(generalInstructions);
		isRecording = false;
		setAcceptButtonEnabled(vibrationPattern.isPatternPresent());
		record.setSelected(true);
		timerText.setSelected(true);
		timerText.setText("0:" + TEN_SECONDS);
		timer.stop();
		timer.reset();
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
		timer = new CountdownTimer(TEN_SECONDS);
		timer.setCountdownCallback(this);

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
			if (v.isSelected()) {
				Log.w(TAG, "recording");
				setRecordingMode();
				record.setSelected(false);
				timerText.setSelected(false);
				vibrationPattern.initializePattern();
				break;
			}
			Log.w(TAG, "stopping");
			setInitialMode();
			break;
		}

	}

	private void setRecordingMode() {
		instructions.setText(recordingText);
		setAcceptButtonEnabled(false);
		isRecording = true;

	}

	@Override
	public boolean onTouchEvent(final MotionEvent e) {
		if (isRecording) {
			Log.d(TAG, "onTouchEvent");
			switch (e.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (!timer.isRunning()) {
					timer.start();
				}
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

	@Override
	public void onTimerStop() {
		if (isRecording) {
			record.performClick();
		}

	}

	@Override
	public void onDecrement(final int time) {
		if (time < 10) {
			timerText.setText("0:0" + time);
			return;
		}
		timerText.setText("0:" + time);

	}

	@Override
	public void onTimerStart() {

	}
}
