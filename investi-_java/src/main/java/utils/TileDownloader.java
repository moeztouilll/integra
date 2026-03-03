package utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

/**
 * Downloads OSM tiles for a bounding box into a local directory.
 * Used by AddSaleController to pre-seed the tile cache on first launch.
 */
public class TileDownloader {

    /** OSM tile servers */
    private static final String[] SERVERS = { "a", "b", "c" };

    private final File tilesDir;

    public TileDownloader(File tilesDir) {
        this.tilesDir = tilesDir;
    }

    /**
     * Download tiles for the given bounding box and zoom range.
     * Already-downloaded tiles are skipped.
     *
     * @param minLat     southern latitude
     * @param maxLat     northern latitude
     * @param minLon     western longitude
     * @param maxLon     eastern longitude
     * @param minZoom    start zoom level (inclusive)
     * @param maxZoom    end zoom level (inclusive)
     * @param onProgress called with (downloaded, total) after each tile
     */
    public void download(double minLat, double maxLat,
            double minLon, double maxLon,
            int minZoom, int maxZoom,
            ProgressCallback onProgress) {

        int serverIdx = 0;
        int total = countTiles(minLat, maxLat, minLon, maxLon, minZoom, maxZoom);
        int done = 0;

        for (int z = minZoom; z <= maxZoom; z++) {
            int x0 = lonToTileX(minLon, z), x1 = lonToTileX(maxLon, z);
            int y0 = latToTileY(maxLat, z), y1 = latToTileY(minLat, z);

            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    File tileFile = new File(tilesDir, z + File.separator + x + File.separator + y + ".png");
                    if (!tileFile.exists()) {
                        tileFile.getParentFile().mkdirs();
                        String sub = SERVERS[serverIdx % SERVERS.length];
                        String urlStr = "https://" + sub + ".tile.openstreetmap.org/" + z + "/" + x + "/" + y + ".png";
                        downloadFile(urlStr, tileFile);
                        serverIdx++;
                        try {
                            Thread.sleep(80);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    done++;
                    if (onProgress != null)
                        onProgress.update(done, total);
                }
            }
        }
    }

    /* ── helpers ──────────────────────────────────────────────────────── */

    public static int lonToTileX(double lon, int z) {
        return (int) Math.floor((lon + 180.0) / 360.0 * (1 << z));
    }

    public static int latToTileY(double lat, int z) {
        double rad = Math.toRadians(lat);
        return (int) Math.floor((1.0 - Math.log(Math.tan(rad) + 1.0 / Math.cos(rad)) / Math.PI) / 2.0 * (1 << z));
    }

    private int countTiles(double minLat, double maxLat, double minLon, double maxLon, int minZ, int maxZ) {
        int n = 0;
        for (int z = minZ; z <= maxZ; z++) {
            n += (lonToTileX(maxLon, z) - lonToTileX(minLon, z) + 1)
                    * (latToTileY(minLat, z) - latToTileY(maxLat, z) + 1);
        }
        return n;
    }

    private static void downloadFile(String urlStr, File dest) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", "JavaFX-MapApp/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, dest.toPath());
            }
        } catch (Exception e) {
            // silently ignore — TileServer returns transparent tile on miss
        }
    }

    public interface ProgressCallback {
        void update(int done, int total);
    }
}
