package org.literacybridge.dcp.handlers;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import org.literacybridge.dcp.DcpConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Created by jefflub on 3/22/15.
 */
public class DropboxFileMoveHandler extends AbstractDropboxDeltaEventHandler {
    final static Logger logger = LoggerFactory.getLogger(DropboxFileMoveHandler.class);
    private String destinationRoot;
    private String sourceRoot;
    private Pattern sourceMatchPattern;
    private boolean dryRun = false;

    public DropboxFileMoveHandler(DbxClient dbxClient, DcpConfiguration dcpConfig) {
        super(dbxClient, dcpConfig);
        destinationRoot = dcpConfig.getFileMoveDestinationRoot();
        sourceRoot = dcpConfig.getFileMoveSourceRoot();
        sourceMatchPattern = Pattern.compile(dcpConfig.getFileMoveFilterRegex());
        dryRun = dcpConfig.isFileMoveDryRun();
        if (dryRun)
            logger.info("DropboxFileMoveHandler handler running in dry-run mode");
    }

    @Override
    public boolean handle(String path, DbxEntry metadata) {
        if ( metadata != null && metadata.isFile() && path.startsWith(sourceRoot) && sourceMatchPattern.matcher(path).matches() )
        {
            String newPath = path.replace(sourceRoot, destinationRoot);
            logger.info( "Moving " + path + " to " + newPath + (dryRun ? " *** SKIPPING..." : "" ) );
            try {
                if ( !dryRun)
                    dbxClient.move( path, newPath );
            } catch (DbxException e) {
                logger.error("Failed to move file", e);
            }
            return true;
        }
        return false;
    }
}
