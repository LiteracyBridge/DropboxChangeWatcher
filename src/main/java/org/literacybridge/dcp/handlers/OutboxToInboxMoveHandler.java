package org.literacybridge.dcp.handlers;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import org.literacybridge.dcp.DcpConfiguration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jefflub on 3/22/15.
 */
public class OutboxToInboxMoveHandler extends AbstractDropboxDeltaEventHandler {
    private String inboxRoot;
    private String outboxRoot;
    private Pattern outboxPattern;
    private boolean outboxDryRun = false;

    public OutboxToInboxMoveHandler(DbxClient dbxClient, DcpConfiguration dcpConfig) {
        super(dbxClient, dcpConfig);
        inboxRoot = dcpConfig.getInboxRoot();
        outboxRoot = dcpConfig.getOutboxRoot();
        outboxPattern = Pattern.compile(dcpConfig.getOutboxRegex());
        outboxDryRun = dcpConfig.isOutboxDryRun();
        if ( outboxDryRun )
            System.out.println("OutboxToInbox handler running in dry-run mode" );
    }

    @Override
    public boolean handle(String path, DbxEntry metadata) {
        if ( metadata != null && metadata.isFile() && path.startsWith( outboxRoot ) && outboxPattern.matcher(path).matches() )
        {
            String newPath = path.replace( outboxRoot, inboxRoot );
            System.out.println( "Moving " + path + " to " + newPath + (outboxDryRun ? " *** SKIPPING..." : "" ) );
            try {
                if ( !outboxDryRun )
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
