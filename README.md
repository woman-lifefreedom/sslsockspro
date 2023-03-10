###################

WOMAN LIFE FREEDOM

###################

# SSLSocks Pro

SSLSocks Pro[file] is a continue of SSLSocks app. The App SSLSocks Pro supports multiple
profiles and uses a Stunnel library for configuration. 

✨ See [this article](https://hamy.io/post/0011/how-to-run-stunnel-on-your-android-device/) by Hamy for an excellent introduction to the SSL!

**Now released on Google Play!** https://play.google.com/store/apps/details?id=link.infra.sslsockspro

SSL/TLS tunnel using [stunnel](https://www.stunnel.org/) for Android.

## How to use
To add a config profile, tap the import button on top or tap the add button and write the config directly according to the [stunnel documentation](https://www.stunnel.org/static/stunnel.html). The app supports MIME intent filters and you can direcly tap on you config file in your file manager, then select SSLSocks Pro to import the config.

Stunnel should start when you press the start button, and will create a notification while it is being run. If the notification is immediately removed after being created, there was an error, so you will need to check the log (second tab).

A dummy configuration file with the following parameters is created by default:

```
foreground = yes
client = yes
[offline_proxy_tunnel]
connect = 
accept = 127.0.0.1:54321
```

Before loading and after unloading every config profile this dummy configuration is loaded.

### How to configure stunnel
Some example configurations are available in the [stunnel documentation](https://www.stunnel.org/static/stunnel.html#EXAMPLES), and more are given below. Many use cases (e.g. tunnelling SSH or SOCKS over HTTPS) require you to run an stunnel server, which you can download from the stunnel website.

The stunnel binary functions as both a server and a client, as long as you put `client = yes` at the top of your config file when you want to use it as a client. This is set by default in the app.

#### SSH over HTTPS
##### Client

```
[ssh]
accept = 10000
connect = example.com:443
```
Connect to 127.0.0.1:10000 in your SSH client.
##### Server
```
[ssh]
accept = 443
connect = 127.0.0.1:22
```
#### SOCKS
##### Client
```
[rdp]
accept = 9050
connect = example.com:443
```
Connect to 127.0.0.1:9050 in your SOCKS client. Orfox works well for this on android.
##### Server
```
[rdp]
accept = 443
protocol = socks
```
#### RDP
##### Client
```
[rdp]
accept = 3380
connect = example.com:443
```
Connect to 127.0.0.1:3380 in your RDP client.
##### Server
```
[rdp]
accept = 443
connect = 127.0.0.1:3389
```
#### SNI
SNI allows you to have multiple tunnels on one server, as many firewalls only allow port 443.
##### Client
```
[default]
accept = 8080
connect = example.com:443

[ssh]
accept = 10000
connect = example.com:443
sni = ssh.example.com
```
##### Server
```
[default]
accept = 443
connect = 127.0.0.1:8080

[ssh]
sni = default:ssh.example.com
connect = 127.0.0.1:22
```
In addition to standard stunnel configurations, 3 new keys are introduced that are used by
the app: "remark", "ovpn_profile", and "ovpn_run". The "remark" key is used for the name
of the profile. "ovpn_profile" denotes the name of an open VPN profile in case the user
want to get started automatically, and "ovpn_run" key is used to command the app whether
to start open VPN profile automatically or not. A typical configuration looks like:

```
remark = my-first-profile
ovpn_profile = a-profile-in-your-open-vpn-for-android-app
ovpn_run = yes
# stunnel native options:
foreground = yes
client = yes
[http_proxy_tunnel]
accept = 0.0.0.0:local_port
connect = server:port
```
