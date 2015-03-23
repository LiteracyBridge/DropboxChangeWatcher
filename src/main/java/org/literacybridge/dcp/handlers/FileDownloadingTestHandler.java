package org.literacybridge.dcp.handlers;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import org.literacybridge.dcp.DcpConfiguration;

import java.io.IOException;

/**
 * Created by jefflub on 3/22/15.
 */
public class FileDownloadingTestHandler extends AbstractDropboxDeltaEventHandler {

    public FileDownloadingTestHandler(DbxClient dbxClient, DcpConfiguration dcpConfig) {
        super(dbxClient, dcpConfig);
    }

    @Override
    public boolean handle(String path, DbxEntry metadata) {
        if ( metadata != null && metadata.isFile() && path.startsWith( "/copytest") ){
            try {
                getFile(path);
                System.out.println("Copied file " + path );
            } catch (IOException e) {
                System.out.println( "Couldn't get file " + path + " due to IO problem" );
                e.printStackTrace();
            } catch (DbxException e) {
                System.out.println( "Couldn't get file " + path + " due to Dropbox problem" );
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
}
