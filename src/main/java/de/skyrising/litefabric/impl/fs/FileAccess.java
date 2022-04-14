package de.skyrising.litefabric.impl.fs;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;

public interface FileAccess {
    byte[] getBytes(String path) throws IOException;
    BasicFileAttributes getAttributes(String path) throws IOException;
}
