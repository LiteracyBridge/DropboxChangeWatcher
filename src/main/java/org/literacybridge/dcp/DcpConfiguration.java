package org.literacybridge.dcp;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

/**
 * Configuration values
 */
public class DcpConfiguration {

    private static final int DEFAULT_DROPBOX_POLL_TIMEOUT = 30;

    Properties properties;

    public DcpConfiguration(String propertiesFile, String keyFile) throws IOException {
        properties = new Properties();

        Reader propReader = null;
        if ( propertiesFile.startsWith("classpath:") ) {
            String propPath = propertiesFile.substring(propertiesFile.indexOf(":") + 1);
            propReader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(propPath));
        }
        else
            propReader = new FileReader(propertiesFile);
        properties.load(propReader);
        propReader.close();

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

    /**
     * For test purposes.
     *
     * @param props
     */
    public void overrideProperties( String... props ){
        if ( props == null || props.length == 0 || props.length % 2 != 0 ) {
            throw new RuntimeException( "Invalid property override" );
        }

        for ( int i = 0; i < props.length; i += 2 ) {
            properties.setProperty( props[i], props[i + 1] );
        }
    }
}
