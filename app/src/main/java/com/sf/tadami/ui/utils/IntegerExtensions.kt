package com.sf.tadami.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R

@Composable
fun Int.defaultParser() : String{
    if(this==0){
        return stringResource(id = R.string.default_string)
    }
    return this.toString()
}