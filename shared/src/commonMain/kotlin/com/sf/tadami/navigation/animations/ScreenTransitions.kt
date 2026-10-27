package com.sf.tadami.navigation.animations

/**
 * Whether the screens slide and fade in and out. Not on the desktop: a click back before the slide
 * ended left the screen stuck at its first frame, off by the slide (NavHost 2.10.0 takes it for a
 * cancelled predictive back, and the next navigation cancels the snap that would end it).
 */
expect val animatesScreenTransitions: Boolean
