/*
 * Modified by WOMAN-LIFE-FREEDOM
 * (First release: 2017-2021 comp500)
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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import link.infra.sslsockspro.database.ProfileDB;
import link.infra.sslsockspro.R;

public class ProfileRecyclerViewAdapter extends RecyclerView.Adapter<ProfileRecyclerViewAdapter.ViewHolder> {

    private final ProfileFragment.OnProfileFragmentInteractionListener mListener;
    public final OnSetView setView;
    private static final long CLICK_TIME_INTERVAL = 100;
    private long mLastClickTime;

//    int selectedItem; // -1 means no selected item by default.

    public ProfileRecyclerViewAdapter(ProfileFragment.OnProfileFragmentInteractionListener listener,
                                      OnSetView setView) {
        this.mListener = listener;
        this.setView = setView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        if (ProfileDB.getSize() == 0) {
//            // No items in the database
//            selectedItem = -1;
//            ProfileDB.setPosition(-1);
//        } else if (ProfileDB.getPosition() == -1) {
//            // first time the app is running
//            if (ProfileDB.getLastSelectedPosition() > ProfileDB.getSize()) {
//                selectedItem = 0;
//                ProfileDB.setPosition(0);
//            } else {
//                // last selected item is valid
//                selectedItem = ProfileDB.getLastSelectedPosition();
//                ProfileDB.setPosition(ProfileDB.getLastSelectedPosition());
//            }
//        } else {
//            selectedItem = ProfileDB.getPosition();
//        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.profile_item, parent, false);
        return new ViewHolder(view);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final View mEdit;
        final View mDelete;
        // view region for onClickListener
        final View mSelect;
        // region to change the color of selected item
        final View mIndicate;
        final TextView mRemarkView;
        final TextView mServerView;

        ViewHolder(View view) {
            super(view);
            mView = view;
            mEdit = view.findViewById(R.id.layout_edit);
            mDelete = view.findViewById(R.id.layout_remove);
            mSelect = view.findViewById(R.id.info_container);
            mIndicate = view.findViewById(R.id.layout_indicator);
            mRemarkView = view.findViewById(R.id.tv_name);
            mServerView = view.findViewById(R.id.tv_statistics);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.mRemarkView.setText(ProfileDB.getRemark(position));
        holder.mServerView.setText(ProfileDB.getHost(position, 0));

//        if (ProfileDB.getSize() > 0 && selectedItem == -1) {
//            selectedItem = 0;
//            ProfileDB.setPosition(0);
//            ProfileDB.setLastSelectedPosition(0);
//        }

        if (position != ProfileDB.getPosition()) {
            holder.mIndicate.setBackgroundColor(0);
        } else {
            holder.mIndicate.setBackgroundResource(R.color.profileSelect);
        }

        if (ProfileDB.getEncrypted(position)) {
            holder.mEdit.setVisibility(View.GONE);
        }

        holder.mEdit.setOnClickListener(v -> {
            if (null != mListener) {
                long now = System.currentTimeMillis();
                if (now - mLastClickTime < CLICK_TIME_INTERVAL) {
                    return;
                }
                mLastClickTime = now;
                // Notify the active callbacks interface (the activity, if the
                // fragment is attached to one) that an item has been selected.
                mListener.onProfileEdit(position);
            }
        });
        holder.mDelete.setOnClickListener(v -> {
            if (null != mListener) {
                long now = System.currentTimeMillis();
                if (now - mLastClickTime < CLICK_TIME_INTERVAL) {
                    return;
                }
                mLastClickTime = now;
                // first delete the item and update the list
                mListener.onProfileDelete(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position,ProfileDB.getSize());
                if (position < ProfileDB.getPosition() || ProfileDB.getSize() == ProfileDB.getPosition()) {
                    ProfileDB.setPosition(ProfileDB.getPosition()-1);
                    notifyItemChanged(ProfileDB.getPosition());
                }
                if (ProfileDB.getSize() == 0) {
                    setView.setEmptyView();
                } else {
                    setView.setRecyclerView();
                }
                // Notify the active callbacks interface (the activity, if the
                // fragment is attached to one) that an item has been selected.
            }
        });
        holder.mSelect.setOnClickListener(v -> {
            if (null != mListener) {
                long now = System.currentTimeMillis();
                if (now - mLastClickTime < CLICK_TIME_INTERVAL) {
                    return;
                }
                mLastClickTime = now;
                if (position != RecyclerView.NO_POSITION) {
                    if (position == ProfileDB.getPosition()) {
                        return;// Here, I don't want to generate a click event on an already selected item.
                    }
//                    int currentSelected = selectedItem;// Create a temp var to deselect an existing one, if any.
                    int currentSelected = ProfileDB.getPosition();// Create a temp var to deselect an existing one, if any.
//                    selectedItem = position;/selectedItem/ Check item.
                    ProfileDB.setPosition(position);
                    if (currentSelected != RecyclerView.NO_POSITION) {
                        notifyItemChanged(currentSelected);// Deselected the previous item.
                    }
//                    notifyItemChanged(selectedItem);// Select the current item.
                    notifyItemChanged(ProfileDB.getPosition());// Select the current item.
//                    ProfileDB.setPosition(selectedItem);
//                    ProfileDB.setLastSelectedPosition(selectedItem);
                    mListener.onProfileSelect(position);
                }
                // Notify the active callbacks interface (the activity, if the
                // fragment is attached to one) that an item has been selected.
            }
        });
    }

    @Override
    public int getItemCount() {
        return ProfileDB.getSize();
    }

    public interface OnSetView {
        void setRecyclerView();
        void setEmptyView();
    }

}