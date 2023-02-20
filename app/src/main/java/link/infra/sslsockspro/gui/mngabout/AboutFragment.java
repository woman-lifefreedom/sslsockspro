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

package link.infra.sslsockspro.gui.mngabout;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import link.infra.sslsockspro.R;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AboutFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AboutFragment extends Fragment {

	private ExpandableListView eView;
	private final List<AboutItem> items = new ArrayList<>();
	private static String[] javidnam_array;
	private static String comma;
	private static String htmlZza;
	private static String memo_title;
	private static String memo_content;
	private AboutExpandableListAdapter eAdapter;


	public AboutFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @return A new instance of fragment LogFragment.
	 */
	public static AboutFragment newInstance() {
		AboutFragment fragment = new AboutFragment();
		return fragment;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		// Inflate the layout for this fragment

		setHasOptionsMenu(true);
		return inflater.inflate(R.layout.fragment_about, container,false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		Context context = view.getContext();

		comma = getResources().getString(R.string.comma);
		String zza = getResources().getString(R.string.about_memo_content_zza);
		htmlZza = "<p><span style=\"color:#8e44ad\"><em><strong><span style=\"font-size:16px\">" + zza + "</span></strong></em></span></p>";
		String javidnam = getResources().getString(R.string.javidnam);
		javidnam_array = javidnam.split(",");
		Collections.shuffle(Arrays.asList(javidnam_array));
		String source_code = getResources().getString(R.string.source_code);

		memo_title = getResources().getString(R.string.about_memo_title);
		memo_content =
				htmlZza
				+ getResources().getString(R.string.about_memo_content)
				+ " <b>" + javidnam_array[0]
				+ comma + " " + javidnam_array[1]
				+ comma + " " + javidnam_array[2]
				+ comma + " " + javidnam_array[3]
				+ comma + " " + javidnam_array[4]
				+ "</b> " + getResources().getString(R.string.about_memo_content_p2);
		String about_us_title = getResources().getString(R.string.about_us_title);
		String about_us_content = getResources().getString(R.string.about_us_content);
		String copyright_title = getResources().getString(R.string.about_copyright_title);
		String copyright_content = getResources().getString(R.string.about_copyright_content)
				+ "<br>"
				+ "<p><a href=\"" + source_code  + "\">"
				+ "<span style=\"font-size:10px;\">"
				+ source_code+ "</span></a></p>";

		String privacy_title = getResources().getString(R.string.about_privacy_title);
		String privacy_content = getResources().getString(R.string.about_privacy_content);
		items.add(new AboutItem(memo_title, Html.fromHtml(memo_content)));
		items.add(new AboutItem(privacy_title, Html.fromHtml(privacy_content)));
		items.add(new AboutItem(copyright_title, Html.fromHtml(copyright_content)));
		items.add(new AboutItem(about_us_title, Html.fromHtml(about_us_content)));

		eView = view.findViewById(R.id.about_list);
		eAdapter = new AboutExpandableListAdapter(context,items);
		eView.setAdapter(eAdapter);
		eView.expandGroup(0);

		//final TextView aboutText = view.findViewById(R.id.about_text);
	}

	@Override
	public void onResume() {
		super.onResume();
		shuffleJavidnam();
		eAdapter.notifyDataSetChanged();
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.action_import_profile).setVisible(false);
		menu.findItem(R.id.action_add_profile).setVisible(false);
		menu.findItem(R.id.export_logs).setVisible(false);
		menu.findItem(R.id.copy_logs).setVisible(false);
		menu.findItem(R.id.export_logs).setVisible(false);
	}

	public void shuffleJavidnam() {
		if (javidnam_array != null ) {
			Collections.shuffle(Arrays.asList(javidnam_array));
			memo_content =	htmlZza
				+ getResources().getString(R.string.about_memo_content)
				+ " <b>" + javidnam_array[0]
				+ comma + " " + javidnam_array[1]
				+ comma + " " + javidnam_array[2]
				+ comma + " " + javidnam_array[3]
				+ comma + " " + javidnam_array[4]
				+ "</b> " + getResources().getString(R.string.about_memo_content_p2);
			items.set(0,new AboutItem(memo_title,Html.fromHtml(memo_content)));
		}
	}

}
