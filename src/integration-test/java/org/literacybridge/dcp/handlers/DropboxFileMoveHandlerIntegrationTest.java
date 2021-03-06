package org.literacybridge.dcp.handlers;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxWriteMode;
import org.junit.Test;
import org.literacybridge.dcp.DcpConfiguration;
import org.literacybridge.dcp.DropboxChangeProcessor;
import org.literacybridge.dcp.IntegrationTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import static org.junit.Assert.*;
import static org.literacybridge.dcp.DropboxChangeProcessor.runProcessor;

/**
 * Created by jefflub on 5/21/15.
 */
public class DropboxFileMoveHandlerIntegrationTest {
    @Test
    public void testDropboxFileMoveHandler() throws Exception
    {
        String keyFile = System.getProperty( "testKeyFile" );
        assertNotNull( keyFile, "Missing keyFile system property" );
        System.out.println("Keyfile=" + keyFile);
        DcpConfiguration config = new DcpConfiguration( keyFile, "classpath:file-move-integration-config.properties" );

        assertNotNull(config.getAccessToken());

        DbxClient client = DropboxChangeProcessor.getDbxClient(config);

        String basePath = IntegrationTestUtils.getDropboxTestPath();
        String sourceRoot = basePath + "/source";
        String destinationRoot = basePath + "/destination";
        Path cursorFile = Files.createTempFile("dcp-test-", "-cursor.txt");
        System.out.println( "Cursor file is: " + cursorFile.toString() );
        DbxEntry entry = client.createFolder(basePath);
        if ( entry == null )
            throw new RuntimeException( "Couldn't create test folder " + basePath );

        config.overrideProperties("file-move-source-root", sourceRoot,
                "file-move-destination-root", destinationRoot,
                "state-file", cursorFile.toString(),
                "one-time-run", "true",
                "file-move-source-filter-regex", ".*txt");

        addFile(client, sourceRoot + "/source-nodelete.txt", 50);
        addFile(client, destinationRoot + "/destination-nodelete.txt", 50);
        addFile(client, sourceRoot + "/subdir/source-todelete.txt", 923);

        runProcessor(config, client);
        assertTrue(fileExists(client, sourceRoot, "/source-nodelete.txt"));
        assertTrue(fileExists(client, destinationRoot, "/destination-nodelete.txt"));

        // Test a basic move
        addFile(client, sourceRoot + "/MoveMe.txt", 100);
        // Test that a regex mismatch doesn't move
        addFile(client, sourceRoot + "/DontMoveMe.foo", 100);
        // Test that a file outside of the sourceroot, but that does regex match, doesn't move
        addFile(client, basePath + "/DontMoveMeEither.txt", 100);
        // Test that a file in a subdirectory of the source root moves
        addFile(client, sourceRoot + "/subdir/IShouldMove.txt", 100);
        // Test that deletions don't blow anything up
        client.delete(sourceRoot + "/subdir/source-todelete.txt");
        // Test that we see folder creations
        client.createFolder(sourceRoot + "/subidr/subsubdir");

        runProcessor(config, client);
        // Did we move the right file?
        assertFalse(fileExists(client, sourceRoot, "/MoveMe.txt"));
        assertTrue(fileExists(client, destinationRoot, "/MoveMe.txt"));
        // Did we not move the unmatching file?
        assertTrue(fileExists(client, sourceRoot, "/DontMoveMe.foo"));
        // Did we not move the file outside the source folder?
        assertTrue(fileExists(client, basePath, "/DontMoveMeEither.txt"));
        // Did we move the file in the subdirectory?
        assertFalse(fileExists(client, sourceRoot, "/subdir/IShouldMove.txt"));
        assertTrue(fileExists(client, destinationRoot, "/subdir/IShouldMove.txt"));
        // Is the deleted file nowhere to be found?
        assertFalse(fileExists(client, sourceRoot, "/subdir/source-todelete.txt"));
        assertFalse(fileExists(client, destinationRoot, "/subdir/source-todelete.txt"));

        // Validate case preservation
        entry = client.getMetadata( destinationRoot + "/moveme.txt" );
        assertEquals( entry.path, destinationRoot + "/MoveMe.txt" );

        // Clean up
        Files.delete(cursorFile);
        client.delete( basePath );
    }

    private boolean fileExists( DbxClient client, String root, String path ) throws DbxException
    {
        return ( client.getMetadata( root + path ) != null );
    }

    private DbxEntry.File addFile(DbxClient client, String path, int length)
            throws DbxException, IOException
    {
        return uploadFile(client, path, length, DbxWriteMode.add());
    }

    private DbxEntry.File uploadFile(DbxClient client, String path, int length, DbxWriteMode writeMode)
            throws DbxException, IOException
    {
        return client.uploadFile(path, writeMode, length, new ByteArrayInputStream(generateRandomBytes(length)));
    }

    private static byte[] generateRandomBytes(int numBytes)
    {
        byte[] data = new byte[numBytes];
        Random random = new Random();
        for (int i = 0; i < numBytes; i++) {
            String randomFileData = "\nabcdefghijklmnopqrstuvwxyz0123456789";
            data[i] = (byte) randomFileData.charAt(random.nextInt(randomFileData.length()));
        }
        return data;
    }
}
