package com.sf.tadami.ui.components.dialog.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import com.sf.tadami.ui.utils.padding

@Composable
fun BottomSheet(
    header: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .nestedScroll(connection = object : NestedScrollConnection {
                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity
                ): Velocity {
                    return available
                }
            })
    ){
        header?.invoke(this)
        Column(
            modifier = Modifier
                .padding(MaterialTheme.padding.extraSmall)
                .verticalScroll(scrollState)
        ) {
            content()
            Text("Merguez")
            Text("Merguez")
            Text("Merguez")
            Text("Merguez")
            Text("Merguez")
            Text("Merguez")
            Text("Merguez")
            Text("Merguez")
            Text("Merguez")
            Text("Merguez")
            Text("Merguez")
            Text("Merguez")
            Text("Merguez")
            Text("Merguez")

        }
    }

}