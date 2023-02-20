package link.infra.sslsockspro;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/*
A Singleton class to manage the profiles
 */
public class ProfileDB {

    private static ProfileDB profileManagement = null;
    private static final List<String> files = new ArrayList<>();
    private static final List<String> remarks = new ArrayList<>();
    private static final List<String> servers = new ArrayList<>();
    private static final List<String> ovpns = new ArrayList<>();
    private static final List<Boolean> runOvpns = new ArrayList<Boolean>();
    private static int position = -1; // -1 means no position by default
    private static int lastSelectedPosition;

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

    public static List<String> getFiles(){
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

}
