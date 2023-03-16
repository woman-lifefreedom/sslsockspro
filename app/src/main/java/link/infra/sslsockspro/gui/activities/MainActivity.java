/*
 * Modified by WOMAN-LIFE-FREEDOM 2022
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

package link.infra.sslsockspro.gui.activities;

import static link.infra.sslsockspro.Constants.APP_LOG;
import static link.infra.sslsockspro.Constants.EXT_CONF;
import static link.infra.sslsockspro.Constants.LAST_SELECTED_PROFILE;
import static link.infra.sslsockspro.Constants.LOG_ISO;
import static link.infra.sslsockspro.Constants.LOG_LEVEL_DEFAULT;
import static link.infra.sslsockspro.Constants.LOG_NONE;
import static link.infra.sslsockspro.Constants.LOG_SHORT;
import static link.infra.sslsockspro.Constants.PROFILES_DIR;
import static link.infra.sslsockspro.Constants.PROFILE_DATABASE;
import static link.infra.sslsockspro.Constants.SERVICE_DIR;
import static link.infra.sslsockspro.database.ProfileDB.NEW_PROFILE;
import static link.infra.sslsockspro.gui.fragments.KeyEditActivity.ARG_EXISTING_FILE_NAME;
import static link.infra.sslsockspro.gui.fragments.ProfileEditActivity.ACTION_DELETED;
import static link.infra.sslsockspro.gui.fragments.ProfileEditActivity.ACTION_EDITED;
import static link.infra.sslsockspro.gui.fragments.ProfileEditActivity.ACTION_IMPORTED;
import static link.infra.sslsockspro.gui.fragments.ProfileEditActivity.ARG_ACTION;
import static link.infra.sslsockspro.gui.fragments.ProfileEditActivity.ARG_POSITION;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import link.infra.sslsockspro.database.FileOperation;
import link.infra.sslsockspro.database.ProfileDB;
import link.infra.sslsockspro.R;
import link.infra.sslsockspro.gui.OpenVPNIntegrationHandler;
import link.infra.sslsockspro.gui.fragments.KeyEditActivity;
import link.infra.sslsockspro.gui.fragments.KeyFragment;
import link.infra.sslsockspro.gui.fragments.KeyRecyclerViewAdapter;
import link.infra.sslsockspro.gui.fragments.AboutFragment;
import link.infra.sslsockspro.gui.fragments.LogFragment;
import link.infra.sslsockspro.gui.fragments.ProfileEditActivity;
import link.infra.sslsockspro.gui.fragments.ProfileFragment;
import link.infra.sslsockspro.gui.fragments.ProfileRecyclerViewAdapter;
import link.infra.sslsockspro.service.StunnelService;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class MainActivity extends AppCompatActivity
		implements KeyFragment.OnKeyFragmentInteractionListener, ProfileFragment.OnProfileFragmentInteractionListener
{

	private FloatingActionButton fabAddKey;
	private static FloatingActionButton fabConnect;

	public static final String CHANNEL_ID = "NOTIFY_CHANNEL_1";
	private WeakReference<KeyFragment> keysFragment;
	private WeakReference<ProfileFragment> profilesFragment;
	private WeakReference<AboutFragment> aboutFragment;
	private LogFragment logFragment;
	private OpenVPNIntegrationHandler openVPNIntegrationHandler = null;
	private static final String TAG = MainActivity.class.getSimpleName();
	SharedPreferences prefs;
	SharedPreferences.Editor prefsEditor;

	private static final long OVPN_START_DELAY = 100L;
	private static final long AUTORUN_BY_SELECT_DELAY = 200L;

	private static boolean oVPNBound = false;

	private static final MutableLiveData<String> stunnelVersionStringPrivate = new MutableLiveData<>();
	public static final LiveData<String> stunnelVersionString = stunnelVersionStringPrivate;

	private static final MutableLiveData<String> logFormatPrivate = new MutableLiveData<>();
	public static final LiveData<String> logFormat = logFormatPrivate;

	private static final MutableLiveData<Integer> logLevelPrivate = new MutableLiveData<>();
	public static final LiveData<Integer> logLevel = logLevelPrivate;

	private static final MutableLiveData<String> logDataFormattedLeveledPrivate = new MutableLiveData<>();
	public static final LiveData<String> logDataFormattedLeveled = logDataFormattedLeveledPrivate;

	private static final MutableLiveData<Boolean> togglePrivate = new MutableLiveData<>();
	public static final LiveData<Boolean> toggle = togglePrivate;

	private static boolean requestsDisabled = false;
	private static boolean autoConnectFlag = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (setupServiceDir(getApplicationContext())) {
			//stunnelVersionStringPrivate.postValue(StunnelService.checkStunnelVersion(getApplicationContext()));
			stunnelVersionStringPrivate.postValue("5.67");
		}
		loadDatabase();

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		ViewPager2 viewPager = findViewById(R.id.container);
		viewPager.setAdapter(new SectionsPagerAdapter(this));

		TabLayout tabLayout = findViewById(R.id.tabs);
		String[] tabNames = getResources().getStringArray(R.array.tabs_array);
		new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(tabNames[position])).attach();

		fabAddKey = findViewById(R.id.fab_key);

		fabAddKey.setOnClickListener(view ->
				keyEditRequestLauncher.launch(new Intent(MainActivity.this, KeyEditActivity.class)));

		fabConnect = findViewById(R.id.fab_connect);
		fabConnect.setOnClickListener(v->fabConnectAction());

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefsEditor = prefs.edit();
		prefs.getInt(LAST_SELECTED_PROFILE,-1);
		ProfileDB.setLastSelectedPosition(prefs.getInt(LAST_SELECTED_PROFILE,-1));

		viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
			@Override
			public void onPageSelected(int position) {
				// show key add button in key fragment
				if (position == 2) {
					fabAddKey.show();
				} else {
					fabAddKey.hide();
				}
				if (position == 0) {
					fabConnect.show();
				} else {
					fabConnect.hide();
				}
			}
		});

		logFormatPrivate.postValue(LOG_SHORT);
		logLevelPrivate.postValue(LOG_LEVEL_DEFAULT);

		// Create the NotificationChannel, but only on API 26+ because
		// the NotificationChannel class is new and not in the support library
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			CharSequence name = getString(R.string.notification_channel);
			String description = getString(R.string.notification_desc);
			int importance = NotificationManager.IMPORTANCE_DEFAULT;
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
			channel.setDescription(description);
			// Register the channel with the system; you can't change the importance
			// or other notification behaviors after this
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			if (notificationManager != null) {
				notificationManager.createNotificationChannel(channel);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		//menu.findItem(R.id.action_settings).setVisible(false);
	return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		if (id == R.id.action_settings) {
			Intent intent = new Intent(this, AdvancedSettingsActivity.class);
			startActivity(intent);
			return true;
		}

		if (id == R.id.action_add_profile) {
			Intent intent = new Intent(this, ProfileEditActivity.class);
			intent.putExtra(ARG_POSITION, NEW_PROFILE);
			profileEditRequestLauncher.launch(intent);
			return true;
		}

		if (id == R.id.action_import_profile) {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("*/*");
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			profileImportRequestLauncher.launch(Intent.createChooser(intent, getString(R.string.title_activity_profile_edit)));
			return true;
		}

		if (id == R.id.copy_logs) {
			String logs = logDataFormattedLeveled.getValue();
			ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("logs", logs);
			clipboard.setPrimaryClip(clip);
			Toast.makeText(this, R.string.logs_copied, Toast.LENGTH_SHORT).show();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	class SectionsPagerAdapter extends FragmentStateAdapter {
		public SectionsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
			super(fragmentActivity);
		}

		@Override
		public int getItemCount() {
			return 4;
		}

		@NonNull
		@Override
		public Fragment createFragment(int position) {
			switch (position) {
				case 0:
					profilesFragment = new WeakReference<>(new ProfileFragment());
					StunnelService.checkStatus(MainActivity.this);
					return profilesFragment.get();
				case 1:
					logFragment = LogFragment.newInstance(new LogFragment.OnLogFragmentInteractionListener() {
						@Override
						public void onLogFormatChanged(RadioGroup group, int checkedId) {
							RadioButton checkedRadioButton = (RadioButton)group.findViewById(checkedId);
							String checkedText = checkedRadioButton.getText().toString();
							setLogFormat(checkedText);
							}

						@Override
						public void onLogLevelChanged(int position) {
							setLogLevel(position);
						}
						;
					});
					return logFragment;
				case 2:
					keysFragment = new WeakReference<>(new KeyFragment());
					return keysFragment.get();
				case 3:
					aboutFragment = new WeakReference<>(new AboutFragment());
					return aboutFragment.get();
			}
			throw new RuntimeException("Invalid fragment reached");
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == OpenVPNIntegrationHandler.PERMISSION_REQUEST) {
			if (resultCode == RESULT_OK && openVPNIntegrationHandler != null) {
				openVPNIntegrationHandler.doVpnPermissionRequest();
			}
		} else if (requestCode == OpenVPNIntegrationHandler.VPN_PERMISSION_REQUEST) {
			if (resultCode == RESULT_OK && openVPNIntegrationHandler != null) {
				openVPNIntegrationHandler.connectProfile();
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		StunnelService.checkStatus(this);
//		if (profilesFragment != null) {
//			profilesFragment.get().profileUpdateList(getApplicationContext());
//		}
	}

	@Override
	protected void onDestroy() {
		if (openVPNIntegrationHandler != null) {
			openVPNIntegrationHandler.unbind();
		}
		super.onDestroy();
	}

	public void onKeyFragmentInteraction(KeyRecyclerViewAdapter.KeyItem item) {
		Intent intent = new Intent(MainActivity.this, KeyEditActivity.class);
		intent.putExtra(ARG_EXISTING_FILE_NAME, item.filename);
		keyEditRequestLauncher.launch(intent);
	}

	private final ActivityResultLauncher<Intent> keyEditRequestLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
		if (result.getResultCode() == RESULT_OK) {
			if (keysFragment != null) {
				KeyFragment frag = keysFragment.get();
				if (frag != null) {
					frag.keyUpdateList(this); // Ensure list is up to date
				}
			}
		}
	});

	public void onProfileEdit(int position) {
		Intent intent = new Intent(MainActivity.this, ProfileEditActivity.class);
		intent.putExtra(ARG_POSITION, position);
		profileEditRequestLauncher.launch(intent);
	}

	private final ActivityResultLauncher<Intent> profileEditRequestLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
		if (result.getResultCode() == RESULT_OK) {
			int position = Objects.requireNonNull(result.getData()).getIntExtra(ARG_POSITION, NEW_PROFILE);
			int action = Objects.requireNonNull(result.getData()).getIntExtra(ARG_ACTION, ACTION_EDITED);
			if (position != NEW_PROFILE) {
				if (action == ACTION_DELETED) {
					Objects.requireNonNull(profilesFragment.get().getRecyclerView().getAdapter()).notifyItemRemoved(position);
					if (ProfileDB.getSize() == 0) {
						profilesFragment.get().updateView();
					}
				} else {
					Objects.requireNonNull(profilesFragment.get().getRecyclerView().getAdapter()).notifyItemChanged(position);
				}
			} else {
				if (ProfileDB.getSize() == 1) {
					profilesFragment.get().updateView();
				}
				Objects.requireNonNull(profilesFragment.get().getRecyclerView().getAdapter()).notifyItemInserted(ProfileDB.getSize()-1);
			}
		}
	});

	public void onProfileDelete(int position) {
		try {
			ProfileDB.removeProfile(getApplicationContext(),position);
		} catch (IOException e) {
			Log.e(TAG, "failed to write to database", e);
		}
	}

	public void onProfileSelect(int position) {
		prefsEditor.putInt(LAST_SELECTED_PROFILE, ProfileDB.getPosition());
		prefsEditor.apply();
		if (!requestsDisabled) {
			if (StunnelService.isRunningBool()) {
				requestsDisabled = true;
				autoConnectFlag = true;
				TimerTask task = new TimerTask() {
					public void run() {
						fabConnectAction();
					}
				};
				Timer timer = new Timer();
				fabConnectAction();
				timer.schedule(task, AUTORUN_BY_SELECT_DELAY);
			}
		}
	}

	public void onServiceStart() {
		if (ProfileDB.getPosition() == -1) {
			return;
		}
		StunnelService.start(getApplicationContext());
		startOVPN();
	}

	public void onServiceStop() {
		stopOVPN();
		Intent intent = new Intent(this, StunnelService.class);
		stopService(intent);
	}
	public void fabConnectAction() {
		// toggle is false by default. This means first fabConnectionAction corresponds to start the service.
		if (!Boolean.TRUE.equals(toggle.getValue()) && !StunnelService.isRunningBool() && !StunnelService.isServiceStartedBool()) {
			if (ProfileDB.getSize() == 0) {
				return;
			}
			togglePrivate.postValue(true);
			onServiceStart();
			if (autoConnectFlag) {
				TimerTask task = new TimerTask() {
					public void run() {
						requestsDisabled = false;
						autoConnectFlag = false;
					}
				};
				Timer timer = new Timer();
				timer.schedule(task, AUTORUN_BY_SELECT_DELAY);
			}
		} else if (!Boolean.FALSE.equals(toggle.getValue()) && StunnelService.isRunningBool() && StunnelService.isServiceStartedBool()) {
			togglePrivate.postValue(false);
			onServiceStop();
		}
	}

	public static void falseToggle() {
		togglePrivate.postValue(false);
	}

	public void startOVPN() {
		if (ProfileDB.getRunOvpn()) {
			openVPNIntegrationHandler = new OpenVPNIntegrationHandler(MainActivity.this, () -> {}, ProfileDB.getOvpn(), false);
			TimerTask task = new TimerTask() {
				public void run() {
					if (openVPNIntegrationHandler != null) {
						oVPNBound = openVPNIntegrationHandler.bind();
					}
				}
			};
			Timer timer = new Timer();
			timer.schedule(task, OVPN_START_DELAY);
		}
	}

	public void stopOVPN() {
		if (oVPNBound) {
			oVPNBound = false;
			if (openVPNIntegrationHandler != null) {
				openVPNIntegrationHandler.disconnect();
			}
		}
	}

	public static FloatingActionButton getFabConnect() {
		return fabConnect;
	}

	public static void formatLog(String log, String logFormat, Integer logLevel) {
		if (log == null) { return; }
		if (logFormat == null) {return;}
		if (logLevel == null) {return;}
		String logFormatted = "";
		String dateRegex = "(\\d{4}\\.\\d{2}\\.\\d{2})"; //$1
		String timeRegex = "(\\d{2}:\\d{2})(:\\d{2})"; // $2 $3
		String logRegex = "(?m)^" + dateRegex + "\\s" +  timeRegex + "\\s" + "(.*)$"; //$4
		switch(logFormat) {
			case LOG_NONE:
				logFormatted = log.replaceAll(logRegex,"$4");
				break;
			case LOG_SHORT:
				logFormatted = log.replaceAll(logRegex,"$2 $4");
				break;
			case LOG_ISO:
				logFormatted = log;
				break;
		}
		String logFormattedLeveled = "";
		switch(logLevel) {
			case 3:
				logFormattedLeveled = logFormatted.replaceAll("(?m).*LOG[4-7].*(?:\\r?\\n)?","");
				break;
			case 4:
				logFormattedLeveled = logFormatted.replaceAll("(?m).*LOG[5-7].*(?:\\r?\\n)?","");
				break;
			case 5:
				logFormattedLeveled = logFormatted.replaceAll("(?m).*LOG[6-7].*(?:\\r?\\n)?","");
				break;
			case 6:
				logFormattedLeveled = logFormatted.replaceAll("(?m).*LOG[7].*(?:\\r?\\n)?","");
				break;
			case 7:
				logFormattedLeveled = logFormatted;
				break;
		}
		logDataFormattedLeveledPrivate.postValue(logFormattedLeveled);
	}

	public static void setLogLevel(int logLevel) {
		logLevelPrivate.postValue(logLevel);
	}

	public static void setLogFormat(String logFormat) {
		logFormatPrivate.postValue(logFormat);
	}

	private final ActivityResultLauncher<Intent> profileImportRequestLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
		if (result.getResultCode() == RESULT_OK) {
			Intent resultData = result.getData();
			if (resultData != null) {
				Uri fileData = resultData.getData();
				if (fileData != null) {
					InputStream inputStream;
					try {
						inputStream = getContentResolver().openInputStream(fileData);
						if (inputStream == null) { // Just to keep the linter happy that I'm doing null checks
							throw new FileNotFoundException();
						}
					} catch (FileNotFoundException e) {
						Log.e(TAG, "Failed to read imported file", e);
						Toast.makeText(this, R.string.file_read_fail, Toast.LENGTH_SHORT).show();
						return;
					}
					String fileContents;
					try (BufferedSource in = Okio.buffer(Okio.source(inputStream))) {
						fileContents = in.readUtf8();
					} catch (IOException e) {
						Log.e(TAG, "Failed to read imported file", e);
						Toast.makeText(this, R.string.file_read_fail, Toast.LENGTH_SHORT).show();
						return;
					}
					String fileName = FileOperation.getFileName(fileData, getApplicationContext());
					if ( !fileName.endsWith(EXT_CONF) ) {
						Toast.makeText(this, R.string.profile_name_ext, Toast.LENGTH_SHORT).show();
						return;
					}
					if (ProfileDB.parseProfile(fileContents)) {
						try {
							ProfileDB.saveProfile(fileContents, getApplicationContext(), NEW_PROFILE);
							if (ProfileDB.getSize() == 1) {
								profilesFragment.get().updateView();
							}
							Objects.requireNonNull(profilesFragment.get().getRecyclerView().getAdapter()).notifyItemInserted(ProfileDB.getSize()-1);
							Toast.makeText(this, R.string.action_profile_added, Toast.LENGTH_SHORT).show();
						} catch (IOException e) {
							Log.e(TAG, "Failed sslsockspro profile writing.", e);
							Toast.makeText(this, R.string.file_write_fail, Toast.LENGTH_SHORT).show();
						}
					} else {
						Log.e(TAG, "file content is wrong");
						//Toast.makeText(this, R.string.file_write_fail, Toast.LENGTH_SHORT).show();
					}
//					profilesFragment.get().profileUpdateList(getApplicationContext());
				}
			}
		}
	});

	public Context getContext() {
		return getApplicationContext();
	}

	private boolean setupServiceDir(Context context) {
		boolean sd = true ,pd = true ,lf = true;
		String serviceDirPath = context.getFilesDir().getAbsolutePath() + "/" +SERVICE_DIR;
		File serviceDir = new File(serviceDirPath);
		if (!serviceDir.exists()) {
			sd = serviceDir.mkdirs();
		}
		String profilesPath = context.getFilesDir().getAbsolutePath() + "/" + PROFILES_DIR;
		File profilesDir = new File(profilesPath);
		if (!profilesDir.exists()) {
			pd = profilesDir.mkdirs();
		}
		String logFullPath = context.getFilesDir().getAbsoluteFile() + "/" + APP_LOG;
		File log = new File(logFullPath);
		if (!log.exists()){
			try {
				lf = log.createNewFile();
			} catch (IOException e) {
				Log.e(TAG, "Log file creation error", e);
			}
		}
		return (sd && pd && lf);
	}

	private void loadDatabase() {
		File db = new File(getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + PROFILE_DATABASE);
		if (!db.exists()) {
			try {
				ProfileDB.updateProfilesFromConfigFiles(getApplicationContext());
			} catch (IOException e) {
				Log.e(TAG, "File Operation Error", e);
			}
		} else {
			try {
				ProfileDB.loadProfilesFromDatabase(getApplicationContext());
			} catch (IOException e) {
				Log.e(TAG, "Error reading the database", e);
			}
		}
	}


//	// Get the file name for importing a file from a Uri
//	private String getFileName(Uri uri) {
//		String result = null;
//		if ("content".equals(uri.getScheme())) {
//			try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
//				if (cursor != null && cursor.moveToFirst()) {
//					result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
//				}
//			}
//		}
//		if (result == null) {
//			result = Objects.requireNonNull(uri.getPath());
//			int cut = result.lastIndexOf('/');
//			if (cut != -1) {
//				result = result.substring(cut + 1);
//			}
//		}
//		return result;
//	}

//	private void saveFile() {
//		String fileName = UUID.randomUUID().toString();
//		File fileConf = new File(getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + fileName + EXT_CONF);
//		try (BufferedSink out = Okio.buffer(Okio.sink(fileConf))) {
//			out.writeUtf8(fileContents);
//			out.close();
//			setResult(RESULT_OK);
//		} catch (IOException e) {
//			Toast.makeText(this, R.string.file_write_fail, Toast.LENGTH_SHORT).show();
//			Log.e(TAG, "Failed stunnel .conf file writing: ", e);
//		}
//		Toast.makeText(this, R.string.action_profile_added, Toast.LENGTH_SHORT).show();
//	}

}
