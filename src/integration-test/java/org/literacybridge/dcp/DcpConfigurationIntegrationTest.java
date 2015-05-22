package org.literacybridge.dcp;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxWriteMode;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.literacybridge.dcp.IntegrationTestUtils.getDropboxTestPath;

/**
 * Created by jefflub on 5/22/15.
 */
public class DcpConfigurationIntegrationTest {
    @Test
    public void testDropboxDownloading() throws Exception
    {
        String keyFile = System.getProperty( "testKeyFile" );
        DcpConfiguration config = new DcpConfiguration();
        config.loadPropertiesFile( keyFile );

        String dropboxConfigFilePath = getDropboxTestPath() + "/configtest.properties";
        DbxClient client = DropboxChangeProcessor.getDbxClient( config );
        InputStream reader = Thread.currentThread().getContextClassLoader().getResourceAsStream("file-move-integration-config.properties" );
        client.uploadFile(dropboxConfigFilePath, DbxWriteMode.force(), -1, reader);
        config.overrideProperties( "config-file-dropbox-location", dropboxConfigFilePath );

        config.loadConfigFromDropbox();
        assertTrue(config.getAccessToken() != null);
        assertEquals(config.getFileMoveSourceRoot(), "/lb-outbox");

        client.delete( dropboxConfigFilePath );
    }
}
