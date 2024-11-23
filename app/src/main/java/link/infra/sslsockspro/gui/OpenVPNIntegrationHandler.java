/*
 * SSLSocks Pro
 * Copyright (C) 2022-2023 WOMAN-LIFE-FREEDOM
 *
 * Portions of SSLSocks Pro contain code derived from, or inspired by
 * SSLSocks (https://github.com/comp500/SSLSocks) under
 * the GNU General Public License (GPL) version 3 or later:
 * The copyright notice of the original code is:
 *
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

package link.infra.sslsockspro.gui;

import static link.infra.sslsockspro.Constants.EXT_CONF;
import static link.infra.sslsockspro.Constants.EXT_OVPN;
import static link.infra.sslsockspro.Constants.PROFILES_DIR;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

import de.blinkt.openvpn.api.APIVpnProfile;
import de.blinkt.openvpn.api.IOpenVPNAPIService;
import link.infra.sslsockspro.database.CryptoManager;
import link.infra.sslsockspro.database.ProfileDB;
import okio.BufferedSource;
import okio.Okio;

public class OpenVPNIntegrationHandler {
    private IOpenVPNAPIService srv = null;
    public static final int PERMISSION_REQUEST = 100;
    public static final int VPN_PERMISSION_REQUEST = 101;

    public static final int OVPN_CONNECT_PROFILE = 101;
    public static final int OVPN_ADD_PROFILE = 101;
    public static final int OVPN_REMOVE_PROFILE = 101;

    private static final String TAG = OpenVPNIntegrationHandler.class.getSimpleName();
    private final WeakReference<Context> ctxRef;
    private boolean isActivity = true;
    private final Runnable doneCallback;
    private final String profileName;
    private final boolean shouldDisconnect;
    private int position;

    public OpenVPNIntegrationHandler(Activity ctx, Runnable doneCallback, String profile, boolean shouldDisconnect) {
        this.ctxRef = new WeakReference<>(ctx);
        this.doneCallback = doneCallback;
        this.profileName = profile;
        this.shouldDisconnect = shouldDisconnect;
        Log.d(TAG, "created");
    }

    public OpenVPNIntegrationHandler(Context ctx, Runnable doneCallback, String profile, boolean shouldDisconnect) {
        isActivity = false;
        this.ctxRef = new WeakReference<>(ctx);
        this.doneCallback = doneCallback;
        this.profileName = profile;
        this.shouldDisconnect = shouldDisconnect;
        Log.d(TAG, "created");
    }

    public OpenVPNIntegrationHandler(Activity ctx, Runnable doneCallback, int position, boolean shouldDisconnect) {
        this.ctxRef = new WeakReference<>(ctx);
        this.doneCallback = doneCallback;
        this.position = position;
        this.profileName = ProfileDB.getOvpn();
        this.shouldDisconnect = shouldDisconnect;
        Log.d(TAG, "created");
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            srv = IOpenVPNAPIService.Stub.asInterface(service);
            try {
                Intent intent = srv.prepare(ctxRef.get().getPackageName());
                if (intent != null && isActivity) {
                    Log.d(TAG, "requesting permission");
                    ((Activity)ctxRef.get()).startActivityForResult(intent, PERMISSION_REQUEST);
                } else {
                    doVpnPermissionRequest();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to connect to OpenVPN", e);
            } catch (SecurityException e) {
                Log.e(TAG, "User permission to access IOpenVPNAPIService was rejected", e);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            srv = null;
        }

        @Override
        public void onBindingDied(ComponentName name) {
            ServiceConnection.super.onBindingDied(name);
        }

        @Override
        public void onNullBinding(ComponentName name) {
            ServiceConnection.super.onNullBinding(name);
        }
    };

    public boolean bind() {
        if (srv != null || ctxRef.get() == null) return false;
        Intent intent = new Intent(IOpenVPNAPIService.class.getName());
        IOpenVPNAPIService.class.getName();
        intent.setPackage("de.blinkt.openvpn");
        boolean bound = ctxRef.get().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        if (bound) {
            Log.d(TAG, "bound");
        } else {
            Log.e(TAG, "failed to bind");
        }
        return bound;
    }

    public void unbind() {
        if (ctxRef.get() == null) return;
        ctxRef.get().unbindService(mConnection);
    }

    public void doVpnPermissionRequest() {
        try {
            Intent intent = srv.prepareVPNService();
            if (intent != null && isActivity) {
                Log.d(TAG, "requesting vpn perms");
                ((Activity)ctxRef.get()).startActivityForResult(intent, VPN_PERMISSION_REQUEST);
            } else {
                if (shouldDisconnect) {
                    disconnect();
                } else {
                    if (ProfileDB.getEncrypted()) {
                        connectEncryptedProfile();
                    }
                    else if(ProfileDB.getEmbeddedOvpn()) {
                        connectEmbeddedProfile();
                    } else {
                        connectProfile();
                    }
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to connect to OpenVPN", e);
        }
    }

    public void connectProfile() {
        try {
            List<APIVpnProfile> profiles = srv.getProfiles();
            APIVpnProfile foundProfile = null;
            for (APIVpnProfile profile : profiles) {
                if (Objects.equals(profile.mName, profileName)) {
                    foundProfile = profile;
                    break;
                }
            }
            if (foundProfile == null) {
                Log.e(TAG, "Failed to find profile");
                return;
            }
            Log.d(TAG, "starting profile");
            srv.startProfile(foundProfile.mUUID);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to connect to OpenVPN", e);
        }
        doneCallback.run();
    }

    public void connectEncryptedProfile() {
        CryptoManager cm = new CryptoManager();
        byte[] contentsByte;
        try {
            contentsByte = cm.decrypt(new FileInputStream(ctxRef.get().getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + ProfileDB.getFile().replaceAll(EXT_CONF,EXT_OVPN)));
            String contents = new String(contentsByte);
            srv.startVPN(contents);
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to OpenVPN", e);
        }
        doneCallback.run();
    }

    public void connectEmbeddedProfile() {
        String fileName = ProfileDB.getFile().replaceAll(EXT_CONF,EXT_OVPN);
        File profile = new File(ctxRef.get().getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + fileName);
        String contents;
        final BufferedSource in;
        try {
            in = Okio.buffer(Okio.source(profile));
            contents = in.readUtf8();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        try {
            srv.startVPN(contents);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to connect to OpenVPN", e);
        }
        doneCallback.run();
    }

    public void disconnect() {
        try {
            srv.disconnect();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to connect to OpenVPN", e);
        } catch (SecurityException e) {
            Log.e(TAG, "User permission to access IOpenVPNAPIService was rejected on connect", e);
        }
        if (shouldDisconnect) {
            doneCallback.run();
        }
    }

    public void addProfile(String profileName, String profileContent) {

    }

    public APIVpnProfile findProfile(String profileName) {
        return null;
    }

}
