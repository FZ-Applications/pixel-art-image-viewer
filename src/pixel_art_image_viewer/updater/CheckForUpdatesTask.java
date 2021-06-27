package pixel_art_image_viewer.updater;

import javafx.concurrent.Task;
import pixel_art_image_viewer.ConfigurationManager;

import java.io.*;
import java.net.URL;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * Task checks for updates and returns the download path of the installer of the new version.
 * If no new version was found a empty string gets returned.
 */
public class CheckForUpdatesTask extends Task<String> {

    private static final String WEB_API_PATH = "https://sourceforge.net/projects/pixel-art-image-viewer/best_release.json";

    /**
     * Used as a secondary return value, to display the new version in the update alert box.
     */
    public Version newVersion;

    private final Version currentVersion;
    private final ConfigurationManager configurationManager;

    public CheckForUpdatesTask(Version currentVersion, ConfigurationManager configurationManager) {
        this.currentVersion = currentVersion;
        this.configurationManager = configurationManager;
    }

    @Override
    protected String call() {
        try {
            if (autoUpdateNecessary(configurationManager)) {
                configurationManager.setLastCheckedForUpdates(new Date());
                return updateAvailable(currentVersion);
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return "";
    }

    /**
     * Reads the sourceforge web api to get the current version and compares it with the current version.
     */
    private String updateAvailable(Version currentVersion) throws IOException {
        URL url = new URL(WEB_API_PATH);
        //Read web api:
        try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String downloadURL = getDownloadURL(in.readLine());

            newVersion = getVersionFromURL(downloadURL);

            //Version check:
            if (newVersion.compareTo(currentVersion) > 0) {
                if (!newVersion.get().equals(configurationManager.getSkippedUpdateVersion())) {
                    return downloadURL;
                }
            }
        }

        return "";
    }

    /**
     * Extracts the version from the download url.
     */
    private Version getVersionFromURL(String downloadURL) {
        String version = downloadURL.substring(0, downloadURL.lastIndexOf('/')); //Remove unused chars after of the version
        version = version.substring(version.lastIndexOf('/') + 2); //Remove unused chars in front of the version
        return new Version(version);
    }

    /**
     * Extracts the download url from the sourceforge web api.
     */
    private String getDownloadURL(String line) {
        String urlText = "";

        //Split key-value pairs:
        StringTokenizer stringTokenizer = new StringTokenizer(line, ",");
        String lookForKey = " \"url\": \"";

        while (stringTokenizer.hasMoreTokens()) {
            String keyValuePair = stringTokenizer.nextToken();

            if (keyValuePair.startsWith(lookForKey)) {
                //Get version from api string:
                urlText = keyValuePair.substring(lookForKey.length()); //Remove unused chars in front of the url
                urlText = urlText.substring(0, urlText.indexOf('"')); //Remove unused chars after
                if (urlText.contains(".exe")) {
                    urlText = urlText.substring(0, urlText.indexOf('?'));
                    return urlText;
                }
            }
        }
        return urlText;
    }

    private boolean autoUpdateNecessary(ConfigurationManager configurationManager) {
        if (configurationManager.isCheckForUpdates()) { //Is autoupdate enabled?
            Calendar nextCheckCalender = Calendar.getInstance();
            nextCheckCalender.setTime(configurationManager.getLastCheckedForUpdates());
            nextCheckCalender.add(Calendar.DATE, configurationManager.getCheckForUpdatesEveryXDays());

            //Is last check older then e.g. 7 days.
            return nextCheckCalender.before(Calendar.getInstance());
        }

        return false;
    }
}