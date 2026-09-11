package com.pulsenet.app.ui.map

import android.preference.PreferenceManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private const val DEFAULT_ZOOM = 15.0
private val DEFAULT_CENTER = GeoPoint(26.9124, 75.7873) // MUJ campus, Jaipur

/**
 * Plots cached messages as priority-colored pins. Peer positions aren't available
 * to plot — GossipEngine never exchanges peer GPS coordinates, only message
 * content, which already carries its own origin coordinates — so only message
 * pins and the device's own location are shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshMapScreen(
    onBack: () -> Unit,
    viewModel: MeshMapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.markers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesh Map") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } }
            )
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(padding),
            factory = {
                Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
                Configuration.getInstance().userAgentValue = context.packageName

                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(DEFAULT_ZOOM)
                    controller.setCenter(DEFAULT_CENTER)

                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this)
                    locationOverlay.enableMyLocation()
                    overlays.add(locationOverlay)
                }
            },
            update = { mapView ->
                // Keep the location overlay (index 0); replace only message pins.
                while (mapView.overlays.size > 1) mapView.overlays.removeAt(mapView.overlays.size - 1)

                messages.forEach { message ->
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(message.latitude, message.longitude)
                        title = "${message.senderAlias}: ${message.content}"
                        icon = priorityMarkerIcon(message.priority)
                    }
                    mapView.overlays.add(marker)
                }
                mapView.invalidate()
            }
        )
    }
}
