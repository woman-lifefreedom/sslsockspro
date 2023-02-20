/*
 * Copyright (C) 2017-2021 comp500
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify this Program, or any covered work, by linking or combining
 * it with OpenSSL (or a modified version of that library), containing parts
 * covered by the terms of the OpenSSL License, the licensors of this Program
 * grant you additional permission to convey the resulting work.
 */

package link.infra.sslsockspro.gui.mnglog;

import static link.infra.sslsockspro.Constants.LOG_ISO;
import static link.infra.sslsockspro.Constants.LOG_LEVEL_DEFAULT;
import static link.infra.sslsockspro.Constants.LOG_LEVEL_OFFSET;
import static link.infra.sslsockspro.Constants.LOG_NONE;
import static link.infra.sslsockspro.Constants.LOG_SHORT;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import link.infra.sslsockspro.R;
import link.infra.sslsockspro.gui.main.MainActivity;
import link.infra.sslsockspro.service.StunnelService;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LogFragment extends Fragment {

	private OnLogFragmentInteractionListener mListener;
	String log;
	int level = LOG_LEVEL_DEFAULT;
	String format = LOG_SHORT;
	public static boolean clearLogsOnNewConnect = true;

	public LogFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @return A new instance of fragment LogFragment.
	 */
	public static LogFragment newInstance(OnLogFragmentInteractionListener listener) {
		LogFragment fragment = new LogFragment();
		fragment.mListener = listener;
		return fragment;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		setHasOptionsMenu(true);
		return inflater.inflate(R.layout.fragment_log, container,false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		final TextView logText = view.findViewById(R.id.logtext);
		logText.setMovementMethod(new ScrollingMovementMethod());
		final TextView logLevelView = view.findViewById(R.id.log_level_tv);
		logLevelView.setText(String.valueOf(LOG_LEVEL_DEFAULT));
		final CheckBox clearLogs = view.findViewById(R.id.clear_logs_connect);
		clearLogs.setChecked(clearLogsOnNewConnect);
		//clearLogs.setVisibility(View.GONE);

		clearLogs.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				clearLogsOnNewConnect = isChecked;
			}
		});

		RadioGroup radioGroup = view.findViewById(R.id.timeFormatRadioGroup);
		RadioButton radioButtonShort = view.findViewById(R.id.radioShort);
		radioGroup.check(radioButtonShort.getId());
		radioGroup.setOnCheckedChangeListener((group, checkedId) -> mListener.onLogFormatChanged(group,checkedId));

		SeekBar seekBar = view.findViewById(R.id.log_level_seekbar);
		seekBar.setProgress(LOG_LEVEL_DEFAULT - LOG_LEVEL_OFFSET);

		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(
					SeekBar seekBar, int progress,
					boolean fromUser) {
				logLevelView.setText(String.valueOf(progress + LOG_LEVEL_OFFSET));
				mListener.onLogLevelChanged(progress + LOG_LEVEL_OFFSET);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		//logText.setMovementMethod(new ScrollingMovementMethod());
		Activity act = getActivity();
		if (act == null) {
			return;
		}

		StunnelService.logData.observe(getViewLifecycleOwner(), log -> {
			this.log = log;
			MainActivity.formatLog(log,format,level);
		});
		MainActivity.logFormat.observe(getViewLifecycleOwner(), format ->{
			this.format = format;
			MainActivity.formatLog(log,format,level);
		});
		MainActivity.logLevel.observe(getViewLifecycleOwner(), level ->{
			this.level = level;
			MainActivity.formatLog(log,format,level);
		});
		MainActivity.logDataFormattedLeveled.observe(getViewLifecycleOwner(), logText::setText);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.action_import_profile).setVisible(false);
		menu.findItem(R.id.action_add_profile).setVisible(false);
		menu.findItem(R.id.export_logs).setVisible(false);
	}

	public interface OnLogFragmentInteractionListener {
		void onLogFormatChanged(RadioGroup group, int checkedId);
		void onLogLevelChanged(int position);
//		void onClearLogs(boolean cleanLogs);
	}

}
