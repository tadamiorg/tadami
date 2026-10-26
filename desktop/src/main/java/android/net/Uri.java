package android.net;

import java.io.File;

/** The few Uri methods the extensions call: a file:// URI of a local file. */
public final class Uri {
    private final String uri;

    private Uri(String uri) {
        this.uri = uri;
    }

    public static Uri parse(String uriString) {
        return new Uri(uriString);
    }

    public static Uri fromFile(File file) {
        return new Uri(file.toURI().toString());
    }

    @Override
    public String toString() {
        return uri;
    }
}
