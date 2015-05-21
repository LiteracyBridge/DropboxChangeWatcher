package org.literacybridge.dcp;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration values
 */
public class DcpConfiguration {

    private static final int DEFAULT_DROPBOX_POLL_TIMEOUT = 30;

    Properties properties;

    public DcpConfiguration(String propertiesFile, String keyFile) throws IOException {
        properties = new Properties();
        properties.load(new FileReader(propertiesFile));
        if ( keyFile != null ) {
            if ( getAppKey() != null || getAppSecret() != null || getAccessToken() != null )
                throw new IOException("Key that should be secret is in main config file!");
            properties.load(new FileReader(keyFile));
        }
        if (getAppKey() == null || getAppSecret() == null)
            throw new IOException("AppKey or AppSecret is missing from properties file.");
    }

    public String getAppKey() {
        return properties.getProperty("dropbox-app-key");
    }

    public String getAppSecret() {
        return properties.getProperty("dropbox-app-secret");
    }

    public String getAccessToken() {
        return properties.getProperty("dropbox-access-token");
    }

    public String getConfigFileDropboxLocation() {
        return properties.getProperty("config-file-dropbox-location");
    }

    public int getPollTimeout() {
        return Integer.getInteger(properties.getProperty("dropbox-poll-timeout"), DEFAULT_DROPBOX_POLL_TIMEOUT);
    }

    public String getStateFileName() {
        return properties.getProperty("state-file");
    }

    public boolean isOneTimeRun() { return Boolean.parseBoolean(properties.getProperty("one-time-run")); }

    public String getTempDownloadDirectory() {
        return properties.getProperty("temp-download-dir");
    }

    public String getFileMoveDestinationRoot() {
        return properties.getProperty("file-move-destination-root");
    }

    public String getFileMoveSourceRoot() {
        return properties.getProperty("file-move-source-root");
    }

    public String getFileMoveFilterRegex() { return properties.getProperty("file-move-source-filter-regex"); }

    public boolean isFileMoveDryRun() { return Boolean.parseBoolean( properties.getProperty("file-move-dry-run") ); }
}
