package com.sf.tadami.network

import okhttp3.Interceptor

/** The browser the desktop app embeds: it solves the Cloudflare challenges and runs the pages of [WebViewResolver]. */
interface DesktopBrowser {

    fun cloudflareInterceptor(userAgent: () -> String): Interceptor

    fun webViewResolver(userAgent: () -> String): WebViewResolver
}
