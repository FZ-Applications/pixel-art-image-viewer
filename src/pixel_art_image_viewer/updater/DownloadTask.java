package pixel_art_image_viewer.updater;

import javafx.concurrent.Task;
import pixel_art_image_viewer.ConfigurationManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadTask extends Task<Void> {

    private ConfigurationManager configurationManager;
    private String downloadURL;

    public DownloadTask(ConfigurationManager configurationManager, String downloadURL) {
        this.configurationManager = configurationManager;
        this.downloadURL = downloadURL;
    }

    /** Code by slartidan
     * Link: https://stackoverflow.com/questions/22273045/java-getting-download-progress
     */
    @Override
    protected Void call() {
        try {
            URL url = new URL("https://downloads.sourceforge.net/project/pixel-art-image-viewer/v1.0.2/Pixel.Art.Image.Viewer.1.0.2.Setup.exe");

            long completeFileSize = getFileSize(url);

            //Get temp directory:
            String installPath = System.getProperty("java.io.tmpdir") + url.toString().substring(url.toString().lastIndexOf('/'));

            try(BufferedInputStream in = new BufferedInputStream(url.openStream());
                FileOutputStream fos = new FileOutputStream(installPath);
                BufferedOutputStream bout = new BufferedOutputStream(fos, 1024)) {

                byte[] data = new byte[1024];
                long downloadedFileSize = 0;
                int x = 0;

                while ((x = in.read(data, 0, 1024)) >= 0) {
                    downloadedFileSize += x;

                    //Calculate progress:
                    final int currentProgress = (int) ((((double) downloadedFileSize) / ((double) completeFileSize)) * 1000d);
                    updateProgress(currentProgress, 1000d);

                    bout.write(data, 0, x);
                }
            }

            //Run installer and exit program:
            Runtime.getRuntime().exec(installPath);
            System.exit(0);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Code by Michel Jung
     * Link: https://stackoverflow.com/questions/12800588/how-to-calculate-a-file-size-from-url-in-java
     */
    public long getFileSize(URL url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            return conn.getContentLengthLong();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
