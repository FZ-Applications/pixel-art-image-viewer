package pixel_art_image_viewer;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class ConfigurationManager {

    public static SimpleDateFormat dateFormatter = new SimpleDateFormat("MM,dd,yyyy");

    private boolean checkForUpdates;
    private String skippedUpdateVersion;
    private Date lastCheckedForUpdates;
    private int checkForUpdatesEveryXDays;
    private final File configFile;

    public ConfigurationManager() throws ParseException, IOException {
        //Create file if not exists:
        String dir = System.getProperty("user.home") + "/Documents/Pixel Art Image Viewer";
        File directory = new File(dir);

        if (!directory.exists()) {
            directory.mkdir();
        }

        configFile = new File(dir + "/config.ini");

        //Create file if not exists:
        if (!configFile.exists()) {
            checkForUpdates = true;
            skippedUpdateVersion = "0.0.0";
            lastCheckedForUpdates = dateFormatter.parse("01,01,0001");
            checkForUpdatesEveryXDays = 7;
            saveConfig();
        }

        //Read config:
        try (InputStream input = new FileInputStream(configFile)) {
            Properties prop = new Properties();
            prop.load(input);
            checkForUpdates = Boolean.parseBoolean(prop.getProperty("checkForUpdates", "false"));
            skippedUpdateVersion = prop.getProperty("skippedUpdateVersion", "0.0.0");
            lastCheckedForUpdates = dateFormatter.parse(prop.getProperty("lastCheckedForUpdates", "01,01,0001"));
            checkForUpdatesEveryXDays = Integer.parseInt(prop.getProperty("checkForUpdatesEveryXDays", "7"));
        }
    }

    public void saveConfig() {
        try (OutputStream output = new FileOutputStream(configFile)) {
            Properties prop = new Properties();
            prop.setProperty("checkForUpdates", checkForUpdates ? "true" : "false");
            prop.setProperty("skippedUpdateVersion", skippedUpdateVersion);
            prop.setProperty("lastCheckedForUpdates", dateFormatter.format(lastCheckedForUpdates));
            prop.setProperty("checkForUpdatesEveryXDays", checkForUpdatesEveryXDays + "");
            prop.store(output, null);
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public boolean isCheckForUpdates() {
        return checkForUpdates;
    }

    public void setCheckForUpdates(boolean checkForUpdates) {
        this.checkForUpdates = checkForUpdates;
        saveConfig();
    }

    public String getSkippedUpdateVersion() {
        return skippedUpdateVersion;
    }

    public void setSkippedUpdateVersion(String skippedUpdateVersion) {
        this.skippedUpdateVersion = skippedUpdateVersion;
        saveConfig();
    }

    public Date getLastCheckedForUpdates() {
        return lastCheckedForUpdates;
    }

    public void setLastCheckedForUpdates(Date lastCheckedForUpdates) {
        this.lastCheckedForUpdates = lastCheckedForUpdates;
        saveConfig();
    }

    public int getCheckForUpdatesEveryXDays() {
        return checkForUpdatesEveryXDays;
    }

    public void setCheckForUpdatesEveryXDays(int checkForUpdatesEveryXDays) {
        this.checkForUpdatesEveryXDays = checkForUpdatesEveryXDays;
        saveConfig();
    }
}
