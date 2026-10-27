package com.sf.tadami.network.utils

/**
 * JavaScript evaluating to true while Cloudflare is still at work on the page: its challenge (or
 * error) page, or the site's page still running the precursor script that follows a challenge.
 * Both define `_cf_chl_opt`. The clearance the precursor step writes is rejected by HTTP clients
 * until that script is done, so it doesn't count as solved yet.
 */
public const val CLOUDFLARE_CHALLENGE_PROBE =
    "(function () { return typeof window._cf_chl_opt !== 'undefined' || " +
        "document.getElementById('challenge-error-title') !== null || " +
        "document.getElementById('challenge-error-text') !== null; })()"

/**
 * JavaScript listening for the message Cloudflare's Turnstile widget posts to the page when it waits
 * for a click (as Mihon does): no headless solve can pass it, so the interceptors give up at once
 * instead of waiting for their timeout. Run it in each document, once is enough.
 */
public const val CLOUDFLARE_INTERACTIVE_LISTENER =
    "(function () { if (window.__tadamiCfListening) return; window.__tadamiCfListening = true; " +
        "addEventListener('message', function (event) { var data = event.data; " +
        "if (data && data.source === 'cloudflare-challenge' && data.event === 'interactiveBegin') " +
        "window.__tadamiCfInteractive = true; }); })()"

/** JavaScript evaluating to true once [CLOUDFLARE_INTERACTIVE_LISTENER] heard that the challenge wants a click. */
public const val CLOUDFLARE_INTERACTIVE_PROBE = "window.__tadamiCfInteractive === true"
