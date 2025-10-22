package dbu.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public class DecompressUtils {

    public static Path decompressIfNeeded(Path input) throws IOException {
        String name = input.toString().toLowerCase();
        if (name.endsWith(".tar.gz")) {
            return decompressTarGz(input);
        } else if (name.endsWith(".gz")) {
            return decompressGzip(input);
        } else if (name.endsWith(".zip")) {
            return decompressZip(input);
        } else {
            return input; 
        }
    }

    private static Path decompressGzip(Path input) throws IOException {
        Path output = stripExtension(input, ".gz");

        try (GZIPInputStream gis = new GZIPInputStream(Files.newInputStream(input));
                OutputStream os = Files.newOutputStream(output)) {
            gis.transferTo(os);
        }
        return output;
    }

    private static Path decompressZip(Path input) throws IOException {
        Path outputDir = input.getParent();
        Path extractedFile = null;

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(input))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    extractedFile = outputDir.resolve(entry.getName());
                    try (OutputStream os = Files.newOutputStream(extractedFile)) {
                        zis.transferTo(os);
                    }
                    break; 
                }
            }
        }

        if (extractedFile == null) {
            throw new IOException("No file found inside zip: " + input);
        }
        return extractedFile;
    }

    private static Path decompressTarGz(Path input) throws IOException {
        Path outputDir = input.getParent();
        Path extractedFile = null;

        try (InputStream fi = Files.newInputStream(input);
                BufferedInputStream bi = new BufferedInputStream(fi);
                GzipCompressorInputStream gzi = new GzipCompressorInputStream(bi);
                TarArchiveInputStream tis = new TarArchiveInputStream(gzi)) {

            TarArchiveEntry entry;
            while ((entry = tis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    extractedFile = outputDir.resolve(entry.getName());
                    try (OutputStream os = Files.newOutputStream(extractedFile)) {
                        tis.transferTo(os);
                    }
                    break; 
                }
            }
        }

        if (extractedFile == null) {
            throw new IOException("No file found inside tar.gz: " + input);
        }
        return extractedFile;
    }

    private static Path stripExtension(Path input, String extension) {
        String name = input.getFileName().toString();
        if (name.endsWith(extension)) {
            name = name.substring(0, name.length() - extension.length());
        }
        return input.getParent().resolve(name);
    }
}
