package de.skyrising.litefabric.impl.fs;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

class LitefabricPath implements Path {
    private final LitefabricFileSystem fs;
    private final String[] names;
    private final String path;

    LitefabricPath(LitefabricFileSystem fs, String path) {
        this(fs, split(path), path);
    }

    LitefabricPath(LitefabricFileSystem fs, String ...names) {
        this(fs, names, String.join("/", names));
    }

    LitefabricPath(LitefabricFileSystem fs, String[] names, String path) {
        this.fs = fs;
        this.names = names;
        this.path = path;
        checkNames();
    }

    private void checkNames() {
        for (int i = 1; i < names.length - 1; i++) {
            if (names[i].isEmpty()) throw new IllegalArgumentException();
        }
    }

    private static String[] split(String path) {
        List<String> list = new ArrayList<>();
        int i = 0;
        while (i < path.length()) {
            int end = path.indexOf('/', i);
            if (end == -1) end = path.length();
            String part = path.substring(i, end);
            if (i == 0 || !part.isEmpty()) {
                list.add(part);
            }
            i = end + 1;
        }
        return list.toArray(new String[0]);
    }

    @NotNull
    @Override
    public FileSystem getFileSystem() {
        return fs;
    }

    @Override
    public boolean isAbsolute() {
        return names.length > 0 && names[0].equals("");
    }

    @Override
    public Path getRoot() {
        return fs.getRoot();
    }

    @Override
    public Path getFileName() {
        if (names.length == 0) return null;
        return getName(names.length - 1);
    }

    @Override
    public Path getParent() {
        if (names.length == 0) return null;
        return new LitefabricPath(fs, Arrays.copyOf(names, names.length - 1));
    }

    @Override
    public int getNameCount() {
        return names.length;
    }

    @NotNull
    @Override
    public Path getName(int index) {
        String name = names[index];
        return new LitefabricPath(fs, new String[]{name}, name);
    }

    @NotNull
    @Override
    public Path subpath(int beginIndex, int endIndex) {
        if (isAbsolute() && beginIndex == 0) beginIndex = 1;
        if (beginIndex < 0 || beginIndex > endIndex || endIndex > names.length) throw new IllegalArgumentException();
        return new LitefabricPath(fs, Arrays.copyOfRange(names, beginIndex, endIndex));
    }

    @Override
    public boolean startsWith(@NotNull Path other) {
        if (!(other instanceof LitefabricPath)) return false;
        LitefabricPath lfp = (LitefabricPath) other;
        if (lfp.fs != fs) return false;
        if (lfp.names.length > names.length) return false;
        for (int i = 0; i < lfp.names.length; i++) {
            if (!lfp.names[i].equals(names[i])) return false;
        }
        return true;
    }

    @Override
    public boolean startsWith(@NotNull String other) {
        return startsWith(new LitefabricPath(fs, other));
    }

    @Override
    public boolean endsWith(@NotNull Path other) {
        if (!(other instanceof LitefabricPath)) return false;
        LitefabricPath lfp = (LitefabricPath) other;
        if (lfp.fs != fs) return false;
        if (lfp.names.length > names.length) return false;
        for (int i = 0; i < lfp.names.length; i++) {
            if (!lfp.names[i].equals(names[names.length - lfp.names.length + i])) return false;
        }
        return true;
    }

    @Override
    public boolean endsWith(@NotNull String other) {
        return endsWith(new LitefabricPath(fs, other));
    }

    @NotNull
    @Override
    public Path normalize() {
        String[] newNames = new String[names.length];
        int index = 0;
        for (String name : names) {
            if (name.equals(".")) continue;
            if (name.equals("..") && index > 0) {
                index--;
            }
            newNames[index++] = name;
        }
        if (index == names.length) return this;
        return new LitefabricPath(fs, Arrays.copyOf(newNames, index));
    }

    @NotNull
    @Override
    public Path resolve(@NotNull Path other) {
        LitefabricPath lfp = (LitefabricPath) other;
        if (lfp.fs != fs) throw new IllegalArgumentException();
        if (lfp.isAbsolute()) return lfp;
        String[] newNames = new String[names.length + lfp.names.length];
        System.arraycopy(names, 0, newNames, 0, names.length);
        System.arraycopy(lfp.names, 0, newNames, names.length, lfp.names.length);
        return new LitefabricPath(fs, newNames).normalize();
    }

    @NotNull
    @Override
    public Path resolve(@NotNull String other) {
        return resolve(new LitefabricPath(fs, other));
    }

    @NotNull
    @Override
    public Path resolveSibling(@NotNull Path other) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Path resolveSibling(@NotNull String other) {
        return resolveSibling(new LitefabricPath(fs, other));
    }

    @NotNull
    @Override
    public Path relativize(@NotNull Path other) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public URI toUri() {
        return fs.toUri(isAbsolute() ? path : "/" + path);
    }

    @NotNull
    @Override
    public Path toAbsolutePath() {
        if (isAbsolute()) return this;
        String[] names = new String[this.names.length + 1];
        names[0] = "";
        System.arraycopy(this.names, 0, names, 1, this.names.length);
        return new LitefabricPath(fs, names);
    }

    @NotNull
    @Override
    public Path toRealPath(@NotNull LinkOption... options) throws IOException {
        return toAbsolutePath();
    }

    @NotNull
    @Override
    public File toFile() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public WatchKey register(@NotNull WatchService watcher, @NotNull WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public WatchKey register(@NotNull WatchService watcher, @NotNull WatchEvent.Kind<?>... events) throws IOException {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Iterator<Path> iterator() {
        return new Iterator<Path>() {
            int index = isAbsolute() ? 1 : 0;

            @Override
            public boolean hasNext() {
                return index < names.length;
            }

            @Override
            public Path next() {
                return getName(index++);
            }
        };
    }

    @Override
    public int compareTo(@NotNull Path other) {
        LitefabricPath lfp = (LitefabricPath) other;
        if (lfp.fs != fs) return 0;
        for (int i = 0; i < names.length && i < lfp.names.length; i++) {
            int cmp = names[i].compareTo(lfp.names[i]);
            if (cmp != 0) return cmp;
        }
        return names.length - lfp.names.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LitefabricPath paths = (LitefabricPath) o;
        return Objects.equals(fs, paths.fs) && Arrays.equals(names, paths.names);
    }

    @Override
    public int hashCode() {
        return 31 * fs.hashCode() + Arrays.hashCode(names);
    }

    @Override
    public String toString() {
        return path;
    }

    public <A extends BasicFileAttributes> A readFileAttributes(Class<A> type, LinkOption[] options) throws IOException {
        if (type != BasicFileAttributes.class) throw new UnsupportedOperationException();
        //noinspection unchecked
        return (A) fs.getAttributes(isAbsolute() ? path : "/" + path);
    }

    public InputStream newInputStream() throws IOException {
        return fs.newInputStream(isAbsolute() ? path : "/" + path);
    }
}
