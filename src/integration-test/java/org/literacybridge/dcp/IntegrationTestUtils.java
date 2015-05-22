package org.literacybridge.dcp;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jefflub on 5/22/15.
 */
public class IntegrationTestUtils {
    public static String getDropboxTestPath()
    {
        String timestamp = new SimpleDateFormat("yyyy-mm-dd HH.mm.ss").format(new Date());
        return "/DcpTests/" + timestamp;
    }
}
