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

package link.infra.sslsockspro.gui.mngkey;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import link.infra.sslsockspro.R;

/**
 * A fragment representing the key list
 */
public class KeyFragment extends Fragment {

	private OnKeyFragmentInteractionListener mListener;
	private RecyclerView recyclerView;
	private TextView emptyView;
	private final List<KeyRecyclerViewAdapter.KeyItem> items = new ArrayList<>();
	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public KeyFragment() {
	}

	public static KeyFragment newInstance() {
		return new KeyFragment();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_key_list, container, false);
		setHasOptionsMenu(true);

		// Set the adapter
		Context context = view.getContext();

		recyclerView = view.findViewById(R.id.key_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(context));
		recyclerView.setAdapter(new KeyRecyclerViewAdapter(items, mListener));

		emptyView = view.findViewById(R.id.key_empty_view);

		keyUpdateList(context);
		return view;
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.action_import_profile).setVisible(false);
		menu.findItem(R.id.action_add_profile).setVisible(false);
		menu.findItem(R.id.copy_logs).setVisible(false);
		menu.findItem(R.id.export_logs).setVisible(false);
	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		if (context instanceof OnKeyFragmentInteractionListener) {
			mListener = (OnKeyFragmentInteractionListener) context;
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
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p/>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnKeyFragmentInteractionListener {
		void onKeyFragmentInteraction(KeyRecyclerViewAdapter.KeyItem item);
	}

	public void keyUpdateList(Context context) {
		items.clear();
		File folder = context.getFilesDir();
		for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
			if (fileEntry.getPath().endsWith(".p12") || fileEntry.getPath().endsWith(".pem")) { // Only show .p12 or .pem files
				items.add(new KeyRecyclerViewAdapter.KeyItem(fileEntry.getName()));
			}
		}
		Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();

		// Show text if there are no items
		if (items.isEmpty()) {
			recyclerView.setVisibility(View.GONE);
			emptyView.setVisibility(View.VISIBLE);
		} else {
			recyclerView.setVisibility(View.VISIBLE);
			emptyView.setVisibility(View.GONE);
		}
	}

}
