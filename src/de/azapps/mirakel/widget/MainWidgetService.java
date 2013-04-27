package de.azapps.mirakel.widget;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.MirakelHelper;
import de.azapps.mirakel.R;
import de.azapps.mirakel.model.ListsDataSource;
import de.azapps.mirakel.model.Task;
import de.azapps.mirakel.model.TasksDataSource;

public class MainWidgetService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new MainWidgetViewsFactory(getApplicationContext(), intent);
	}

}

class MainWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

	private Context mContext;
	private List<Task> tasks;
	private TasksDataSource tasksDatasource;
	private int listId = 0;
	private int sorting;
	private boolean showDone;

	public MainWidgetViewsFactory(Context context, Intent intent) {
		mContext = context;
		listId = intent.getIntExtra(MainWidgetProvider.EXTRA_LISTID, 0);
		sorting = intent.getIntExtra(MainWidgetProvider.EXTRA_LISTSORT,
				(int) Mirakel.SORT_BY_OPT);
		showDone = intent.getBooleanExtra(MainWidgetProvider.EXTRA_SHOWDONE,
				false);
	}

	/**
	 * Define and open the DataSources
	 */
	public void onCreate() {
		tasksDatasource = new TasksDataSource(mContext);
		tasksDatasource.open();
		tasks = tasksDatasource.getTasks(listId, sorting, showDone);
	}

	public void onDestroy() {
		tasksDatasource.close();
	}

	public int getCount() {
		return tasks.size();
	}

	public RemoteViews getViewAt(int position) {
		// Get The Task
		Task task = tasks.get(position);
		// Initialize the Remote View
		RemoteViews rv = new RemoteViews(mContext.getPackageName(),
				R.layout.widget_row);

		// Set the Contents of the Row
		rv.setTextViewText(R.id.tasks_row_name, task.getName());
		if (task.isDone()) {
			rv.setTextColor(R.id.tasks_row_name, mContext.getResources()
					.getColor(R.color.Grey));
		} else {
			rv.setTextColor(R.id.tasks_row_name, mContext.getResources()
					.getColor(R.color.Black));
		}
		rv.setTextViewText(R.id.tasks_row_priority, task.getPriority() + "");
		rv.setTextColor(R.id.tasks_row_priority, mContext.getResources()
				.getColor(R.color.Black));
		rv.setInt(R.id.tasks_row_priority, "setBackgroundColor",
				Mirakel.PRIO_COLOR[task.getPriority() + 2]);

		if (listId <= 0) {
			rv.setViewVisibility(R.id.tasks_row_list_name, View.VISIBLE);
			ListsDataSource listsDataSource = new ListsDataSource(mContext);
			rv.setTextViewText(R.id.tasks_row_list_name, listsDataSource
					.getList((int) task.getListId()).getName());
		} else {
			rv.setViewVisibility(R.id.tasks_row_list_name, View.GONE);
		}

		String due = MirakelHelper.formatDate(task.getDue(),
				mContext.getString(R.string.dateFormat));
		if (due != "") {
			rv.setViewVisibility(R.id.tasks_row_due, View.VISIBLE);
		rv.setTextViewText(
				R.id.tasks_row_due,
				MirakelHelper.formatDate(task.getDue(),
						mContext.getString(R.string.dateFormat)));
		rv.setTextColor(
				R.id.tasks_row_due,
				mContext.getResources().getColor(
						MirakelHelper.getTaskDueColor(task.getDue(),
								task.isDone())));
		} else {
			rv.setViewVisibility(R.id.tasks_row_due, View.GONE);
		}

		if (task.getContent().length() != 0 ) {
			rv.setViewVisibility(R.id.tasks_row_has_content, View.VISIBLE);
		} else {
			rv.setViewVisibility(R.id.tasks_row_has_content, View.GONE);
		}

		// rv.setBoolean(R.id.tasks_row_done, "setChecked", true);
		/*
		 * holder.taskRowDone.setChecked(task.isDone());
		 * holder.taskRowDone.setOnClickListener(clickCheckbox);
		 */

		// Set the Click–Intent
		// We need to do so, because we can not start the Activity directly from
		// the Service

		Bundle extras = new Bundle();
		extras.putInt(MainWidgetProvider.EXTRA_TASKID, (int) task.getId());
		Intent fillInIntent = new Intent(MainWidgetProvider.CLICK_TASK);
		fillInIntent.putExtras(extras);
		rv.setOnClickFillInIntent(R.id.tasks_row, fillInIntent);

		return rv;
	}

	public RemoteViews getLoadingView() {
		// We aren't going to return a default loading view in this sample
		return null;
	}

	public int getViewTypeCount() {
		return 1;
	}

	public int getItemViewType(int position) {
		return 0;
	}

	public long getItemId(int position) {
		return position;
	}

	public boolean hasStableIds() {
		return true;
	}

	public void onDataSetChanged() {
		tasksDatasource.open();
		// tasks = tasksDatasource.getTasks(listId);
	}
}