package com.notificationbuzzer.fabuzaid21;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class CustomAlertDialog extends Dialog implements OnClickListener {

	private DialogInterface.OnClickListener listener;

	public CustomAlertDialog(final Context context, final int theme) {
		super(context, theme);
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alert_dialog);
		final Button button = (Button) findViewById(R.id.dialog_button);

		button.setOnClickListener(this);
	}

	public void setOnClickListener(final DialogInterface.OnClickListener listener) {
		this.listener = listener;
	}

	@Override
	public void onClick(final View v) {
		if (listener != null) {
			listener.onClick(this, R.id.dialog_button);
		}
	}

}
