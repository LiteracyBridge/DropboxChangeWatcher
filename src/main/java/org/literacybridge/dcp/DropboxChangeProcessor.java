package org.literacybridge.dcp;

import com.dropbox.core.*;
import com.dropbox.core.http.StandardHttpRequestor;
import org.literacybridge.dcp.handlers.DropboxFileMoveHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * Created by jefflub on 3/21/15.
 */
public class DropboxChangeProcessor {

    final static Logger logger = LoggerFactory.getLogger( DropboxChangeProcessor.class );

    public static void main(String[] args) throws IOException, DbxException, InterruptedException {

        if (args.length < 1 || args.length > 2) {
            System.out.println("Usage: DropboxChangeProcessor <properties file> [<key file>]");
            System.exit(-1);
        }

        DcpConfiguration dcpConfig = null;
        try {
            dcpConfig = new DcpConfiguration(args[0], args.length == 2 ? args[1] : null);
        } catch (IOException ex) {
            System.out.println("Failed to load properties file " + args[0]);
            ex.printStackTrace();
            System.exit(-1);
        }

        DbxClient client = getDbxClient(dcpConfig);

        logger.info("Linked account: {}", client.getAccountInfo().displayName);

        runProcessor(dcpConfig, client);
    }

    public static void runProcessor(DcpConfiguration dcpConfig, DbxClient client) throws DbxException {
        DropboxDeltaEventDistributor distributor = new DropboxDeltaEventDistributor();
        distributor.addHandler(new DropboxFileMoveHandler(client, dcpConfig));
        //distributor.addHandler(new FileDownloadingTestHandler(client, dcpConfig));

        DropboxDeltaEventSource eventGenerator = new DropboxDeltaEventSource(client, dcpConfig, distributor);
        eventGenerator.watchDropbox();
    }

    public static DbxClient getDbxClient(DcpConfiguration dcpConfig) throws IOException, DbxException {
        DbxAppInfo appInfo = new DbxAppInfo(dcpConfig.getAppKey(), dcpConfig.getAppSecret());

        DbxRequestConfig config = new DbxRequestConfig("LiteracyBridge DBChangeProcessor/1.0",
                Locale.getDefault().toString(), new MyHttpRequestor(dcpConfig.getPollTimeout()));

        if (dcpConfig.getAccessToken() == null) {
            generateOAuthAccessToken(appInfo, config);
            System.exit(0);
        }

        String accessToken = dcpConfig.getAccessToken();
        return new DbxClient(config, accessToken);
    }

    private static void generateOAuthAccessToken(DbxAppInfo appInfo, DbxRequestConfig config) throws IOException, DbxException {
        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);
        String authorizeUrl = webAuth.start();
        System.out.println("1. Go to: " + authorizeUrl);
        System.out.println("2. Click \"Allow\" (you might have to log in first)");
        System.out.print("3. Copy the authorization code and enter here: ");
        String code = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();
        DbxAuthFinish authFinish = webAuth.finish(code);
        System.out.println("Access token is '" + authFinish.accessToken + "'");
        System.out.println("Add this to the properties file as a property named 'dropbox-access-token'.");
    }

    /**
     * The standard HttpRequestor in the Dropbox API times out after 35 seconds, but longpoll reads can be as long as
     * the longpoll timeout PLUS 90 seconds. Make the read timeout at least longpoll timeout, plus 90, plus a bonus
     * 5 for good luck.
     */
    private static final class MyHttpRequestor extends StandardHttpRequestor {
        private int readTimeoutSeconds;

        private MyHttpRequestor(int longpollTimeout) {
            this.readTimeoutSeconds = longpollTimeout + 95;
        }

        @Override
        protected void configureConnection(HttpsURLConnection conn) throws IOException {
            super.configureConnection(conn);
            conn.setReadTimeout(readTimeoutSeconds * 1000);
        }
    }


}
