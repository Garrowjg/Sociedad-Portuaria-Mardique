package com.example.MardiqueWeb.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ExcelToPdfService {

    private static final Logger log = LoggerFactory.getLogger(ExcelToPdfService.class);

    private static final String LO_BIN = resolveLibreOfficeBin();
    private static final int TIMEOUT_SECONDS = 60;

    public byte[] convertExcelToPdf(byte[] excelBytes, String fileName) throws IOException {
        Path tempDir = Files.createTempDirectory("lo_conv_" + UUID.randomUUID());

        try {
            String ext = getExtension(fileName);
            Path inputFile = tempDir.resolve("documento" + ext);
            Files.write(inputFile, excelBytes);

            ProcessBuilder pb = new ProcessBuilder(
                    LO_BIN,
                    "--headless",
                    "--norestore",
                    "--nofirststartwizard",
                    "--convert-to", "pdf",
                    "--outdir", tempDir.toString(),
                    inputFile.toAbsolutePath().toString()
            );
            pb.environment().put("HOME", tempDir.toString());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("LibreOffice tardó más de " + TIMEOUT_SECONDS + "s");
            }

            int exitCode = process.exitValue();
            log.debug("LibreOffice output: {}", output);

            if (exitCode != 0) {
                throw new IOException("LibreOffice terminó con código " + exitCode + ": " + output);
            }

            String baseName = inputFile.getFileName().toString().replaceAll("\\.[^.]+$", "");
            Path pdfPath = tempDir.resolve(baseName + ".pdf");

            if (!Files.exists(pdfPath)) {
                pdfPath = Files.list(tempDir)
                        .filter(p -> p.toString().endsWith(".pdf"))
                        .findFirst()
                        .orElseThrow(() -> new IOException("LibreOffice no generó ningún PDF. Output: " + output));
            }

            return Files.readAllBytes(pdfPath);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Conversión interrumpida", e);
        } finally {
            deleteDir(tempDir);
        }
    }

    private static String resolveLibreOfficeBin() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            String[] candidates = {
                    "C:\\Program Files\\LibreOffice\\program\\soffice.exe",
                    "C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe"
            };
            for (String path : candidates) {
                if (new File(path).exists()) return path;
            }
            return "soffice.exe";
        } else {
            String[] candidates = {
                    "/usr/bin/libreoffice",
                    "/usr/bin/soffice",
                    "/opt/libreoffice/program/soffice",
                    "/Applications/LibreOffice.app/Contents/MacOS/soffice"
            };
            for (String path : candidates) {
                if (new File(path).exists()) return path;
            }
            return "libreoffice";
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null) return ".xlsx";
        int dot = fileName.lastIndexOf('.');
        return (dot >= 0) ? fileName.substring(dot) : ".xlsx";
    }

    private void deleteDir(Path dir) {
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.warn("No se pudo limpiar directorio temporal: {}", dir, e);
        }
    }
}
