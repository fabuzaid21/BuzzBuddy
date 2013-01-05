package com.notificationbuzzer.fabuzaid21;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

public class VibrationPatternDialog extends Dialog implements android.view.View.OnClickListener {

	private static final String TAG = VibrationPatternDialog.class.getSimpleName();

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vibration_pattern);
		final ImageButton cancel = (ImageButton) findViewById(R.id.cancel);
		final ImageButton accept = (ImageButton) findViewById(R.id.accept);
		cancel.setOnClickListener(this);
		accept.setOnClickListener(this);
	}

	public VibrationPatternDialog(final Context context, final int theme) {
		super(context, theme);
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
		}

	}

}
