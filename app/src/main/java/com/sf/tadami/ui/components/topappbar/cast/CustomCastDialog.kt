package com.sf.tadami.ui.components.topappbar.cast

import androidx.mediarouter.app.MediaRouteChooserDialogFragment
import androidx.mediarouter.app.MediaRouteControllerDialogFragment
import androidx.mediarouter.app.MediaRouteDialogFactory

class CustomMediaRouteDialogFactory : MediaRouteDialogFactory() {
    override fun onCreateControllerDialogFragment(): MediaRouteControllerDialogFragment {
        return CustomMediaRouteControllerDialogFragment()
    }

    override fun onCreateChooserDialogFragment(): MediaRouteChooserDialogFragment {
        return CustomMediaRouteChooserDialogFragment()
    }
}

class CustomMediaRouteChooserDialogFragment : MediaRouteChooserDialogFragment() {

}

class CustomMediaRouteControllerDialogFragment : MediaRouteControllerDialogFragment() {

}