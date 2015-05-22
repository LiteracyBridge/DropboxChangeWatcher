package org.literacybridge.dcp;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by jefflub on 5/21/15.
 */
public class DcpConfigurationTest {

    @Test
    public void testDcpConfigurationOneFile() throws Exception
    {
        DcpConfiguration config = new DcpConfiguration( "classpath:test-config-with-keys.properties" );
        assertTrue( config.getAccessToken() != null );
        assertEquals( config.getFileMoveSourceRoot(), "/lb-outbox" );
    }

    @Test
    public void testDcpConfigurationTwoFiles() throws Exception
    {
        DcpConfiguration config = new DcpConfiguration( "classpath:test-config-keys-only.properties", "classpath:test-config-no-keys.properties" );
        assertTrue( config.getAccessToken() != null );
        assertEquals( config.getFileMoveSourceRoot(), "/lb-outbox" );
    }
}
