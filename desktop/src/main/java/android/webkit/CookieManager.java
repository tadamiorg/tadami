package android.webkit;

import com.sf.tadami.network.DesktopCookieJar;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

import java.util.List;
import java.util.stream.Collectors;

/** The WebView cookie store, the same as the OkHttp clients' on the desktop. */
public class CookieManager {
    private static final CookieManager INSTANCE = new CookieManager();

    protected CookieManager() {
    }

    public static CookieManager getInstance() {
        return INSTANCE;
    }

    /** The cookies [url] would send, as a Cookie header value, null when there are none. */
    public String getCookie(String url) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) return null;
        List<Cookie> cookies = DesktopCookieJar.INSTANCE.get(httpUrl);
        if (cookies.isEmpty()) return null;
        return cookies.stream().map(c -> c.name() + "=" + c.value()).collect(Collectors.joining("; "));
    }

    public void setCookie(String url, String value) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) return;
        Cookie cookie = Cookie.parse(httpUrl, value);
        if (cookie != null) DesktopCookieJar.INSTANCE.saveFromResponse(httpUrl, List.of(cookie));
    }

    public void flush() {
    }

    public void setAcceptCookie(boolean accept) {
    }

    public boolean acceptCookie() {
        return true;
    }
}
