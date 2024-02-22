package com.sf.tadami.domain.extensions

data class Extensions(
    val updates: List<Extension.Installed>,
    val installed: List<Extension.Installed>,
    val available: List<Extension.Available>
)