package link.infra.sslsockspro.gui.fragments;

import android.text.Spanned;

public class AboutItem {
    public final String title;
    public final Spanned contents;

    public AboutItem(String title, Spanned contents) {
        this.title = title;
        this.contents = contents;
    }
}
