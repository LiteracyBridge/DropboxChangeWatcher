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
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by jefflub on 3/21/15.
 */
public class DropboxChangeProcessor {

    private static final class MyHttpRequestor extends StandardHttpRequestor
    {
        @Override
        protected void configureConnection(HttpsURLConnection conn) throws IOException {
            super.configureConnection( conn );
            conn.setReadTimeout( 125 * 1000 );
        }
    }

    public static void main( String[] args ) throws IOException, DbxException, InterruptedException {
        final String APP_KEY = "dcmewl4bae8dhhh";
        final String APP_SECRET = "jbryotuhmlo5txs";

        DbxAppInfo appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);

        DbxRequestConfig config = new DbxRequestConfig(
                "LiteracyBridge DBChangeProcessor/1.0", Locale.getDefault().toString(), new MyHttpRequestor());
        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);

        // Have the user sign in and authorize your app.
        /*
        String authorizeUrl = webAuth.start();
        System.out.println("1. Go to: " + authorizeUrl);
        System.out.println("2. Click \"Allow\" (you might have to log in first)");
        System.out.println("3. Copy the authorization code.");
        String code = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();
        DbxAuthFinish authFinish = webAuth.finish( code );
        */

        String accessToken = "nO4h8oh1-9QAAAAAAAAB8TBCboxaaDBxolzEf1rt_zNw0Ee3X_b08WYwDUFR4pBC";
        DbxClient client = new DbxClient( config, accessToken );
        System.out.println("Linked account: " + client.getAccountInfo().displayName);

        String cursor = getLatestDeltaCursor( config, accessToken );
        System.out.println( "Latest Delta Cursor: " + cursor );

        while ( true )
        {
            LongpollResponse longpollResponse = pollForChanges( config, accessToken, cursor );
            System.out.println( "Response! Changes: " + longpollResponse.changes + " Backoff: " + longpollResponse.backoff );
            if ( longpollResponse.changes )
            {
                cursor = processChanges( client, cursor );
            }
            Thread.sleep( longpollResponse.backoff * 1000 );
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
                    }
                    else {
                        JsonReader.skipValue(parser);
                    }
                }
                catch (JsonReadException ex) {
                    throw ex.addFieldContext(fieldName);
                }
            }

            JsonReader.expectObjectEnd(parser);

            if (cursorId == null) throw new JsonReadException("missing field \"upload_id\"", top);

            return cursorId;
        }
    };

    private static String getLatestDeltaCursor( DbxRequestConfig config, String accessToken ) throws DbxException {
        String[] params = {
                "cursor", null,
                "path_prefix", null,
        };

        return DbxRequestUtil.doPost(config, accessToken, DbxHost.Default.api, "1/delta/latest_cursor", params, null,
                new DbxRequestUtil.ResponseHandler<String>() {
                    @Override
                    public String handle(HttpRequestor.Response response) throws DbxException {
                        if (response.statusCode != 200) throw DbxRequestUtil.unexpectedStatus(response);
                        return DbxRequestUtil.readJsonFromResponse( CursorReader, response.body);
                    }
                });
    }

    private static class LongpollResponse
    {
        public boolean changes;
        public long backoff;

        public LongpollResponse( boolean changes, long backoff )
        {
            this.changes = changes;
            this.backoff = backoff;
        }

        public static final class Reader extends JsonReader<LongpollResponse>
        {
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
                        }
                        else if (fieldName.equals("backoff")) {
                            backoff = JsonReader.readUnsignedLongField( parser, fieldName, 0);
                        }
                        else {
                            JsonReader.skipValue(parser);
                        }
                    }
                    catch (JsonReadException ex) {
                        throw ex.addFieldContext(fieldName);
                    }
                }

                JsonReader.expectObjectEnd(parser);

                return new LongpollResponse( changes, backoff );
            }
        }
    }

    private static LongpollResponse pollForChanges( DbxRequestConfig config, String accessToken, String cursor ) throws DbxException {
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

    private static final String processChanges( DbxClient client, String cursor ) throws DbxException {
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
        } while ( delta.hasMore );

        return delta.cursor;
    }
}
