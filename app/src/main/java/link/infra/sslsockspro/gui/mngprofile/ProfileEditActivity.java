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

package link.infra.sslsockspro.gui.mngprofile;

import static link.infra.sslsockspro.Constants.EXT_CONF;
import static link.infra.sslsockspro.Constants.PROFILES_DIR;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

import link.infra.sslsockspro.R;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class ProfileEditActivity extends AppCompatActivity {
    private EditText vfileContents;
    private String fileName = null;
    private boolean importFlag = false;
    private boolean typedProfile = false;
    public static final String ARG_EXISTING_FILE_NAME = "EXISTING_FILE_NAME";

    private static final String TAG = ProfileEditActivity.class.getSimpleName();
    private boolean showDelete = true;

    private final ActivityResultLauncher<Intent> importFileRequestLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            Intent resultData = result.getData();
            if (resultData != null) {
                Uri fileData = resultData.getData();
                if (fileData != null) {
                    String fileName;
                    String fileContents;
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
                    fileName = getFileName(fileData);
                    if (!fileName.endsWith(EXT_CONF) ) {
                        Toast.makeText(this, R.string.profile_name_ext, Toast.LENGTH_SHORT).show();
                    } else {
                        vfileContents.setText(fileContents);
                        this.fileName = UUID.randomUUID().toString() + EXT_CONF;
                        importFlag = true;
                    }
                }
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_black_24dp);

        vfileContents = findViewById(R.id.file_contents);

        Intent intent = getIntent();
        if (intent != null) {
            fileName = intent.getStringExtra(ARG_EXISTING_FILE_NAME);
        }
        if (fileName == null) {
            getSupportActionBar().setTitle(R.string.title_activity_profile_create);
            findViewById(R.id.import_button).setVisibility(View.VISIBLE);
            showDelete = false;
            invalidateOptionsMenu();
        } else {
            openFile();
        }

        // Add event listeners in code, because onClick doesn't work on 4.4.x for some reason
        // https://stackoverflow.com/a/54060752/816185
        findViewById(R.id.import_button).setOnClickListener(view -> importExternalFile());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_key_edit, menu);
        if (!showDelete) {
            menu.findItem(R.id.action_delete).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_save) {
            saveFile();
            finish();
            return true;
        }
        if (id == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        if (id == R.id.action_delete) {
            deleteFile();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void importExternalFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        importFileRequestLauncher.launch(Intent.createChooser(intent, getString(R.string.title_activity_profile_create)));
    }

    // Get the file name for importing a file from a Uri
    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = Objects.requireNonNull(uri.getPath());
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        // remove the file extension from the result
        return result;
    }

    private void saveFile() {
        if (fileName == null) {
            fileName = UUID.randomUUID().toString() + EXT_CONF;
            typedProfile = true;
        }
        File file = new File(getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + fileName);
        try (BufferedSink out = Okio.buffer(Okio.sink(file))) {
            String pendingContent = vfileContents.getText().toString();
            out.writeUtf8(pendingContent);
            out.close();
            setResult(RESULT_OK);
        } catch (IOException e) {
            Toast.makeText(this, R.string.file_write_fail, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Failed SSLSocks" + EXT_CONF + "file writing: ", e);
        }
        if (importFlag | typedProfile) {
            Toast.makeText(this, R.string.action_profile_added, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.action_profile_edited, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteFile() {
        if (fileName != null) {
            File existingFile = new File(getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + fileName );
            if (!existingFile.exists()) {
                Toast.makeText(this, R.string.file_delete_nexist, Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
            if (!existingFile.delete()) {
                Toast.makeText(this, R.string.file_delete_failed, Toast.LENGTH_SHORT).show();
            }
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, R.string.file_delete_err, Toast.LENGTH_SHORT).show();
        }
    }

    private void openFile() {
        File file = new File(getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + fileName);
        try (BufferedSource in = Okio.buffer(Okio.source(file))) {
            vfileContents.setText(in.readUtf8());
        } catch (IOException e) {
            Log.e(TAG, "Failed to read .conf file", e);
            Toast.makeText(this, R.string.file_read_fail, Toast.LENGTH_SHORT).show();
            vfileContents.getText().clear();
        }
    }
}
