package de.skyrising.litefabric.impl.fs;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class UrlHandler extends URLStreamHandler {
    private static final UrlHandler INSTANCE = new UrlHandler();

    private UrlHandler() {}

    public static void register() {
        URL.setURLStreamHandlerFactory(protocol -> protocol.equals("litefabric") ? INSTANCE : null);
    }

    @Override
    protected URLConnection openConnection(URL u) {
        return new Connection(u);
    }

    @Override
    protected InetAddress getHostAddress(URL u) {
        return null;
    }

    public static final class Connection extends URLConnection {
        private InputStream stream;

        Connection(URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {
            if (stream != null) return;
            try {
                stream = Files.newInputStream(Paths.get(url.toURI()));
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            connect();
            return stream;
        }
    }
}
