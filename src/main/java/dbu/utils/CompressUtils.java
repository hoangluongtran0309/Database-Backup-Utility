package dbu.utils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

public class CompressUtils {

    public static Path compressGzip(Path input, Path output) throws IOException {
        try (GZIPOutputStream gos = new GZIPOutputStream(Files.newOutputStream(output))) {
            Files.copy(input, gos);
        }
        return output;
    }

    public static Path compressZip(Path input, Path output) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(output))) {
            if (Files.isDirectory(input)) {
                Files.walk(input)
                     .filter(Files::isRegularFile)
                     .forEach(file -> {
                         try {
                             String entryName = input.relativize(file).toString();
                             zos.putNextEntry(new ZipEntry(entryName));
                             Files.copy(file, zos);
                             zos.closeEntry();
                         } catch (IOException e) {
                             throw new UncheckedIOException(e);
                         }
                     });
            } else {
                zos.putNextEntry(new ZipEntry(input.getFileName().toString()));
                Files.copy(input, zos);
                zos.closeEntry();
            }
        }
        return output;
    }

    public static Path compressTarGz(Path input, Path output) throws IOException {
        try (
            OutputStream fos = Files.newOutputStream(output);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(bos);
            TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)
        ) {
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            if (Files.isDirectory(input)) {
                Files.walk(input)
                     .filter(Files::isRegularFile)
                     .forEach(file -> {
                         try {
                             String entryName = input.relativize(file).toString();
                             TarArchiveEntry entry = new TarArchiveEntry(file.toFile(), entryName);
                             taos.putArchiveEntry(entry);
                             Files.copy(file, taos);
                             taos.closeArchiveEntry();
                         } catch (IOException e) {
                             throw new UncheckedIOException(e);
                         }
                     });
            } else {
                TarArchiveEntry entry = new TarArchiveEntry(input.toFile(), input.getFileName().toString());
                taos.putArchiveEntry(entry);
                Files.copy(input, taos);
                taos.closeArchiveEntry();
            }
            taos.finish();
        }
        return output;
    }
}
