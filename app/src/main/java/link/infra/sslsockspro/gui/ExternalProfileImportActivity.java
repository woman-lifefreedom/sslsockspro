package link.infra.sslsockspro.gui;

import static link.infra.sslsockspro.Constants.EXT_CONF;
import static link.infra.sslsockspro.Constants.PROFILES_DIR;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
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
import link.infra.sslsockspro.gui.main.MainActivity;
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
                String fileName = getFileName(fileData);
                if (!fileName.endsWith(EXT_CONF) ) {
                    Toast.makeText(this, R.string.profile_name_ext, Toast.LENGTH_SHORT).show();
                    return;
                }
                saveFile();
//                Intent intent = new Intent(this, AdvancedSettingsActivity.class);
//                startActivity(intent);
                finish();
            }
        }
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
        return result;
    }

    private void saveFile() {
        String fileName = UUID.randomUUID().toString();
        File fileConf = new File(getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + fileName + EXT_CONF);
        try (BufferedSink out = Okio.buffer(Okio.sink(fileConf))) {
            out.writeUtf8(fileContents);
            out.close();
            setResult(RESULT_OK);
        } catch (IOException e) {
            Toast.makeText(this, R.string.file_write_fail, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Failed stunnel .conf file writing: ", e);
        }
        Toast.makeText(this, R.string.action_profile_added, Toast.LENGTH_SHORT).show();
    }



}
