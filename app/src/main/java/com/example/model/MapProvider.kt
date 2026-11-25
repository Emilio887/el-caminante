package com.example.model

enum class MapProvider(
    val id: String,
    val displayName: String,
    val description: String,
    val urlPattern: String,
    val maxZoom: Int = 17,
    val isTopographic: Boolean = false,
    val isSatellite: Boolean = false
) {
    OPEN_TOPO(
        id = "opentopo",
        displayName = "OpenTopoMap",
        description = "Curvas de nivel y sombreado topográfico ideal para MTB",
        urlPattern = "https://tile.opentopomap.org/{z}/{x}/{y}.png",
        maxZoom = 17,
        isTopographic = true
    ),
    CYCL_OSM(
        id = "cyclosm",
        displayName = "CyclOSM",
        description = "Especializado en ciclismo, senderos MTB y carriles",
        urlPattern = "https://a.tile-cyclosm.openstreetmap.fr/cyclosm/{z}/{x}/{y}.png",
        maxZoom = 17,
        isTopographic = true
    ),
    OPEN_STREET_MAP(
        id = "osm",
        displayName = "OpenStreetMap",
        description = "Mapa estándar con caminos, pistas forestales y carreteras",
        urlPattern = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        maxZoom = 18
    ),
    ESRI_SATELLITE(
        id = "esri_sat",
        displayName = "Esri Satellite",
        description = "Fotografía aérea y satelital de alta resolución",
        urlPattern = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
        maxZoom = 18,
        isSatellite = true
    ),
    IGN_SPAIN(
        id = "ign_spain",
        displayName = "IGN España Topográfico",
        description = "Cartografía oficial del Instituto Geográfico Nacional",
        urlPattern = "https://www.ign.es/wmts/mapa-raster?layer=MTN&style=default&tilematrixset=GoogleMapsCompatible&Service=WMTS&Request=GetTile&Version=1.0.0&Format=image/jpeg&TileMatrix={z}&TileCol={x}&TileRow={y}",
        maxZoom = 17,
        isTopographic = true
    ),
    GOOGLE_MAPS(
        id = "google_maps",
        displayName = "Google Maps Compose",
        description = "Mapa vectorial oficial de Google Maps con polilineas y ubicación",
        urlPattern = "",
        maxZoom = 20
    );

    fun buildTileUrl(zoom: Int, x: Int, y: Int): String {
        return urlPattern
            .replace("{z}", zoom.toString())
            .replace("{x}", x.toString())
            .replace("{y}", y.toString())
    }

    companion object {
        fun fromId(id: String): MapProvider {
            return entries.firstOrNull { it.id == id } ?: OPEN_TOPO
        }
    }
}
