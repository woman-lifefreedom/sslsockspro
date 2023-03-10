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

package link.infra.sslsockspro.service;

import static link.infra.sslsockspro.Constants.APP_LOG;
import static link.infra.sslsockspro.Constants.CONFIG;
import static link.infra.sslsockspro.Constants.DUMMY_CONF;
import static link.infra.sslsockspro.Constants.OVPN_PROFILE;
import static link.infra.sslsockspro.Constants.OVPN_RUN;
import static link.infra.sslsockspro.Constants.PROFILES_DIR;
import static link.infra.sslsockspro.Constants.SERVICE_DIR;
import static link.infra.sslsockspro.Constants.SSLSOCKS_REMARK;
import static link.infra.sslsockspro.Constants.TUNNEL;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import link.infra.sslsockspro.BuildConfig;
import link.infra.sslsockspro.database.ProfileDB;
import link.infra.sslsockspro.R;
import link.infra.sslsockspro.gui.activities.MainActivity;
import link.infra.sslsockspro.gui.fragments.LogFragment;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class StunnelService extends Service {
	private static final String ACTION_STARTNOVPN = "link.infra.sslsockspro.service.action.STARTNOVPN";
	private static final String ACTION_RESUMEACTIVITY = "link.infra.sslsockspro.service.action.RESUMEACTIVITY";

	private static final String TAG = StunnelService.class.getSimpleName();

	private static final int NOTIFICATION_ID = 1;
	private static final String ACTION_STOP = "link.infra.sslsockspro.service.action.STOP";

	private static final MutableLiveData<Boolean> privateIsRunning = new MutableLiveData<>();
	public static final LiveData<Boolean> isRunning = privateIsRunning;

	private static boolean stunnelThreadCreated = false;
	private static boolean runningBool = false;
	private String configFullPath;

	private static boolean serviceStartedBool = false;

	private static final MutableLiveData<String> logDataPrivate = new MutableLiveData<>();
	public static final LiveData<String> logData = logDataPrivate;
	private String currLogValue = "";

	private static final long STREAM_READER_DELAY = 200;

	public native static int beginStunnel(String profile);
	public native static void endStunnel();
	public native static void reloadStunnel();
	static { System.loadLibrary("stunnel"); }

	@Override
	public void onCreate() {
		privateIsRunning.postValue(false);
		runningBool = false;
		Context context = getApplicationContext();
		createDummyConfig(context);
		setupDummyConfig(context);
		configFullPath = context.getFilesDir().getAbsolutePath() + "/" + SERVICE_DIR + "/" + CONFIG;
		if (!stunnelThreadCreated) {
			Thread stunnelThread = new Thread() {
				public void run() {
					beginStunnel(configFullPath);
				}
			};
			stunnelThreadCreated = true;
			stunnelThread.start();
		}

		super.onCreate();
	}

	/**
	 * Starts this service to perform action Foo with the given parameters. If
	 * the service is already performing a task this action will be queued.
	 *
	 * @see IntentService
	 */
	public static void start(Context context) {
		Intent intent = new Intent(context, StunnelService.class);
		intent.setAction(ACTION_STARTNOVPN);
		if (Build.VERSION.SDK_INT >= 26) {
			context.startForegroundService(intent);
		} else {
			context.startService(intent);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			final String action = intent.getAction();
			if (ACTION_STARTNOVPN.equals(action)) {
				handleStart();
			}
		}
		return START_NOT_STICKY;
	}

	/**
	 * Handle start action in the provided background thread with the provided
	 * parameters.
	 */
	private void handleStart() {
		Context context = getApplicationContext();
		setupConfigFile(context);
		reloadStunnel();
		privateIsRunning.postValue(true);
		runningBool = true;
		Log.d(TAG, "Stunnel started");

		showNotification();
		Log.d(TAG, "Service started");

		String logFullPath = context.getFilesDir().getAbsoluteFile() + "/" + APP_LOG;
		File log = new File(logFullPath);
		clearLog();
		TimerTask task = new TimerTask() {
			public void run() {
				try {
					readInputStream(StunnelService.this, Okio.buffer(Okio.source(log)));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		};
		Timer timer = new Timer();
		timer.schedule(task, STREAM_READER_DELAY);
	}

	public void onDestroy() {
		setupDummyConfig(getApplicationContext());
		reloadStunnel();
		privateIsRunning.postValue(false);
		runningBool = false;
		serviceStartedBool = false;

		Log.d(TAG, "Stunnel ended");
//		privateServiceStarted.postValue(false);
		removeNotification();
		super.onDestroy();
		Log.d(TAG, "Service ended");
	}

	public static boolean isRunningBool() {
		return runningBool;
	}

	public static boolean isServiceStartedBool() {
		return serviceStartedBool;
	}

	public static void checkStatus(Context context) {
		Intent localIntent = new Intent(ACTION_RESUMEACTIVITY);
		// Broadcasts the Intent to receivers in this app.
		LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	void readInputStream(final StunnelService context, final BufferedSource in) {
		Thread streamReader = new Thread(){
			public void run() {
				String line;
				try {
					while (runningBool) {
						if ((line = in.readUtf8Line()) != null) {
							context.appendLog(line);
						} else {
							Thread.sleep(20);
						}
					}
					while ((line = in.readUtf8Line()) != null) {
						context.appendLog(line);
					}
				} catch (InterruptedException | IOException e) {
					if (e instanceof InterruptedIOException) {
						// This is fine, it quit
					return;
				}
					Log.e(TAG, "Error reading stunnel log file: ", e);
				}
			}
		};
		streamReader.start();
	}


	public void appendLog(String value) {
		currLogValue += value + "\n";
		logDataPrivate.postValue(currLogValue);
	}

	public void clearLog() {
		currLogValue = "";
		logDataPrivate.postValue(currLogValue);
	}

	/**
	 * reads the initial configuration file and removes the content that stunnel does not need
	 * @param context: Application context
	 * @return true on success, false on failure
	 */
	static boolean setupConfigFile(Context context) {
		String profileName = ProfileDB.getFile();
		String profileFullPath = context.getFilesDir().getAbsolutePath() + "/" + PROFILES_DIR + "/" + profileName;
		File profile = new File(profileFullPath);
		String configFullPath = context.getFilesDir().getAbsolutePath() + "/" + SERVICE_DIR + "/" + CONFIG;
		File config = new File(configFullPath);
		try {
			final BufferedSource in = Okio.buffer(Okio.source(profile));
			BufferedSink out = Okio.buffer(Okio.sink(config));
			String fileContents = in.readUtf8();
			// remove leading whitespace + remove commented + remove empty lines
			String pendingContent = fileContents.replaceAll("(?m)^[\\s]*","").replaceAll("(?m)^#.*", "").replaceAll("(?m)^[ \t]*\r?\n", "");
			String[] lines = pendingContent.split("\n");
			for(int i=0;i<lines.length;i++){
				if (lines[i].startsWith(OVPN_PROFILE)
						| lines[i].startsWith(OVPN_RUN)
						| lines[i].startsWith(SSLSOCKS_REMARK)
						| lines[i].startsWith(TUNNEL)
						| lines[i].startsWith("pid")
						| lines[i].startsWith("output")
						| lines[i].startsWith("log")) {
					lines[i]="";
				}
			}
			List<String> linesList = new ArrayList<>(Arrays.asList(lines));
			linesList.add(0,"output =" + context.getFilesDir().getPath() + "/" + APP_LOG);
			linesList.add(0,"log = append");
//			if (LogFragment.clearLogsOnNewConnect) {
//				linesList.add(0,"log = overwrite");
//			} else {
//				linesList.add(0,"log = append");
//			}
			linesList.add(0,"debug = 7");
			lines = linesList.toArray(lines);
			pendingContent = TextUtils.join("\n",lines);
			out.writeUtf8(pendingContent + "\n");
			out.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	static boolean setupDummyConfig(Context context) {
		String dummyConfigFullPath = context.getFilesDir().getAbsoluteFile() + "/" +SERVICE_DIR + "/" + DUMMY_CONF;
		File dummyConfig = new File(dummyConfigFullPath);
		String configFullPath = context.getFilesDir().getAbsolutePath() + "/" + SERVICE_DIR + "/" + CONFIG;
		File config = new File(configFullPath);
		try {
			final BufferedSource in = Okio.buffer(Okio.source(dummyConfig));
			BufferedSink out = Okio.buffer(Okio.sink(config));
			String dummyConfigContents = in.readUtf8();
			out.writeUtf8(dummyConfigContents);
			out.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean createDummyConfig(Context context) {
		File dummyConf = new File(context.getFilesDir().getAbsoluteFile() + "/" +SERVICE_DIR + "/" + DUMMY_CONF);
//		if (dummyConf.exists() ) {
//			return true; // already created
//		}
		String logPolicy;
		if (LogFragment.clearLogsOnNewConnect) {
			logPolicy = "overwrite";
		} else {
			logPolicy = "append";
		}
		try (BufferedSink out = Okio.buffer(Okio.sink(dummyConf))) {
			out.writeUtf8(
					"output =" + context.getFilesDir().getPath() + "/" + APP_LOG + "\n" +
							"log = " + logPolicy + "\n" +
							"debug = 7\n" +
							"foreground = yes\n" +
							"client = yes\n" +
							"[offline_proxy_tunnel]\n" +
							"connect = \n" +
							"accept = 127.0.0.1:54321\n"
			);
			return true;
		} catch (IOException e) {
			Log.e(TAG, "Failed dummy config file creation: ", e);
			return false;
		}
	}

	private static boolean hasBeenUpdated(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		int versionCode = sharedPreferences.getInt("VERSION_CODE", 0);

		if (versionCode != BuildConfig.VERSION_CODE) {
			sharedPreferences.edit().putInt("VERSION_CODE", BuildConfig.VERSION_CODE).apply();
			return true;
		}
		return false;
	}

	private void showNotification() {
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
						.setSmallIcon(R.drawable.ic_service_running)
						.setContentTitle(getString(R.string.app_name_full))
						.setContentText(getString(R.string.notification_desc))
						.setCategory(NotificationCompat.CATEGORY_SERVICE)
						.setPriority(NotificationCompat.PRIORITY_DEFAULT)
						.setSilent(true)
						.setOngoing(true);
		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, MainActivity.class);
		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(MainActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);

		PendingIntent resultPendingIntent;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			resultPendingIntent = stackBuilder.getPendingIntent(0,
					PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE );
		}
		else {
			resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT );
		}
		mBuilder.setContentIntent(resultPendingIntent);

		Intent serviceStopIntent = new Intent(this, ServiceStopReceiver.class);
		serviceStopIntent.setAction(ACTION_STOP);
		PendingIntent serviceStopIntentPending;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			serviceStopIntentPending = PendingIntent.getBroadcast(this, 1, serviceStopIntent,
					PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		}
		else {
			serviceStopIntentPending = PendingIntent.getBroadcast(this, 1, serviceStopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		}
		mBuilder.addAction(R.drawable.ic_stop, "Stop", serviceStopIntentPending);

		// Ensure that the service is a foreground service
		startForeground(NOTIFICATION_ID, mBuilder.build());
		serviceStartedBool = true;
//		privateServiceStarted.postValue(true);
		Log.d(TAG, "Service started");
	}

	private void removeNotification() {
		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (mNotificationManager != null) {
			mNotificationManager.cancel(NOTIFICATION_ID);
		}
	}

/*
END OF STUNNEL SERVICE
 */
}
