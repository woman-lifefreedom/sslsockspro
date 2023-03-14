package link.infra.sslsockspro.database;

import java.util.ArrayList;
import java.util.List;

class StunnelConfig {

    class StunnelGlobalOptions {
        String remark;
        String ovpnProfile;
        Boolean runOvpn;

        public StunnelGlobalOptions(String remark, String ovpnProfile, Boolean runOvpn) {
            this.remark = remark;
            this.ovpnProfile = ovpnProfile;
            this.runOvpn = runOvpn;
        }
    }

    class StunnelServiceOptions {
        String serviceName;
        String acceptHost;
        String acceptPort;
        Boolean client;
        String connectHost;
        String connectPort;
    }

    void parseConfig(String config) { }
    void parseServiceOptions(String options) { }
}
