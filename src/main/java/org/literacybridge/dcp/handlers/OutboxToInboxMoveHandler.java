package org.literacybridge.dcp.handlers;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import org.literacybridge.dcp.DcpConfiguration;

/**
 * Created by jefflub on 3/22/15.
 */
public class OutboxToInboxMoveHandler extends AbstractDropboxDeltaEventHandler {
    private String inboxRoot;
    private String outboxRoot;
    public OutboxToInboxMoveHandler(DbxClient dbxClient, DcpConfiguration dcpConfig) {
        super(dbxClient, dcpConfig);
        inboxRoot = dcpConfig.getInboxRoot();
        outboxRoot = dcpConfig.getOutboxRoot();
    }

    @Override
    public boolean handle(String path, DbxEntry metadata) {
        if ( metadata != null && metadata.isFile() && path.startsWith( outboxRoot ) )
        {
            String newPath = path.replace( outboxRoot, inboxRoot );
            System.out.println( "Moving " + path + " to " + newPath );
            try {
                dbxClient.move( path, newPath );
            } catch (DbxException e) {
                System.out.println("Failed to move file");
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
}
