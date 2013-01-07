package com.notificationbuzzer.fabuzaid21;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AppAdapter extends ArrayAdapter<ResolveInfo> {

	private List<ResolveInfo> list;
	private final Context context;

	public AppAdapter(final Context context, final List<ResolveInfo> list) {
		super(context, R.layout.app_row, list);
		this.context = context;
		this.list = list;
	}

	private static class ViewHolder {
		protected ImageView icon;
		protected TextView appName;
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		View view = null;

		if (convertView == null) {
			final LayoutInflater inflator = ((Activity) context).getLayoutInflater();
			view = inflator.inflate(R.layout.app_row, null);
			final ViewHolder viewHolder = new ViewHolder();
			viewHolder.icon = (ImageView) view.findViewById(R.id.app_icon);
			viewHolder.appName = (TextView) view.findViewById(R.id.app_name);
			view.setTag(viewHolder);
		} else {
			view = convertView;
		}

		final ViewHolder holder = (ViewHolder) view.getTag();
		final ResolveInfo item = list.get(position);
		holder.icon.setImageDrawable(item.loadIcon(context.getPackageManager()));
		holder.appName.setText(item.loadLabel(context.getPackageManager()));

		return view;
	}
}