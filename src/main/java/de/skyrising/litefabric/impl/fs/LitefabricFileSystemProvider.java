package de.skyrising.litefabric.impl.fs;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LitefabricFileSystemProvider extends FileSystemProvider {
    static final LitefabricFileSystemProvider INSTANCE = new LitefabricFileSystemProvider();
    static final Map<String, LitefabricFileSystem> FILE_SYSTEMS = new HashMap<>();

    private LitefabricFileSystemProvider() {}

    public static void install() {
        List<FileSystemProvider> installed = FileSystemProvider.installedProviders();
        if (installed.contains(INSTANCE)) return;
        try {
            // Collections$UnmodifiableRandomAccessList extends Collections$UnmodifiableList
            Field internalList = installed.getClass().getSuperclass().getDeclaredField("list");
            internalList.setAccessible(true);
            //noinspection unchecked
            ((List<FileSystemProvider>) internalList.get(installed)).add(INSTANCE);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static LitefabricFileSystemProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public String getScheme() {
        return "litefabric";
    }

    public FileSystem newFileSystem(String id, FileAccess access) throws IOException {
        synchronized (FILE_SYSTEMS) {
            id = encode(id);
            try {
                new URI(getScheme(), id, "", null);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid file system id: '" + id + "'");
            }
            if (FILE_SYSTEMS.containsKey(id)) throw new FileAlreadyExistsException(id);
            LitefabricFileSystem fs = new LitefabricFileSystem(id, access);
            FILE_SYSTEMS.put(id, fs);
            return fs;
        }
    }

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private static String encode(String id) {
        StringBuilder sb = new StringBuilder();
        byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
        boolean wasLowerCase = false;
        for (byte b : bytes) {
            if (b < 0) {
                sb.append('%');
                sb.append(HEX_CHARS[(b >> 4) & 0xF]);
                sb.append(HEX_CHARS[b & 0xF]);
            } else if (b >= 'A' && b <= 'Z') {
                if (wasLowerCase) sb.append('-');
                sb.append((char) (b + 0x20));
            } else if ((b >= 'a' && b <= 'z') || (b >= '0' && b <= '9')|| b == '-' ||b == '.') {
                sb.append((char) b);
            } else if (b == '_' || b == ' ') {
                sb.append('-');
            }
            wasLowerCase = b >= 'a' && b <= 'z';
        }
        return sb.toString();
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Path getPath(@NotNull URI uri) {
        if (!getScheme().equals(uri.getScheme())) throw new IllegalArgumentException("Invalid scheme");
        LitefabricFileSystem fs = FILE_SYSTEMS.get(uri.getHost());
        if (fs == null) {
            System.out.println("no fs for " + uri.getHost() + ", " + FILE_SYSTEMS);
            throw new FileSystemNotFoundException(uri.getHost());
        }
        return new LitefabricPath(fs, uri.getPath());
    }

    @Override
    public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        for (OpenOption option : options) {
            if (option != StandardOpenOption.READ) throw new UnsupportedOperationException();
        }
        return ((LitefabricPath) path).newInputStream();
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Path path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        for (AccessMode mode : modes) {
            switch (mode) {
                case READ:
                    readAttributes(path, BasicFileAttributes.class);
                    break;
                case WRITE:
                case EXECUTE:
                    throw new AccessDeniedException(path.toString());
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        return ((LitefabricPath) path).readFileAttributes(type, options);
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        System.out.println(path);
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }
}
