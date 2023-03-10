package link.infra.sslsockspro.database;

import static link.infra.sslsockspro.Constants.EXT_CONF;
import static link.infra.sslsockspro.Constants.EXT_XML;
import static link.infra.sslsockspro.Constants.PROFILES_DIR;
import static link.infra.sslsockspro.database.StunnelKeys.KEY_ST_ACCEPT;
import static link.infra.sslsockspro.database.StunnelKeys.KEY_ST_CONNECT;
import static link.infra.sslsockspro.database.StunnelKeys.KEY_ST_OVPN_PROFILE;
import static link.infra.sslsockspro.database.StunnelKeys.KEY_ST_OVPN_RUN;
import static link.infra.sslsockspro.database.StunnelKeys.KEY_ST_REMARK;

import android.content.Context;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import com.google.gson.Gson;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import link.infra.sslsockspro.R;
import link.infra.sslsockspro.gui.activities.MainActivity;
import okio.BufferedSink;
import okio.Okio;

/*
A Singleton class to manage the profiles
 */
public class ProfileDB {

    private static ProfileDB profileManagement = null;
    private static final List<String> files = new ArrayList<>();
    private static final List<String> remarks = new ArrayList<>();
    private static final List<String> servers = new ArrayList<>();
    private static final List<String> ovpns = new ArrayList<>();
    private static final List<Boolean> runOvpns = new ArrayList<>();
    private static int position = -1; // -1 means no position by default
    private static int lastSelectedPosition;

    private static final List<ProfileItem> mProfileItems = new ArrayList<>();
    private static ProfileItem newProfileItem;

    static class StunnelGlobalOptions {
        String remark = "";
        String ovpnProfile = "";
        Boolean runOvpn = false;

        public StunnelGlobalOptions() {
        }

        public StunnelGlobalOptions(String remark, String ovpnProfile, Boolean runOvpn) {
            this.remark = remark;
            this.ovpnProfile = ovpnProfile;
            this.runOvpn = runOvpn;
        }
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
        String configFileName;
        StunnelGlobalOptions stunnelGlobalOptions;
        List<StunnelServiceOptions> stunnelServiceOptions = new ArrayList<>();

        public ProfileItem(StunnelGlobalOptions stunnelGlobalOptions) {
            this.stunnelGlobalOptions = stunnelGlobalOptions;
        }

        public ProfileItem() {
        }

        public ProfileItem(String configFileName, StunnelGlobalOptions stunnelGlobalOptions) {
            this.configFileName = configFileName;
            this.stunnelGlobalOptions = stunnelGlobalOptions;
        }
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

    public static void saveProfile(String fileContents, Context context) throws IOException {
        mProfileItems.add(newProfileItem);
        Gson gson = new Gson();
        String test = gson.toJson(mProfileItems);

        String fileName = UUID.randomUUID().toString();
        File storedData = new File(context.getFilesDir().getPath() + "/" + PROFILES_DIR + "/" + fileName + EXT_CONF);
        BufferedSink out = Okio.buffer(Okio.sink(storedData));
        out.writeUtf8(fileContents);
        out.close();

        //if ( fileName.endsWith(EXT_XML) ) {
        //} else if ( fileName.endsWith(EXT_CONF) ) {
        //    out.writeUtf8("<sslsockspro>\n");
        //    out.writeUtf8("<stunnel>\n");
        //    out.writeUtf8(fileContents);
        //    out.writeUtf8("\n");
        //    out.writeUtf8("</stunnel>\n");
        //    out.writeUtf8("</sslsockspro>\n");
        //}
    }

    public static boolean parseProfile(String fileContents) {
        // TODO : parse the profile and validate the content
        String sslsocksConfig;
        //remove openvpn part of the config
        sslsocksConfig = fileContents.replaceAll("<openvpn>[\\s\\S]*</openvpn>","");
        //remove commented lines
        sslsocksConfig = sslsocksConfig.replaceAll("(?m)^[\\s]*#","");
        // first part is stunnel global options, the rest are arrays of service options
        final String[] configParts = sslsocksConfig.split("(?=\\[)");
        //final String goString = configParts[0];
        //final String soString[] = configParts;

        newProfileItem = new ProfileItem();

        StunnelGlobalOptions go = new StunnelGlobalOptions();
        parseStunnelGlobalOptions(configParts[0],go);
        newProfileItem.stunnelGlobalOptions = go;

        StunnelServiceOptions so;
        for (int i = 1; i < configParts.length; i++) {
            so = new StunnelServiceOptions();
            parseStunnelServiceOptions(configParts[i],so);
            newProfileItem.stunnelServiceOptions.add(so);
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
        //matcher = Pattern.compile("[\\s]*" + KEY_ST_REMARK + "[\\s]*=[\\s]*([a-zA-Z0-9_-]+)[\\s]*")
        //        .matcher(serviceOptions);
        //if (matcher.find()) {
        //    so.acceptHost = matcher.group(1);
        //}
        return true;
    }

    public static List<String> getFiles() {
        if(profileManagement==null){
            profileManagement = new ProfileDB();
        }
        return files;
    }

    public static int getSize() {
        return files.size();
    }

    public static List<String> getRemarks(){
        if(profileManagement==null){
            profileManagement = new ProfileDB();
        }
        return remarks;
    }

    public static List<String> getServers(){
        if(profileManagement==null){
            profileManagement = new ProfileDB();
        }
        return servers;
    }

    public static void setPosition(int position) {
        ProfileDB.position = position;
    }

    public static String getOvpn() {
        return ovpns.get(position);
    }

    public static Boolean getRunOvpn() {
        return runOvpns.get(position);
    }

    /**
     * @return last selected file on the database
     */
    public static String getFile() {
        return files.get(position);
    }

    public static int getPosition() {
        return position;
    }

    public static void addDB(String file, String remark, String server, String ovpn, Boolean runOvpn) {
        ProfileDB.files.add(file);
        ProfileDB.remarks.add(remark);
        ProfileDB.servers.add(server);
        ProfileDB.ovpns.add(ovpn);
        ProfileDB.runOvpns.add(runOvpn);
    }

    public static void clear() {
        files.removeAll(files);
        remarks.removeAll(remarks);
        servers.removeAll(servers);
        ovpns.removeAll(ovpns);
        runOvpns.removeAll(runOvpns);
    }

    public static void remove(int position) {
        files.remove(position);
        remarks.remove(position);
        servers.remove(position);
        ovpns.remove(position);
        runOvpns.remove(position);
    }

    public static int getLastSelectedPosition() {
        return lastSelectedPosition;
    }

    public static void setLastSelectedPosition(int lastSelectedPosition) {
        ProfileDB.lastSelectedPosition = lastSelectedPosition;
    }

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
