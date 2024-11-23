/*
 * Author: WOMAN-LIFE-FREEDOM
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
    public static final String KEY_ST_OVPN_EMBEDDED = "openvpn";

    /* service option keys */
    public static final String KEY_ST_CONNECT = "connect";
    public static final String KEY_ST_ACCEPT = "accept";
    public static final String KEY_ST_CLIENT = "client";

    /* stunnel keys that should be removed from config file */
    public static final String KEY_RM_ST_LOG = "log";           /* internal usage by app */
    public static final String KEY_RM_ST_OUTPUT = "output";     /* internal usage by app */
    public static final String KEY_RM_ST_DEBUG = "debug";     /* internal usage by app */
    public static final String KEY_RM_ST_PID = "pid";           /* no pid is created */

    /* encryption related keys
    format: KEY_ST_ENC + "v00" + "&" + "remark=someRemark" + "&" + serverUUID=someUUID + "&" + "EOH&" + encryptedData
    */
    public static final String KEY_ST_ENC = "sslsocksenc://";
}
