package com.buspljus

import org.oscim.core.GeoPoint
import org.oscim.layers.marker.MarkerItem

class MojMarker(var brojLinije: String, var garazniBOriginal: String, var garazniBMenjan: String?, var vremePolaska: String, var polozajVozila: GeoPoint):
    MarkerItem(brojLinije, garazniBOriginal, polozajVozila) {
}