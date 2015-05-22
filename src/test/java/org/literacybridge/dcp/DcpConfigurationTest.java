package org.literacybridge.dcp;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by jefflub on 5/21/15.
 */
public class DcpConfigurationTest {

    @Test
    public void testDcpConfiguration() throws Exception
    {
        DcpConfiguration config = new DcpConfiguration( "classpath:test-config-with-keys.properties", null );
        assertTrue( config.getAccessToken() != null );
    }

    @Test( expected = IOException.class )
    public void testInvalidConfiguration() throws Exception
    {
        DcpConfiguration config = new DcpConfiguration( "classpath:test-config-no-keys.properties", null );
    }
}
