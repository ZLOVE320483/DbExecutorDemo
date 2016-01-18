package com.zlove.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Created by ZLOVE on 2015/3/2.
 */
public class FileUtils {

    public static void copy(File srcFile, File destFile) throws IOException {

        final long CHUNK_SIZE = 4 * 1024;

        if (destFile.exists() && destFile.isDirectory()) {
            throw new IOException("Destination '" + destFile + "' exists but is a directory");
        }

        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel input = null;
        FileChannel output = null;
        try {
            fis = new FileInputStream(srcFile);
            fos = new FileOutputStream(destFile);
            input = fis.getChannel();
            output = fos.getChannel();
            long size = input.size();
            long pos = 0;
            long count = 0;
            while (pos < size) {
                count = (size - pos) > CHUNK_SIZE ? CHUNK_SIZE : (size - pos);
                pos += output.transferFrom(input, pos, count);
            }
        } finally {
            if (output != null) {
                output.force(true);
                output.close();
            }
            if (fos != null) {
                fos.flush();
                fos.close();
            }
            if (input != null)
                input.close();
            if (fis != null)
                fis.close();
        }

        if (srcFile.length() != destFile.length()) {
            throw new IOException("Failed to copy full contents from '" + srcFile + "' to '" + destFile + "'");
        }
    }
}
