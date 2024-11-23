/*
 * SSLSocks Pro
 * Copyright (C) 2022-2023 WOMAN-LIFE-FREEDOM
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

import static link.infra.sslsockspro.Constants.EXT_CONF;
import static link.infra.sslsockspro.Constants.PROFILES_DIR;
import static link.infra.sslsockspro.database.ProfileDB.NEW_PROFILE;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

import link.infra.sslsockspro.R;
import link.infra.sslsockspro.database.FileOperation;
import link.infra.sslsockspro.database.ProfileDB;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class ExternalProfileImportActivity extends AppCompatActivity {

    private static final String TAG = ExternalProfileImportActivity.class.getSimpleName();
    private String fileContents;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent resultData = getIntent();
        if (resultData != null) {
            Uri fileData = resultData.getData();
            if (fileData != null) {
                InputStream inputStream;
                try {
                    // TODO: this doesn't seem to work on Android 4.4.x, for some reason
                    inputStream = getContentResolver().openInputStream(fileData);
                    if (inputStream == null) { // Just to keep the linter happy that I'm doing null checks
                        throw new FileNotFoundException();
                    }
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Failed to read imported file", e);
                    Toast.makeText(this, R.string.file_read_fail, Toast.LENGTH_SHORT).show();
                    return;
                }
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
                if (ProfileDB.isEncrypted(fileContents)){
                    try {
                        ProfileDB.addEncryptedProfile(fileContents, getApplicationContext());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                }
                if (ProfileDB.parseProfile(fileContents)) {
                    try {
                        ProfileDB.saveProfile(fileContents, getApplicationContext(), NEW_PROFILE);
                        if (ProfileDB.getSize() == 1) {
                            ProfileDB.setPosition(0);
                        }
                        Toast.makeText(this, R.string.action_profile_added, Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Log.e(TAG, "Failed sslsockspro profile writing.", e);
                        Toast.makeText(this, R.string.file_write_fail, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "file content is wrong");
                }
                finish();
            }
        }
    }

}
