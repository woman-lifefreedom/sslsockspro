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

package link.infra.sslsockspro.gui.fragments;

import static link.infra.sslsockspro.Constants.EXT_CONF;
import static link.infra.sslsockspro.Constants.PROFILES_DIR;
import static link.infra.sslsockspro.database.ProfileDB.NEW_PROFILE;

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
import link.infra.sslsockspro.database.FileOperation;
import link.infra.sslsockspro.database.ProfileDB;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class ProfileEditActivity extends AppCompatActivity {
    private EditText vfileContents;
    private String fileName = null;
    private boolean importFlag = false;
    private boolean typedProfile = false;
    public static final String ARG_POSITION= "POSITION";
    private static int position;

    private static final String TAG = ProfileEditActivity.class.getSimpleName();
    private boolean showDelete = true;

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
            position = intent.getIntExtra(ARG_POSITION, NEW_PROFILE);
        }
        if (position == NEW_PROFILE) {
            getSupportActionBar().setTitle(R.string.title_activity_profile_create);
            findViewById(R.id.import_button).setVisibility(View.VISIBLE);
            showDelete = false;
            invalidateOptionsMenu();
        } else {
            openFile();
        }
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
                    fileName = FileOperation.getFileName(fileData, getApplicationContext());
                    if (!fileName.endsWith(EXT_CONF) ) {
                        Toast.makeText(this, R.string.profile_name_ext, Toast.LENGTH_SHORT).show();
                    } else {
                        vfileContents.setText(fileContents);
                    }
                }
            }
        }
    });


    private void importExternalFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        importFileRequestLauncher.launch(Intent.createChooser(intent, getString(R.string.title_activity_profile_create)));
    }

    private void saveFile() {
        String fileContents = vfileContents.getText().toString();
        if (ProfileDB.parseProfile(fileContents)) {
            try {
                ProfileDB.saveProfile(fileContents, getApplicationContext(), position);
            } catch (IOException e) {
                Log.e(TAG, "Failed sslsockspro profile writing.", e);
                Toast.makeText(this, R.string.file_write_fail, Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            Log.e(TAG, "file content is wrong");
            //Toast.makeText(this, R.string.file_write_fail, Toast.LENGTH_SHORT).show();
            return;
        }
        if (position == NEW_PROFILE) {
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
