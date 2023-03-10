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

package link.infra.sslsockspro.gui.fragments;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static link.infra.sslsockspro.Constants.EXT_CONF;
import static link.infra.sslsockspro.Constants.OVPN_PROFILE;
import static link.infra.sslsockspro.Constants.OVPN_RUN;
import static link.infra.sslsockspro.Constants.PROFILES_DIR;
import static link.infra.sslsockspro.Constants.SSLSOCKS_REMARK;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import link.infra.sslsockspro.database.ProfileDB;
import link.infra.sslsockspro.R;
import link.infra.sslsockspro.gui.activities.MainActivity;
import link.infra.sslsockspro.service.StunnelService;
import okio.BufferedSource;
import okio.Okio;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ProfileFragment.OnProfileFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class ProfileFragment extends Fragment {

	private static final String TAG = ProfileFragment.class.getSimpleName();
	private OnProfileFragmentInteractionListener mListener;

	private RecyclerView recyclerView;
	private TextView emptyView;
	private final List<String> remarks = ProfileDB.getRemarks();
	private final List<String> servers = ProfileDB.getServers();
	private TextView infoBox;

	public ProfileFragment() {
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_profile, container, false);
		setHasOptionsMenu(true);
		return view;
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.copy_logs).setVisible(false);
		menu.findItem(R.id.export_logs).setVisible(false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		//set the adapter
		Context context = view.getContext();

		recyclerView = view.findViewById(R.id.profile_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(context));
		recyclerView.setAdapter(new ProfileRecyclerViewAdapter(remarks, servers, mListener, new ProfileRecyclerViewAdapter.OnSetView() {
			@Override
			public void setRecyclerView() {
				recyclerView.setVisibility(View.VISIBLE);
				emptyView.setVisibility(View.GONE);
			}

			@Override
			public void setEmptyView() {
				recyclerView.setVisibility(View.GONE);
				emptyView.setVisibility(View.VISIBLE);
			}
		}));

		emptyView = view.findViewById(R.id.profile_empty_view);

		infoBox = view.findViewById(R.id.info_box);
		infoBox.setText(R.string.run_status_not_running);

		profileUpdateList(context);

		MainActivity.toggle.observe(getViewLifecycleOwner(), bool -> {
			if (bool) {
				MainActivity.getFabConnect().setBackgroundTintList(getResources().getColorStateList(R.color.fab_connecting));
				infoBox.setText(R.string.run_status_starting);
			} else {
				MainActivity.getFabConnect().setBackgroundTintList(getResources().getColorStateList(R.color.fab_disconnected));
				infoBox.setText(R.string.run_status_not_running);
			}
		});

		StunnelService.isRunning.observe(getViewLifecycleOwner(), bool -> {
			if (bool) {
				MainActivity.getFabConnect().setBackgroundTintList(getResources().getColorStateList(R.color.fab_connected));
				infoBox.setText(R.string.run_status_running);
			}
			else {
				MainActivity.getFabConnect().setBackgroundTintList(getResources().getColorStateList(R.color.fab_disconnected));
				infoBox.setText(R.string.run_status_not_running);
			}
		});
	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		if (context instanceof ProfileFragment.OnProfileFragmentInteractionListener) {
			mListener = (ProfileFragment.OnProfileFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString()
					+ " must implement OnListFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	/**
	 * This is a factory method to get a unique instance of This class
	 */


	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */

	public interface OnProfileFragmentInteractionListener {
		void onProfileEdit(int position);
		void onProfileDelete(int position);
		void onProfileSelect(int position);
	}

	public void profileUpdateList(Context context) {
		remarks.clear();
		servers.clear();
		ProfileDB.clear();
		File folder = new File(context.getFilesDir().getAbsolutePath() + "/" + PROFILES_DIR);
		String fileName;

		// sort the files
		File[] files = folder.listFiles();
		if (files == null) {
			return;
		} else {
			Arrays.sort(files, new Comparator<File>() {
				public int compare(File f1, File f2) {
					return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
				}
			});
		}
		for (File fileEntry : files) {
		//for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
			if (fileEntry.getPath().endsWith(EXT_CONF)) { // Only show config files
				fileName = fileEntry.getName();
				// remove leading whitespace + remove commented + keep empty lines
				//String pendingContent = fileContents.replaceAll("(?m)^\\s","").replaceAll("(?m)^#.*", "");
				BufferedSource buffFile = null;
				try {
					buffFile = Okio.buffer(Okio.source(fileEntry));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				Pattern ovpnPattern = Pattern.compile("^[\\s]*(?!#)" + OVPN_PROFILE + "[\\s]*=[\\s]*([a-zA-Z0-9_-]+)[\\s]*");
				Pattern ovpnRunPattern = Pattern.compile("^[\\s]*(?!#)" + OVPN_RUN + "[\\s]*=[\\s]*([a-zA-Z0-9_-]+)[\\s]*");
				Pattern remarkPattern = Pattern.compile("^[\\s]*(?!#)" + SSLSOCKS_REMARK + "[\\s]*=[\\s]*([a-zA-Z0-9_-]+)[\\s]*");
				Pattern serverPattern = Pattern.compile("^[\\s]*(?!#)" + "connect" + "[\\s]*=[\\s]*(.*)[\\s]*");
				String line = "";
				String ovpnString = "";
				String ovpnRunString = "";
				String remarkString = "";
				String serverString = "";
				while (true) {
					try {
						if (buffFile != null && ((line = buffFile.readUtf8Line()) == null)) break;
					} catch (IOException e) {
						Log.e(TAG, "File error", e);
					}
					Matcher ovpnMatcher = ovpnPattern.matcher(line);
					Matcher ovpnRunMatcher = ovpnRunPattern.matcher(line);
					Matcher remarkMatcher = remarkPattern.matcher(line);
					Matcher serverMatcher = serverPattern.matcher(line);
					if (ovpnMatcher.find()) {
						ovpnString = ovpnMatcher.group(1);
					}
					if (ovpnRunMatcher.find()) {
						ovpnRunString = ovpnRunMatcher.group(1);
					}
					if (remarkMatcher.find()) {
						remarkString = remarkMatcher.group(1);
					}
					if (serverMatcher.find()) {
						serverString = serverMatcher.group(1);
					}
				}
				if (ovpnRunString != null) {
					if (ovpnRunString.equals("yes"))
					{
						ProfileDB.addDB(fileName,remarkString,serverString ,ovpnString, TRUE);
					} else {
						ProfileDB.addDB(fileName,remarkString,serverString,ovpnString, FALSE);
					}
				}
			}
		}
		// Show text if there are no items
		if (remarks.isEmpty()) {
			recyclerView.setVisibility(View.GONE);
			emptyView.setVisibility(View.VISIBLE);
		} else {
			recyclerView.setVisibility(View.VISIBLE);
			emptyView.setVisibility(View.GONE);
		}
		Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
	}

	public RecyclerView getRecyclerView() {
		return recyclerView;
	}

	@Override
	public void onResume() {
		super.onResume();
		StunnelService.checkStatus(getActivity());
	}
}
