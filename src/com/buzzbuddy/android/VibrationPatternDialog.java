package com.buzzbuddy.android;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
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

import com.buzzbuddy.android.CountdownTimer.CountdownCallback;

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
	private final DrawableManager drawableManager;
	private ImageButton playback;

	public VibrationPatternDialog(final Context context, final int theme) {
		super(context, theme);
		Log.d(TAG, "constructor");
		drawableManager = ((BuzzBuddyApp) context.getApplicationContext()).getDrawableManager();
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
		playback = (ImageButton) findViewById(R.id.play_pattern);
		accept = (ImageButton) findViewById(R.id.accept);
		record = (ImageButton) findViewById(R.id.record);

		cancel.setOnClickListener(this);
		accept.setOnClickListener(this);
		record.setOnClickListener(this);
		playback.setOnClickListener(this);

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
		final ApplicationInfo applicationInfo = currentApp.activityInfo.applicationInfo;
		titleText.setText(applicationInfo.loadLabel(packageManager));
		titleIcon.setImageDrawable(drawableManager.fetchDrawable(applicationInfo.packageName, currentApp));
		setInitialMode();
	}

	private void setInitialMode() {
		vibrator.cancel(); // stop any vibration (could be coming from playback)
		instructions.setText(generalInstructions);
		isRecording = false;
		setPlaybackButtonEnabled(vibrationPattern.isPatternPresent());
		setAcceptButtonEnabled(vibrationPattern.isNewPatternPresent());
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
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "onStop");
		}
	}

	private void setPlaybackButtonEnabled(final boolean isEnabled) {
		playback.setEnabled(isEnabled);
		playback.setClickable(isEnabled);
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
			vibrator.cancel();
			cancel();
			break;
		case R.id.accept:
			Log.w(TAG, "accept clicked");
			vibrator.cancel();
			dismiss();
			break;
		case R.id.record:
			// if record button is showing
			if (v.isSelected()) {
				Log.w(TAG, "recording");
				setRecordingMode();
				record.setSelected(false);
				timerText.setSelected(false);
				vibrationPattern.initializePattern();
				break;
			}
			// if stop button is showing
			Log.w(TAG, "stopping");
			playback.removeCallbacks(enablePlayback);
			setInitialMode();
			break;
		case R.id.play_pattern:
			final long[] pattern = vibrationPattern.getFinalizedPattern();
			final long delay = VibrationPatternUtils.totalPatternTime(pattern);
			setPlaybackButtonEnabled(false);
			record.setSelected(false);
			playback.postDelayed(enablePlayback, delay);
			vibrator.vibrate(pattern, -1);
			break;
		}
	}

	private void setRecordingMode() {
		instructions.setText(recordingText);
		setAcceptButtonEnabled(false);
		setPlaybackButtonEnabled(false);
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

	private final Runnable enablePlayback = new Runnable() {

		@Override
		public void run() {
			record.setSelected(true);
			setPlaybackButtonEnabled(true);
		}
	};
}
