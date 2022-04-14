package de.skyrising.litefabric.impl.url.litefabric;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class Handler extends URLStreamHandler {
    static {
        StringBuilder pkgs = new StringBuilder(System.getProperty("java.protocol.handler.pkgs", ""));
        if (pkgs.length() != 0) pkgs.append('|');
        pkgs.append("de.skyrising.litefabric.impl.url");
        System.setProperty("java.protocol.handler.pkgs", pkgs.toString());
    }

    public static void register(String id, Function<String, byte[]> handler) {
        try {
            Field handlers = ClassLoader.getSystemClassLoader().loadClass(Connection.class.getName()).getField("HANDLERS");
            //noinspection unchecked
            ((Map<String, Function<String, byte[]>>) handlers.get(null)).put(id, handler);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
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
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        public static final Map<String, Function<String, byte[]>> HANDLERS = new HashMap<>();
        private InputStream stream;

        Connection(URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {
            if (stream != null) return;
            Function<String, byte[]> handler = HANDLERS.get(url.getHost());
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
