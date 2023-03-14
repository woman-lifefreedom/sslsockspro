package link.infra.sslsockspro.database;

public class StunnelKeys {
    /**
     notation:
     KEY: to remind that this constant is a key
     ST: for stunnel service
     _THE_REST: description of the key
     */
    public static final String KEY_ST_OVPN_RUN = "ovpn_run";
    public static final String KEY_ST_OVPN_PROFILE = "ovpn_profile";
    public static final String KEY_ST_REMARK = "remark";

    /* service option keys */
    public static final String KEY_ST_CONNECT = "connect";
    public static final String KEY_ST_ACCEPT = "accept";
    public static final String KEY_ST_CLIENT = "client";

    /* stunnel keys that should be removed from config file */
    public static final String KEY_RM_ST_LOG = "log";           /* internal usage by app */
    public static final String KEY_RM_ST_OUTPUT = "output";     /* internal usage by app */
    public static final String KEY_RM_ST_DEBUG = "debug";     /* internal usage by app */
    public static final String KEY_RM_ST_PID = "pid";           /* no pid is created */
}
