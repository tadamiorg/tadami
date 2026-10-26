package com.sf.tadami.network

import java.io.IOException

/**
 * A Cloudflare challenge could not be solved in the background (interactive challenge, outdated
 * WebView, timeout...). Opening [url] in the WebView and solving it there usually works.
 */
class CloudflareChallengeException(val url: String, message: String) : IOException(message)
