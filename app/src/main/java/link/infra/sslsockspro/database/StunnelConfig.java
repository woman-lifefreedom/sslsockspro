package link.infra.sslsockspro.database;

import java.util.ArrayList;
import java.util.List;

class StunnelConfig {
    String remark;
    String ovpnProfile;
    Boolean runOvpn;
    final List<ServiceOptions> serviceOptions = new ArrayList<>();

    class ServiceOptions {
        String serviceName;
        String acceptHost;
        String acceptPort;
        Boolean client;
        String connectHost;
        String connectPort;
    }

    void parseConfig(String config) {

    }

    void parseServiceOptions(String options) {

    }
}
