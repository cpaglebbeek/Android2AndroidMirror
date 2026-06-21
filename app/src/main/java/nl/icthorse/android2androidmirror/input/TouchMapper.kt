package nl.icthorse.android2androidmirror.input

/**
 * Map touch-coördinaten van de cartablet-SurfaceView naar BRON-coördinaten (Z Fold 6).
 *
 * De videostream is aan de bron geschaald naar max_size=1024 (beslispunt 3), maar de
 * scrcpy-server verwacht touch-coördinaten in het BRON-coördinatenstelsel (de werkelijke
 * device-resolutie die in de video-header staat). We schalen dus van SurfaceView-pixels
 * naar bron-resolutie, rekening houdend met letterboxing (aspect-ratio-verschil tussen
 * de brede cartablet en de Z Fold 6).
 */
class TouchMapper(
    private var viewWidth: Int = 0,
    private var viewHeight: Int = 0,
    private var srcWidth: Int = 0,
    private var srcHeight: Int = 0,
) {
    fun updateView(w: Int, h: Int) { viewWidth = w; viewHeight = h }
    fun updateSource(w: Int, h: Int) { srcWidth = w; srcHeight = h }

    /** Geeft bron-(x,y) of null als de tik in de letterbox-rand valt. */
    fun map(viewX: Float, viewY: Float): Pair<Int, Int>? {
        if (viewWidth == 0 || viewHeight == 0 || srcWidth == 0 || srcHeight == 0) return null
        val scale = minOf(viewWidth.toFloat() / srcWidth, viewHeight.toFloat() / srcHeight)
        val drawnW = srcWidth * scale
        val drawnH = srcHeight * scale
        val offX = (viewWidth - drawnW) / 2f
        val offY = (viewHeight - drawnH) / 2f
        val rx = (viewX - offX) / scale
        val ry = (viewY - offY) / scale
        if (rx < 0 || ry < 0 || rx > srcWidth || ry > srcHeight) return null
        return rx.toInt() to ry.toInt()
    }
}
