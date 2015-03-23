package org.literacybridge.dcp.handlers;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import org.literacybridge.dcp.DcpConfiguration;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class AbstractDropboxDeltaEventHandler {
    protected DbxClient dbxClient;
    protected DcpConfiguration dcpConfig;

    public AbstractDropboxDeltaEventHandler(DbxClient dbxClient, DcpConfiguration dcpConfig) {
        this.dbxClient = dbxClient;
        this.dcpConfig = dcpConfig;
    }

    public abstract boolean handle(String path, DbxEntry metadata);

    /**
     * Gets a file from Dropbox and puts it under the configured temporary download root at the same
     * relative path.
     *
     * @param path Path to the file in Dropbox
     * @throws IOException
     * @throws DbxException
     */
    protected void getFile(String path) throws IOException, DbxException {
        Files.createDirectories( Paths.get( dcpConfig.getTempDownloadDirectory() + path ).getParent() );
        FileOutputStream outputStream = new FileOutputStream(dcpConfig.getTempDownloadDirectory() + path);
        try {
            DbxEntry.File downloadedFile = dbxClient.getFile(path, null,
                    outputStream);
        } finally {
            outputStream.close();
        }
    }

    /**
     * Removes a file from the configured temp download directory
     *
     * @param path Path to file returned from Dropbox
     * @throws IOException
     */
    protected void deleteTempFile(String path) throws IOException {
        Files.delete(Paths.get(dcpConfig.getTempDownloadDirectory() + path));
    }
}
