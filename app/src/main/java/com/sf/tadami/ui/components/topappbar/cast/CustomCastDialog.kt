package com.sf.tadami.ui.components.topappbar.cast

import android.util.Log
import androidx.mediarouter.app.MediaRouteChooserDialogFragment
import androidx.mediarouter.app.MediaRouteControllerDialogFragment
import androidx.mediarouter.app.MediaRouteDialogFactory

class CustomMediaRouteDialogFactory : MediaRouteDialogFactory() {
    override fun onCreateControllerDialogFragment(): MediaRouteControllerDialogFragment {
        Log.e("Cast Controller","Here")
        return CustomMediaRouteControllerDialogFragment()
    }

    override fun onCreateChooserDialogFragment(): MediaRouteChooserDialogFragment {
        Log.e("Cast Chooser","Here")
        return CustomMediaRouteChooserDialogFragment()
    }
}

class CustomMediaRouteChooserDialogFragment : MediaRouteChooserDialogFragment() {

}

class CustomMediaRouteControllerDialogFragment : MediaRouteControllerDialogFragment() {

}