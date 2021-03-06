/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.main_activity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import de.azapps.mirakel.helper.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewSwitcher;
import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.R;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.Helpers.ExecInterface;
import de.azapps.mirakel.helper.TaskDialogHelpers;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakel.reminders.ReminderAlarm;

public class TaskFragment extends Fragment {
	private View view;
	private static final String TAG = "TaskActivity";
	protected TextView Task_name;
	protected CheckBox Task_done;
	protected TextView Task_prio;
	protected TextView Task_content;
	protected TextView Task_due;
	protected TextView Task_reminder;

	protected MainActivity main;
	protected NumberPicker picker;
	protected EditText input;
	private Task task;
	private boolean created = false;

	private boolean mIgnoreTimeSet = false;

	public void setActivity(MainActivity activity) {
		main = activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		main = (MainActivity) getActivity();
		view = inflater.inflate(R.layout.activity_task, container, false);
		created = true;
		update();
		return view;
	}

	public void update() {
		if (!created)
			return;
		ViewSwitcher s = (ViewSwitcher) view.findViewById(R.id.switch_name);
		if (s.getNextView().getId() != R.id.edit_name) {
			s.showPrevious();
		}
		s = (ViewSwitcher) view.findViewById(R.id.switch_content);
		if (s.getNextView().getId() != R.id.task_content_edit) {
			s.showPrevious();
		}
		// Task Name
		task = main.getCurrentTask();
		Task_name = (TextView) view.findViewById(R.id.task_name);
		Task_name.setText(task.getName());
		Task_name.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ViewSwitcher switcher = (ViewSwitcher) view
						.findViewById(R.id.switch_name);
				switcher.showNext(); // or switcher.showPrevious();
				EditText txt = (EditText) view.findViewById(R.id.edit_name);
				txt.setText(Task_name.getText());
				txt.requestFocus();

				InputMethodManager imm = (InputMethodManager) main
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(txt, InputMethodManager.SHOW_IMPLICIT);
				txt.setOnEditorActionListener(new OnEditorActionListener() {
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_DONE) {
							EditText txt = (EditText) view
									.findViewById(R.id.edit_name);
							InputMethodManager imm = (InputMethodManager) main
									.getSystemService(Context.INPUT_METHOD_SERVICE);
							task.setName(txt.getText().toString());
							main.saveTask(task);
							Task_name.setText(task.getName());
							imm.hideSoftInputFromWindow(txt.getWindowToken(), 0);
							return true;
						}
						return false;
					}
				});
			}
		});

		// Task done
		Task_done = (CheckBox) view.findViewById(R.id.task_done);
		Task_done.setChecked(task.isDone());
		Task_done.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				task.setDone(isChecked);
				main.saveTask(task);
				ReminderAlarm.updateAlarms(getActivity());
				main.getListFragment().update();
			}
		});

		// Task priority
		Task_prio = (TextView) view.findViewById(R.id.task_prio);
		set_prio(Task_prio, task);
		Task_prio.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				TaskDialogHelpers.handlePriority(main, task,
						new ExecInterface() {

							@Override
							public void exec() {
								main.updatesForTask(task);
								set_prio(Task_prio, task);

							}
						});
			}
		});

		// Task due
		Task_due = (TextView) view.findViewById(R.id.task_due);
		Drawable due_img = main.getResources().getDrawable(
				android.R.drawable.ic_menu_today);
		due_img.setBounds(0, 0, 60, 60);
		Task_due.setCompoundDrawables(due_img, null, null, null);
		if (task.getDue() == null) {
			Task_due.setText(getString(R.string.no_date));
		} else {
			Task_due.setText(Helpers.formatDate(task.getDue(),
					main.getString(R.string.dateFormat)));
		}

		Task_due.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mIgnoreTimeSet = false;
				GregorianCalendar due = (task.getDue() == null ? new GregorianCalendar()
						: task.getDue());
				DatePickerDialog dialog = new DatePickerDialog(main,
						new OnDateSetListener() {

							@Override
							public void onDateSet(DatePicker view, int year,
									int monthOfYear, int dayOfMonth) {
								if (mIgnoreTimeSet)
									return;

								task.setDue(new GregorianCalendar(year,
										monthOfYear, dayOfMonth));
								main.saveTask(task);
								Task_due.setText(new SimpleDateFormat(view
										.getContext().getString(
												R.string.dateFormat), Locale
										.getDefault()).format(task.getDue()
										.getTime()));

							}
						}, due.get(Calendar.YEAR), due.get(Calendar.MONTH), due
								.get(Calendar.DAY_OF_MONTH));
				dialog.setButton(DialogInterface.BUTTON_NEGATIVE,
						getString(R.string.no_date),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								if (which == DialogInterface.BUTTON_NEGATIVE) {
									mIgnoreTimeSet = true;
									Log.v(TAG, "cancel");
									task.setDue(null);
									main.saveTask(task);
									Task_due.setText(R.string.no_date);
								}
							}
						});
				dialog.show();

			}
		});

		// Task Reminder
		Task_reminder = (TextView) view.findViewById(R.id.task_reminder);
		Drawable reminder_img = main.getResources().getDrawable(
				android.R.drawable.ic_menu_recent_history);
		reminder_img.setBounds(0, 0, 60, 60);
		Task_reminder.setCompoundDrawables(reminder_img, null, null, null);
		if (task.getReminder() == null) {
			Task_reminder.setText(getString(R.string.no_reminder));
		} else {
			Task_reminder.setText(Helpers.formatDate(task.getReminder(),
					main.getString(R.string.humanDateTimeFormat)));
		}

		Task_reminder.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				TaskDialogHelpers.handleReminder(main, task,
						new ExecInterface() {

							@Override
							public void exec() {
								if (task.getReminder() == null) {
									Task_reminder.setText(R.string.no_reminder);
								} else {
									Task_reminder
											.setText(new SimpleDateFormat(
													view.getContext()
															.getString(
																	R.string.humanDateTimeFormat),
													Locale.getDefault())
													.format(task.getReminder()
															.getTime()));
								}
								ReminderAlarm.updateAlarms(getActivity());

							}
						});
			}
		});

		// Task content
		Task_content = (TextView) view.findViewById(R.id.task_content);
		Task_content.setText(task.getContent().length() == 0 ? this
				.getString(R.string.task_no_content) : task.getContent());
		Drawable content_img = main.getResources().getDrawable(
				android.R.drawable.ic_menu_edit);
		content_img.setBounds(0, 0, 60, 60);
		Task_content.setCompoundDrawables(content_img, null, null, null);
		Task_content.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ViewSwitcher switcher = (ViewSwitcher) view
						.findViewById(R.id.switch_content);
				switcher.showNext(); // or switcher.showPrevious();
				EditText txt = (EditText) view.findViewById(R.id.edit_content);
				txt.setText(task.getContent());
				txt.requestFocus();

				InputMethodManager imm = (InputMethodManager) main
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(txt, InputMethodManager.SHOW_IMPLICIT);
				Button submit = (Button) view.findViewById(R.id.submit_content);
				submit.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						EditText txt = (EditText) view
								.findViewById(R.id.edit_content);
						InputMethodManager imm = (InputMethodManager) main
								.getSystemService(Context.INPUT_METHOD_SERVICE);
						task.setContent(txt.getText().toString());
						main.saveTask(task);
						Task_content
								.setText(task.getContent().trim().length() == 0 ? getString(R.string.task_no_content)
										: task.getContent());
						/*
						 * 
						 * ViewSwitcher switcher = (ViewSwitcher) view
						 * 
						 * .findViewById(R.id.switch_content);
						 * switcher.showPrevious();
						 */
						imm.hideSoftInputFromWindow(txt.getWindowToken(), 0);

					}
				});
			}
		});

	}

	protected void set_prio(TextView Task_prio, Task task) {
		Task_prio.setText("" + task.getPriority());
		Task_prio
				.setBackgroundColor(Mirakel.PRIO_COLOR[task.getPriority() + 2]);

	}

}
