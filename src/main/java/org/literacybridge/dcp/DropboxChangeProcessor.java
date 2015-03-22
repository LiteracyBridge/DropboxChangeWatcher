package org.literacybridge.dcp;

import com.dropbox.core.*;
import com.dropbox.core.http.HttpRequestor;
import com.dropbox.core.http.StandardHttpRequestor;
import com.dropbox.core.json.JsonReadException;
import com.dropbox.core.json.JsonReader;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * Created by jefflub on 3/21/15.
 */
public class DropboxChangeProcessor {

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

    public static void main(String[] args) throws IOException, DbxException, InterruptedException {

        if (args.length != 1) {
            System.out.println("Usage: DropboxChangeProcessor <properties file>");
            System.exit(-1);
        }

        DcpConfiguration dcpConfig = null;
        try {
            dcpConfig = new DcpConfiguration(args[0]);
        } catch (IOException ex) {
            System.out.println("Failed to load properties file " + args[0]);
            ex.printStackTrace();
            System.exit(-1);
        }

        DbxAppInfo appInfo = new DbxAppInfo(dcpConfig.getAppKey(), dcpConfig.getAppSecret());

        DbxRequestConfig config = new DbxRequestConfig("LiteracyBridge DBChangeProcessor/1.0",
                Locale.getDefault().toString(), new MyHttpRequestor(dcpConfig.getPollTimeout()));

        if (dcpConfig.getAccessToken() == null) {
            DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);
            String authorizeUrl = webAuth.start();
            System.out.println("1. Go to: " + authorizeUrl);
            System.out.println("2. Click \"Allow\" (you might have to log in first)");
            System.out.print("3. Copy the authorization code and enter here: ");
            String code = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();
            DbxAuthFinish authFinish = webAuth.finish(code);
            System.out.println("Access token is '" + authFinish.accessToken + "'");
            System.out.println("Add this to the properties file as a property named 'dropbox-access-token'.");
            System.exit(0);
        }

        String accessToken = dcpConfig.getAccessToken();
        DbxClient client = new DbxClient(config, accessToken);

        System.out.println("Linked account: " + client.getAccountInfo().displayName);

        String cursor = getLatestDeltaCursor(config, accessToken);
        System.out.println("Latest Delta Cursor: " + cursor);

        while (true) {
            LongpollResponse longpollResponse = pollForChanges(config, accessToken, cursor);
            System.out.println("Response! Changes: " + longpollResponse.changes + " Backoff: " + longpollResponse.backoff);
            if (longpollResponse.changes) {
                cursor = processChanges(client, cursor);
            }
            Thread.sleep(longpollResponse.backoff * 1000);
        }
    }

    private static JsonReader<String> CursorReader = new JsonReader<String>() {
        @Override
        public String read(JsonParser parser) throws IOException, JsonReadException {
            JsonLocation top = JsonReader.expectObjectStart(parser);

            String cursorId = null;

            while (parser.getCurrentToken() == JsonToken.FIELD_NAME) {
                String fieldName = parser.getCurrentName();
                parser.nextToken();

                try {
                    if (fieldName.equals("cursor")) {
                        cursorId = JsonReader.StringReader.readField(parser, fieldName, cursorId);
                    } else {
                        JsonReader.skipValue(parser);
                    }
                } catch (JsonReadException ex) {
                    throw ex.addFieldContext(fieldName);
                }
            }

            JsonReader.expectObjectEnd(parser);

            if (cursorId == null) throw new JsonReadException("missing field \"upload_id\"", top);

            return cursorId;
        }
    };

    private static String getLatestDeltaCursor(DbxRequestConfig config, String accessToken) throws DbxException {
        String[] params = {
                "cursor", null,
                "path_prefix", null,
        };

        return DbxRequestUtil.doPost(config, accessToken, DbxHost.Default.api, "1/delta/latest_cursor", params, null,
                new DbxRequestUtil.ResponseHandler<String>() {
                    @Override
                    public String handle(HttpRequestor.Response response) throws DbxException {
                        if (response.statusCode != 200) throw DbxRequestUtil.unexpectedStatus(response);
                        return DbxRequestUtil.readJsonFromResponse(CursorReader, response.body);
                    }
                });
    }

    private static class LongpollResponse {
        public boolean changes;
        public long backoff;

        public LongpollResponse(boolean changes, long backoff) {
            this.changes = changes;
            this.backoff = backoff;
        }

        public static final class Reader extends JsonReader<LongpollResponse> {
            @Override
            public LongpollResponse read(JsonParser parser) throws IOException, JsonReadException {
                JsonLocation top = JsonReader.expectObjectStart(parser);

                boolean changes = false;
                long backoff = 0;

                while (parser.getCurrentToken() == JsonToken.FIELD_NAME) {
                    String fieldName = parser.getCurrentName();
                    parser.nextToken();

                    try {
                        if (fieldName.equals("changes")) {
                            changes = JsonReader.BooleanReader.readField(parser, fieldName, null);
                        } else if (fieldName.equals("backoff")) {
                            backoff = JsonReader.readUnsignedLongField(parser, fieldName, 0);
                        } else {
                            JsonReader.skipValue(parser);
                        }
                    } catch (JsonReadException ex) {
                        throw ex.addFieldContext(fieldName);
                    }
                }

                JsonReader.expectObjectEnd(parser);

                return new LongpollResponse(changes, backoff);
            }
        }
    }

    private static LongpollResponse pollForChanges(DbxRequestConfig config, String accessToken, String cursor) throws DbxException {
        String[] params = {
                "cursor", cursor,
                "timeout", "30"
        };

        return DbxRequestUtil.doGet(config, accessToken, "api-notify.dropbox.com", "1/longpoll_delta", params, null,
                new DbxRequestUtil.ResponseHandler<LongpollResponse>() {
                    @Override
                    public LongpollResponse handle(HttpRequestor.Response response) throws DbxException {
                        if (response.statusCode != 200) throw DbxRequestUtil.unexpectedStatus(response);
                        return DbxRequestUtil.readJsonFromResponse(new LongpollResponse.Reader(), response.body);
                    }
                });
    }

    private static final String processChanges(DbxClient client, String cursor) throws DbxException {
        DbxDelta<DbxEntry> delta = null;
        do {
            delta = client.getDelta(cursor);
            for (DbxDelta.Entry<DbxEntry> e : delta.entries) {
                System.out.println("Changed file: " + e.lcPath);
                if (e.metadata == null)
                    System.out.println("File deleted");
                else
                    System.out.println("File added/changed");
            }
            cursor = delta.cursor;
        } while (delta.hasMore);

        return delta.cursor;
    }
}
