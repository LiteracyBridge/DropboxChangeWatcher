package org.literacybridge.dcp;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Configuration values
 */
public class DcpConfiguration {
    final static Logger logger = LoggerFactory.getLogger(DcpConfiguration.class);

    private static final int DEFAULT_DROPBOX_POLL_TIMEOUT = 30;

    Properties properties;

    DcpConfiguration()
    {
        // Testing only constructor
    }

    public DcpConfiguration(String keyFile) throws IOException, DbxException {
        logger.info( "Loading key file: {}", keyFile );
        loadPropertiesFile(keyFile);
        if ( getAppKey() == null || getAppSecret() == null )
            throw new RuntimeException( "Missing app keys from key file." );
        if ( getAccessToken() == null ) {
            // This can be used for generating an access token, but not much else
            return;
        }

        loadConfigFromDropbox();
    }

    void loadConfigFromDropbox() throws IOException, DbxException {
        // If there's a config file in Dropbox, load it. Otherwise assume all config is in the key file
        if ( getConfigFileDropboxLocation() != null ) {
            logger.info( "Getting config file from Dropbox. Path: {}", getConfigFileDropboxLocation() );
            DbxClient client = DropboxChangeProcessor.getDbxClient(this);
            Path configFilePath = Files.createTempFile("dcp-config-", ".properties");
            FileOutputStream output = new FileOutputStream(configFilePath.toString());
            DbxEntry.File configFile = client.getFile(getConfigFileDropboxLocation(), null, output);
            output.close();
            if (configFile == null)
                throw new RuntimeException("Missing Dropbox config file at '" + getConfigFileDropboxLocation() + "'");
            loadPropertiesFile(configFilePath.toString());
            Files.delete(configFilePath);
        }
    }

    public DcpConfiguration(String keyFile, String propertiesFile) throws IOException {
        loadPropertiesFile( propertiesFile );
        if ( getAppKey() != null || getAppSecret() != null || getAccessToken() != null )
            throw new IOException("Key that should be secret is in main config file!");
        loadPropertiesFile( keyFile );
    }

    void loadPropertiesFile( String filePath ) throws IOException
    {
        if ( properties == null ) {
            properties = new Properties();
        }

        Reader reader = null;
        if ( filePath.startsWith( "classpath:" ) ) {
            String propPath = filePath.substring(filePath.indexOf(":") + 1);
            reader = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(propPath));
        }
        else {
            reader = new FileReader( filePath );
        }
        properties.load( reader );
        reader.close();
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

    public boolean isFileMoveDryRun() { return Boolean.parseBoolean(properties.getProperty("file-move-dry-run")); }

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
