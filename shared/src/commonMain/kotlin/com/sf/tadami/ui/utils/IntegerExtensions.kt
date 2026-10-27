package com.sf.tadami.ui.utils

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*

@Composable
fun Int.defaultParser() : String{
    if(this==0){
        return stringResource(Res.string.default_string)
    }
    return this.toString()
}