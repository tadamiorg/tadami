package com.sf.tadami.ui.webview.adblock

/**
 * The filter lists the WebView ad blocker can use, downloaded on the device: none is bundled in the
 * app. EasyList and EasyPrivacy are GPLv3 / CC BY-SA 3.0 (the EasyList authors, https://easylist.to/),
 * the AdGuard filters GPLv3 (AdGuard), Peter Lowe's list is free to use (https://pgl.yoyo.org/adservers/).
 */
enum class FilterList(val id: String, val title: String, val url: String) {
    EASYLIST("easylist", "EasyList", "https://easylist.to/easylist/easylist.txt"),
    EASYPRIVACY("easyprivacy", "EasyPrivacy", "https://easylist.to/easylist/easyprivacy.txt"),
    ADGUARD_POPUPS("adguard_popups", "AdGuard Popups", "https://filters.adtidy.org/extension/ublock/filters/19.txt"),
    PETER_LOWE(
        "peter_lowe",
        "Peter Lowe's Ad and tracking server list",
        "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=adblockplus&showintro=0&mimetype=plaintext",
    ),
}
