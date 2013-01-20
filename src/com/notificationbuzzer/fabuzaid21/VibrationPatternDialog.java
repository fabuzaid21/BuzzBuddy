package com.notificationbuzzer.fabuzaid21;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.notificationbuzzer.fabuzaid21.CountdownTimer.CountdownCallback;

public class VibrationPatternDialog extends Dialog implements OnClickListener, CountdownCallback {

	private static final String O_SECONDS_TEXT = "0:00";
	private static final int THIRTY_SECONDS = 30;
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
	private TextView titleText;
	private ImageView titleIcon;
	private ResolveInfo currentApp;
	private final PackageManager packageManager;

	public VibrationPatternDialog(final Context context, final int theme) {
		super(context, theme);
		Log.d(TAG, "constructor");
		res = context.getResources();
		packageManager = context.getPackageManager();
		generalInstructions = res.getString(R.string.vibration_pattern_explanation);
		recordingText = res.getString(R.string.vibration_pattern_tapping);
		vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		timer = new CountdownTimer(THIRTY_SECONDS);
		timer.setCountdownCallback(this);
	}

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
		final LinearLayout dialogTitle = (LinearLayout) findViewById(R.id.dialog_title);
		titleText = (TextView) dialogTitle.findViewById(R.id.app_name);
		titleIcon = (ImageView) dialogTitle.findViewById(R.id.app_icon);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart");
		titleText.setText(currentApp.loadLabel(packageManager));
		titleIcon.setImageDrawable(currentApp.loadIcon(packageManager));
		setInitialMode();
	}

	private void setInitialMode() {
		instructions.setText(generalInstructions);
		isRecording = false;
		setAcceptButtonEnabled(vibrationPattern.isPatternPresent());
		record.setSelected(true);
		timerText.setSelected(true);
		timerText.setText(O_SECONDS_TEXT);
		timer.stop();
		timer.reset();
	}

	@Override
	protected void onStop() {
		super.onStop();
		isRecording = false;
		timer.stop();
		Log.d(TAG, "onStop");
	}

	private void setAcceptButtonEnabled(final boolean isEnabled) {
		accept.setEnabled(isEnabled);
		accept.setClickable(isEnabled);
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
				Log.d(TAG, "Action down");
				if (!timer.isRunning()) {
					timer.start();
				}
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
			Toast.makeText(getContext(), res.getString(R.string.pattern_limit_length), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onDecrement(final int time) {
		final int displayTime = THIRTY_SECONDS - time;
		if (displayTime < 10) {
			timerText.setText("0:0" + displayTime);
			return;
		}
		timerText.setText("0:" + displayTime);
	}

	@Override
	public void onTimerStart() {
		// do nothing
	}

	public void setCurrentApp(final ResolveInfo currentItem) {
		currentApp = currentItem;

	}
}
