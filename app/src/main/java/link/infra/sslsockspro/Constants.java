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

package link.infra.sslsockspro;

public class Constants {
    public static final String PSKSECRETS = "psksecrets.txt";
    public static final String PID = "pid";
    public static final String EXT_CONF = ".conf";
    public static final String EXT_OVPN = ".ovpn";
    public static final String EXT_XML = ".xml";
    public static final String SERVICE_DIR = "stunnel_service";
    public static final String PROFILES_DIR = "stunnel_profiles";
    public static final String CONFIG = "config.conf";
    public static final String PROFILE_DATABASE = "profiles_db.json";
    public static final String VERSION_CONF = "ver.conf";
    public static final String DUMMY_CONF = "dummy.conf";
    public static final String APP_LOG = "log";
    public static final String VERSION_LOG = "ver_log";

    // sslsocks related configurations
    public static final String OVPN_RUN = "ovpn_run";
    public static final String OVPN_PROFILE = "ovpn_profile";
    public static final String SSLSOCKS_REMARK = "remark";
    public static final String STUNNEL_OUTPUT = "output";
    public static final String TUNNEL = "tunnel";
    public static final String STUNNEL_LOG = "log";


    // log related constants
    public static final int LOG_LEVEL_DEFAULT = 5;
    public static final int LOG_LEVEL_OFFSET = 3;
    public static final String LOG_NONE = "None";
    public static final String LOG_SHORT = "Short";
    public static final String LOG_ISO = "ISO";

    //shared prefs key
    public static final String LAST_SELECTED_PROFILE = "last_selected_profile";


}
