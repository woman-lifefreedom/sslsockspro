/*
 * Modified by WOMAN-LIFE-FREEDOM
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

package link.infra.sslsockspro.gui;

import static java.lang.Boolean.TRUE;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import link.infra.sslsockspro.database.ProfileDB;
import link.infra.sslsockspro.service.StunnelService;

/**
 * Other applications can launch this activity to stop the service
 */
public class ServiceStopActivity extends Activity {
	OpenVPNIntegrationHandler openVPNIntegrationHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		StunnelService.start(this);
		/*
		String openVpnProfile = PreferenceManager.getDefaultSharedPreferences(this).getString("open_vpn_profile", "");
		if (openVpnProfile.trim().length() > 0) {
			openVPNIntegrationHandler = new OpenVPNIntegrationHandler(this, ServiceStopActivity.this::finish, ProfileDB.getOvpn(), true);
			openVPNIntegrationHandler.bind();
		}

		 */
		if (ProfileDB.getRunOvpn() == TRUE) {
			openVPNIntegrationHandler = new OpenVPNIntegrationHandler(this, ServiceStopActivity.this::finish, ProfileDB.getOvpn(), true);
			openVPNIntegrationHandler.bind();
		}
		Intent intentStop = new Intent(this, StunnelService.class);
		stopService(intentStop);
		if (openVPNIntegrationHandler == null) {
			finish();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == OpenVPNIntegrationHandler.PERMISSION_REQUEST) {
			if (resultCode == RESULT_OK && openVPNIntegrationHandler != null) {
				openVPNIntegrationHandler.doVpnPermissionRequest();
			}
		} else if (requestCode == OpenVPNIntegrationHandler.VPN_PERMISSION_REQUEST) {
			if (resultCode == RESULT_OK && openVPNIntegrationHandler != null) {
				openVPNIntegrationHandler.disconnect();
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (openVPNIntegrationHandler != null) {
			openVPNIntegrationHandler.unbind();
		}
	}
}
