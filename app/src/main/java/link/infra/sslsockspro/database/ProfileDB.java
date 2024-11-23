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

package link.infra.sslsockspro.database;

import static link.infra.sslsockspro.Constants.EXT_CONF;
import static link.infra.sslsockspro.Constants.EXT_OVPN;
import static link.infra.sslsockspro.Constants.PROFILES_DIR;
import static link.infra.sslsockspro.Constants.PROFILE_DATABASE;
import static link.infra.sslsockspro.database.StunnelKeys.KEY_ST_ACCEPT;
import static link.infra.sslsockspro.database.StunnelKeys.KEY_ST_CLIENT;
import static link.infra.sslsockspro.database.StunnelKeys.KEY_ST_CONNECT;
import static link.infra.sslsockspro.database.StunnelKeys.KEY_ST_ENC;
import static link.infra.sslsockspro.database.StunnelKeys.KEY_ST_OVPN_EMBEDDED;
import static link.infra.sslsockspro.database.StunnelKeys.KEY_ST_OVPN_PROFILE;
import static link.infra.sslsockspro.database.StunnelKeys.KEY_ST_OVPN_RUN;
import static link.infra.sslsockspro.database.StunnelKeys.KEY_ST_REMARK;

import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import link.infra.sslsockspro.R;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

/**
A Singleton class to manage the profiles
 */
public class ProfileDB {

    public static final int NEW_PROFILE = -1;
    private static ProfileDB profileManagement = null;
    private static int position = -1; // -1 means no position by default
//    private static int lastSelectedPosition;

    private static List<ProfileItem> mProfileItems = new ArrayList<>();
    private static ProfileItem newProfileItem;

    static class StunnelGlobalOptions {
        String remark = "";
        String ovpnProfile = "";
        Boolean runOvpn = false;
    }

    static class StunnelServiceOptions {
        String serviceName = "";
        String acceptHost = "";
        String acceptPort = "";
        Boolean client = true;
        String connectHost = "";
        String connectPort = "";
    }

    static class ProfileItem {
        String profileName = "";
        StunnelGlobalOptions stunnelGlobalOptions;
        List<StunnelServiceOptions> stunnelServiceOptions = new ArrayList<>();
        Boolean embeddedOvpn = false;
        Boolean encrypted = false;
        /* serverUUID is for synchronization purposes only */
        String serverUUID = "";
    }

    /**
     * Private Constructor
     * @return
     */
    private ProfileDB() {
    }

    public static ProfileDB getInstance(){
        if(profileManagement==null){
            profileManagement = new ProfileDB();
        }
        return profileManagement;
    }

    public static void saveProfilesToDatabase(Context context) throws IOException {
        BufferedSink out;
        Gson gson = new Gson();
        File db = new File(context.getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + PROFILE_DATABASE);
        out = Okio.buffer(Okio.sink(db));
        out.writeUtf8(gson.toJson(mProfileItems));
        out.close();
    }

    public static void loadProfilesFromDatabase(Context context) throws IOException {
        File db = new File(context.getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + PROFILE_DATABASE);
        final BufferedSource in = Okio.buffer(Okio.source(db));
        String contents = in.readUtf8();
        Gson gson = new Gson();

        Type listType = new TypeToken<List<ProfileItem>>() {}.getType();
        mProfileItems = gson.fromJson(contents,listType);
    }

    /**
     * This method is used to rebuild the database from already stored profiles.
     * it reads a config file from PROFILES_DIR and adds a profile item to the database class.
     * Call @saveProfilesToDatabase() to store the database class to the json database PROFILE_DATABASE
     * @param context - application context
     * @param fileName - file name from application database
     * @throws IOException
     */
    private static void addProfileFromConfigFile(Context context, String fileName) throws IOException {
        File profile = new File(context.getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + fileName);
        final BufferedSource in = Okio.buffer(Okio.source(profile));
        String contents = in.readUtf8();
        parseProfile(contents);
        newProfileItem.profileName = fileName;
        mProfileItems.add(newProfileItem);
    }

    /**
     * This method is for backward compatibility, when on older versions there was no database.
     * With this method the database class and database file are updated from the existing config files
     * @param context - application context
     * @throws IOException
     */
    public static void updateProfilesFromConfigFiles(Context context) throws IOException {
        mProfileItems.clear();
        File folder = new File(context.getFilesDir().getAbsolutePath() + "/" + PROFILES_DIR);
        String fileName;

        // sort the files
        File[] files = folder.listFiles();
        if (files == null) { return; }
        else {
            Arrays.sort(files, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return Long.compare(f1.lastModified(), f2.lastModified());
                }
            });
        }
        for (File fileEntry : files) {
            if (fileEntry.getPath().endsWith(EXT_CONF)) { // Only show config files
                fileName = fileEntry.getName();
                addProfileFromConfigFile(context,fileName);
            }
        }
        saveProfilesToDatabase(context);
    }

    /**
     * the method parseProfile must be called before this method
     * @param fileContents - raw contents of the profile
     * @param context - pass the application context to the method
     * @throws IOException
     */
    public static void saveProfile(String fileContents, Context context, int position) throws IOException {
        /* save the profile */
        BufferedSink out;
        String fileName;
        if (position == NEW_PROFILE) {
            fileName = UUID.randomUUID().toString() + EXT_CONF;
        } else {
            fileName = getFile(position);
        }
        File profile = new File(context.getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + fileName);
        out = Okio.buffer(Okio.sink(profile));
        out.writeUtf8(fileContents);
        out.close();

        /* writing the embedded ovpn*/
        String ovpnFileName = fileName.replaceAll(EXT_CONF,EXT_OVPN);
        File embeddedOvpnProfile = new File(context.getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + ovpnFileName);
        out = Okio.buffer(Okio.sink(embeddedOvpnProfile));
        Matcher matcher;
        matcher = Pattern.compile("<" + KEY_ST_OVPN_EMBEDDED + ">" + "(.+)" + "</" + KEY_ST_OVPN_EMBEDDED + ">",Pattern.DOTALL)
                .matcher(fileContents);
        if (matcher.find()) {
            String embeddedOvpnProfileContent;
            embeddedOvpnProfileContent = matcher.group(1);
            if (embeddedOvpnProfileContent != null)
            {
                out.writeUtf8(embeddedOvpnProfileContent);
            }
        }
        out.close();

        /* add the profile contents to the database */
        newProfileItem.profileName = fileName;
        if (position == NEW_PROFILE) {
            mProfileItems.add(newProfileItem);
        } else {
            mProfileItems.set(position,newProfileItem);
        }
        saveProfilesToDatabase(context);
    }

    public static boolean parseProfile(String fileContents) {
        String sslsocksConfig;
        newProfileItem = new ProfileItem();
        /* remove commented lines */
        fileContents= fileContents.replaceAll("(?m)^[\\s]*#.*","");
        /* separating the embedded ovpn*/
        Matcher matcher;
        matcher = Pattern.compile("<" + KEY_ST_OVPN_EMBEDDED + ">" + "(.+)" + "</" + KEY_ST_OVPN_EMBEDDED + ">",Pattern.DOTALL)
                .matcher(fileContents);
        if (matcher.find()) {
            newProfileItem.embeddedOvpn = true;
        }
        /* remove openvpn part of the config */
        sslsocksConfig = fileContents.replaceAll("<openvpn>[\\s\\S]*</openvpn>","");
        /* first part is stunnel global options, the rest are arrays of service options */
        final String[] configParts = sslsocksConfig.split("(?=\\[)");

        StunnelGlobalOptions go = new StunnelGlobalOptions();
        if ( parseStunnelGlobalOptions(configParts[0],go) ) {
            newProfileItem.stunnelGlobalOptions = go;
        } else return false;

        StunnelServiceOptions so;
        for (int i = 1; i < configParts.length; i++) {
            so = new StunnelServiceOptions();
            if ( parseStunnelServiceOptions(configParts[i],so) ) {
                newProfileItem.stunnelServiceOptions.add(so);
            } else return false;
        }
        return true;
    }

    private static boolean parseStunnelGlobalOptions(String globalOptions, StunnelGlobalOptions go) {
        Matcher matcher;
        matcher = Pattern.compile("[\\s]*" + KEY_ST_REMARK + "[\\s]*=[\\s]*([a-zA-Z0-9_-]+)[\\s]*")
                .matcher(globalOptions);
        if (matcher.find()) {
            go.remark = matcher.group(1);
        }
        matcher = Pattern.compile("[\\s]*" + KEY_ST_OVPN_PROFILE + "[\\s]*=[\\s]*([a-zA-Z0-9_-]+)[\\s]*")
                .matcher(globalOptions);
        if (matcher.find()) {
            go.ovpnProfile= matcher.group(1);
        }
        matcher = Pattern.compile("[\\s]*" + KEY_ST_OVPN_RUN + "[\\s]*=[\\s]*([a-zA-Z0-9_-]+)[\\s]*")
                .matcher(globalOptions);
        if (matcher.find()) {
            if (Objects.equals(matcher.group(1), "yes"))
                go.runOvpn = true;
            else if (Objects.equals(matcher.group(1), "no"))
                go.runOvpn = false;
            else {
                // TODO: bad config
            }
        }
        return true;
    }

    private static boolean parseStunnelServiceOptions(String serviceOptions, StunnelServiceOptions so) {
        Matcher matcher;
        matcher = Pattern.compile("[\\s]*\\[[\\s]*(.*)[\\s]*\\][\\s]*")
                .matcher(serviceOptions);
        if (matcher.find()) {
            so.serviceName = matcher.group(1);
        }
        matcher = Pattern.compile("[\\s]*" + KEY_ST_ACCEPT + "[\\s]*=[\\s]*(.*):(\\d+)[\\s]*")
                .matcher(serviceOptions);
        if (matcher.find()) {
            so.acceptHost = matcher.group(1);
            so.acceptPort = matcher.group(2);
        }
        matcher = Pattern.compile("[\\s]*" + KEY_ST_CONNECT + "[\\s]*=[\\s]*(.*):(\\d+)[\\s]*")
                .matcher(serviceOptions);
        if (matcher.find()) {
            so.connectHost = matcher.group(1);
            so.connectPort = matcher.group(2);
        }

        matcher = Pattern.compile("[\\s]*" + KEY_ST_CLIENT + "[\\s]*=[\\s]*([a-zA-Z0-9_-]+)[\\s]*")
                .matcher(serviceOptions);
        if (matcher.find()) {
            if (Objects.equals(matcher.group(1), "yes"))
                so.client = true;
            else if (Objects.equals(matcher.group(1), "no"))
                so.client = false;
            else {
                // TODO: bad config
            }
        }
        return true;
    }

    public static boolean isEncrypted(String fileContents){
        Matcher matcher = Pattern.compile("^"+KEY_ST_ENC).matcher(fileContents);
        return matcher.find();
    }

    public static void addEncryptedProfile(String fileContents, Context context) throws Exception {
        Matcher matcher = Pattern.compile("^"+KEY_ST_ENC+"v([\\d]{2})&remark=(.*)&serverUUID=(.*)&EOH(.*)",Pattern.DOTALL).matcher(fileContents);
        String version = "";
        String remark = "";
        String serverUUID = "";
        String profileEnc= "";
        if (!matcher.find()) { return; }
        version = matcher.group(1);
        remark = matcher.group(2);
        serverUUID = matcher.group(3);
        profileEnc = matcher.group(4).trim();
        byte[] profileByte = SimpleCrypto.decodeBase64(profileEnc);
        byte[] profileDec = CryptoManager.Companion.decryptServerProfile(profileByte);
        String profile = new String(profileDec, StandardCharsets.UTF_8);

        newProfileItem = new ProfileItem();
        newProfileItem.stunnelGlobalOptions = new StunnelGlobalOptions();
        newProfileItem.encrypted = true;
        newProfileItem.serverUUID = serverUUID;
        newProfileItem.stunnelGlobalOptions.remark=remark;
        /* remove commented lines */
        profile = profile.replaceAll("(?m)^[\\s]*#.*","");
        /* separating the embedded ovpn*/
        String sslsocks= "";
        String ovpn = "";
        matcher = Pattern.compile("(.*)"+"<" + KEY_ST_OVPN_EMBEDDED + ">" + "(.+)" + "</" + KEY_ST_OVPN_EMBEDDED + ">",Pattern.DOTALL)
                .matcher(profile);
        if (!matcher.find()) { return; }
        sslsocks = matcher.group(1).trim();
        ovpn = matcher.group(2).trim();

        // BufferedSink out;
        String sslsocksName = UUID.randomUUID().toString() + EXT_CONF;
        String ovpnName = sslsocksName.replaceAll(EXT_CONF,EXT_OVPN);
        File sslsocksProfile = new File(context.getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + sslsocksName);
        sslsocksProfile.createNewFile();
        File ovpnProfile = new File(context.getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + ovpnName);
        ovpnProfile.createNewFile();
        CryptoManager cm1 = new CryptoManager();
        FileOutputStream fos2 = new FileOutputStream(ovpnProfile);
        cm1.encrypt(ovpn.getBytes(),fos2);
        FileOutputStream fos1 = new FileOutputStream(sslsocksProfile);
        cm1.encrypt(sslsocks.getBytes(),fos1);

//        CryptoManager cm3 = new CryptoManager();
//        byte[] ovpn1;
//        ovpn1 = cm3.decrypt(new FileInputStream(context.getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + ovpnName));
//        String ovpnstr = new String(ovpn1);
//
//        CryptoManager cm2 = new CryptoManager();
//        byte[] ssl1;
//        ssl1 = cm2.decrypt(new FileInputStream(context.getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + sslsocksName));
//        String ssl1str = new String(ssl1);

        /* add the profile contents to the database */
        newProfileItem.embeddedOvpn = true;
        newProfileItem.stunnelGlobalOptions.runOvpn = true;
        newProfileItem.profileName = sslsocksName;
        StunnelServiceOptions so = new StunnelServiceOptions();
        so.connectHost = "Synchronized";
        newProfileItem.stunnelServiceOptions.add(so);
        mProfileItems.add(newProfileItem);
        saveProfilesToDatabase(context);
    }

    /**
     * @return last selected file on the database
     */
    public static String getFile() {
        return mProfileItems.get(position).profileName;
    }

    public static String getFile(int position) {
        return mProfileItems.get(position).profileName;
    }

    public static int getSize() {
        return mProfileItems.size();
    }

    public static String getRemark(int position){
        return mProfileItems.get(position).stunnelGlobalOptions.remark;
    }

    public static String getHost(int position, int serviceIdx){
        return mProfileItems.get(position).stunnelServiceOptions.get(serviceIdx).connectHost;
    }

    public static String getHostPort(int position, int serviceIdx){
        return mProfileItems.get(position).stunnelServiceOptions.get(serviceIdx).connectPort;
    }

    public static String getOvpn() {
        return mProfileItems.get(position).stunnelGlobalOptions.ovpnProfile;
    }

    public static Boolean getEmbeddedOvpn() {
        return mProfileItems.get(position).embeddedOvpn;
    }

    public static Boolean getEncrypted(int position) {
        return mProfileItems.get(position).encrypted;
    }

    public static Boolean getEncrypted() {
        return mProfileItems.get(position).encrypted;
    }

    public static Boolean getRunOvpn() {
        return mProfileItems.get(position).stunnelGlobalOptions.runOvpn;
    }

    public static void setPosition(int position) {
        ProfileDB.position = position;
    }

    public static int getPosition() {
        return position;
    }

//    public static void addDB(String file, String remark, String server, String ovpn, Boolean runOvpn) {
//        ProfileDB.files.add(file);
//        ProfileDB.remarks.add(remark);
//        ProfileDB.servers.add(server);
//        ProfileDB.ovpns.add(ovpn);
//        ProfileDB.runOvpns.add(runOvpn);
//    }

    public static void clear() {
        mProfileItems.clear();
    }

    public static void remove(int position) {
        mProfileItems.remove(position);
    }

    public static boolean removeProfile(Context context, int position) throws IOException {
        String fileName = getFile(position);
        if (fileName == null) {
            Toast.makeText(context, R.string.file_delete_err, Toast.LENGTH_SHORT).show();
            return false;
        } else
        {
            File existingFile = new File(context.getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + fileName);
            if (!existingFile.exists()) {
                Toast.makeText(context, R.string.file_delete_nexist, Toast.LENGTH_SHORT).show();
                return false;
            }
            if (!existingFile.delete()) {
                Toast.makeText(context, R.string.file_delete_failed, Toast.LENGTH_SHORT).show();
                return false;
            }
            removeOvpnProfile(context,position);
            mProfileItems.remove(position);
            saveProfilesToDatabase(context);
            return true;
        }
    }

    public static boolean removeOvpnProfile(Context context, int position) throws IOException {
        String fileName = getFile(position);
        String ovpnFileName = fileName.replaceAll(EXT_CONF,EXT_OVPN);
        File existingFile = new File(context.getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + ovpnFileName);
        if (!existingFile.exists()) {
            Toast.makeText(context, R.string.file_delete_nexist, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!existingFile.delete()) {
            Toast.makeText(context, R.string.file_delete_failed, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

//    public static int getLastSelectedPosition() {
//        return lastSelectedPosition;
//    }
//
//    public static void setLastSelectedPosition(int lastSelectedPosition) {
//        ProfileDB.lastSelectedPosition = lastSelectedPosition;
//    }
//
//    public static void saveProfile(Context context){
//        final String xmlFilePath = context.getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + "xml-db.xml";
//        XmlSerializer xmlSerializer = Xml.newSerializer();
//        StringWriter writer = new StringWriter();
//        try {
//            FileOutputStream fileOS = new FileOutputStream(xmlFilePath);
//            xmlSerializer.setOutput(writer);
//            xmlSerializer.startDocument("UTF-8", true);
//            xmlSerializer.startTag(null, "sslsockspro");
//            xmlSerializer.startTag(null, "stunnel");
//            xmlSerializer.endTag(null,"stunnel");
//            xmlSerializer.endTag(null, "sslsockspro");
//            xmlSerializer.endDocument();
//            xmlSerializer.flush();
//            String dataWrite = writer.toString();
//            fileOS.write(dataWrite.getBytes());
//            fileOS.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}
