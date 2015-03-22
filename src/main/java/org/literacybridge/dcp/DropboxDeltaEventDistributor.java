package org.literacybridge.dcp;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import org.literacybridge.dcp.DcpConfiguration;
import org.literacybridge.dcp.handlers.AbstractDropboxDeltaEventHandler;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by jefflub on 3/22/15.
 */
public class DropboxDeltaEventDistributor  {

    private List<AbstractDropboxDeltaEventHandler> handlers = new LinkedList<AbstractDropboxDeltaEventHandler>();

    public void addHandler(AbstractDropboxDeltaEventHandler handler) {
        handlers.add(handler);
    }

    public void distributeEvent(String path, DbxEntry metadata) {
        for ( AbstractDropboxDeltaEventHandler h : handlers ){
            if ( h.handle( path, metadata ) )
                break;
        }
    }
}
