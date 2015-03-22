package org.literacybridge.dcp;

import com.dropbox.core.*;
import com.dropbox.core.http.HttpRequestor;
import com.dropbox.core.json.JsonReadException;
import com.dropbox.core.json.JsonReader;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by jefflub on 3/22/15.
 */
public class DropboxDeltaEventSource {

    DbxClient client;
    DcpConfiguration config;
    DropboxDeltaEventDistributor distributor;

    public DropboxDeltaEventSource(DbxClient client, DcpConfiguration config, DropboxDeltaEventDistributor distributor) {
        this.client = client;
        this.config = config;
        this.distributor = distributor;
    }

    public void watchDropbox() throws DbxException {
        String cursor = readDeltaCursorFromFile();

        // If no delta state, get latest cursor
        if (cursor == null) {
            cursor = getLatestDeltaCursor(client);
            System.out.println("Latest delta cursor: " + cursor);
        } else {
            System.out.println("Read cursor from file: " + cursor);
        }

        // Poll
        while (true) {
            LongpollResponse longpollResponse = pollForChanges(client, cursor);
            System.out.println("Response! Changes: " + longpollResponse.changes + " Backoff: " + longpollResponse.backoff);
            if (longpollResponse.changes) {
                cursor = processChanges(client, cursor);
            }
            writeDeltaCursorToFile(cursor);

            try {
                Thread.sleep(longpollResponse.backoff * 1000);
            } catch (InterruptedException ex) {
                throw new RuntimeException("Thread interrupted!", ex);
            }
        }
    }

    private String readDeltaCursorFromFile() {
        if (config.getStateFileName() != null) {
            try {
                Path path = Paths.get(config.getStateFileName());
                if (Files.exists(path))
                    return new String(Files.readAllBytes(Paths.get(config.getStateFileName())));
            } catch (IOException ex) {
                // Should this fail, or just log and continue?
                System.out.println("Read failed");
                ex.printStackTrace();
            }
        }
        return null;
    }

    private void writeDeltaCursorToFile(String cursor) {
        Path path = Paths.get(config.getStateFileName());
        try {
            Files.write(path, cursor.getBytes());
            System.out.println("Wrote cursor to file: " + cursor);
        } catch (IOException e) {
            // Fail? Continue?
            System.out.println("Cursor write failed");
            e.printStackTrace();
        }
    }

    private String getLatestDeltaCursor(DbxClient client) throws DbxException {
        String[] params = {
                "cursor", null,
                "path_prefix", null,
        };

        return DbxRequestUtil.doPost(client.getRequestConfig(), client.getAccessToken(),
                DbxHost.Default.api, "1/delta/latest_cursor", params, null,
                new DbxRequestUtil.ResponseHandler<String>() {
                    @Override
                    public String handle(HttpRequestor.Response response) throws DbxException {
                        if (response.statusCode != 200) throw DbxRequestUtil.unexpectedStatus(response);
                        return DbxRequestUtil.readJsonFromResponse(CursorReader, response.body);
                    }
                });
    }

    private LongpollResponse pollForChanges(DbxClient client, String cursor) throws DbxException {
        String[] params = {
                "cursor", cursor,
                "timeout", "30"
        };

        return DbxRequestUtil.doGet(client.getRequestConfig(), client.getAccessToken(), "api-notify.dropbox.com",
                "1/longpoll_delta", params, null,
                new DbxRequestUtil.ResponseHandler<DropboxDeltaEventSource.LongpollResponse>() {
                    @Override
                    public LongpollResponse handle(HttpRequestor.Response response) throws DbxException {
                        if (response.statusCode != 200) throw DbxRequestUtil.unexpectedStatus(response);
                        return DbxRequestUtil.readJsonFromResponse(LongpollResponse.Reader, response.body);
                    }
                });
    }

    private String processChanges(DbxClient client, String cursor) throws DbxException {
        DbxDelta<DbxEntry> delta = null;
        do {
            delta = client.getDelta(cursor);
            for (DbxDelta.Entry<DbxEntry> e : delta.entries) {
                System.out.println("Changed file: " + e.lcPath);
                if (e.metadata == null)
                    System.out.println("File deleted");
                else
                    System.out.println("File added/changed");
                distributor.distributeEvent(e.lcPath, e.metadata);
            }
            cursor = delta.cursor;
        } while (delta.hasMore);

        return delta.cursor;
    }

    static class LongpollResponse {
        public boolean changes;
        public long backoff;

        public LongpollResponse(boolean changes, long backoff) {
            this.changes = changes;
            this.backoff = backoff;
        }

        public static final JsonReader<LongpollResponse> Reader = new JsonReader<LongpollResponse>() {
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
        };
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
}