package de.skyrising.litefabric.impl.fs;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class UrlHandler extends URLStreamHandler {
    private static final UrlHandler INSTANCE = new UrlHandler();
    static {
        URL.setURLStreamHandlerFactory(protocol -> protocol.equals("litefabric") ? INSTANCE : null);
    }
    private final Map<String, Function<String, byte[]>> handlers = new HashMap<>();

    private UrlHandler() {}

    public static UrlHandler getInstance() {
        return INSTANCE;
    }

    public static void register(String id, Function<String, byte[]> handler) {
        INSTANCE.handlers.put(id, handler);
    }

    @Override
    protected URLConnection openConnection(URL u) {
        return new Connection(u);
    }

    @Override
    protected InetAddress getHostAddress(URL u) {
        return null;
    }

    public final class Connection extends URLConnection {
        private InputStream stream;

        Connection(URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {
            if (stream != null) return;
            Function<String, byte[]> handler = handlers.get(url.getHost());
            if (handler == null) throw new UnknownHostException(url.getHost());
            byte[] data = handler.apply(url.getPath().substring(1));
            if (data == null) throw new FileNotFoundException(url.getHost() + "/" + url.getPath());
            stream = new ByteArrayInputStream(data);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            connect();
            return stream;
        }
    }
}
