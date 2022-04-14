package de.skyrising.litefabric.impl.fs;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Set;

class LitefabricFileSystem extends FileSystem {
    private final String id;
    private final FileAccess access;

    LitefabricFileSystem(String id, FileAccess access) {
        this.id = id;
        this.access = access;
    }

    @Override
    public FileSystemProvider provider() {
        return LitefabricFileSystemProvider.INSTANCE;
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public String getSeparator() {
        return "/";
    }

    Path getRoot() {
        return new LitefabricPath(this, new String[]{""}, "/");
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singleton(getRoot());
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Path getPath(@NotNull String first, @NotNull String... more) {
        StringBuilder path = new StringBuilder();
        if (!first.startsWith("/")) path.append('/');
        path.append(first);
        for (String part : more) {
            path.append('/').append(part);
        }
        return new LitefabricPath(this, path.toString());
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException();
    }

    URI toUri(String path) {
        return URI.create(provider().getScheme() + "://" + id + path);
    }

    BasicFileAttributes getAttributes(String path) throws IOException {
        return access.getAttributes(path);
    }

    public InputStream newInputStream(String path) throws IOException {
        try {
            return new ByteArrayInputStream(access.getBytes(path));
        } catch (NoSuchFileException e) {
            throw new NoSuchFileException(provider().getScheme() + "://" + id + path);
        }
    }
}
