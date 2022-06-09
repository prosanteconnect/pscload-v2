/*
 * Copyright A.N.S 2021
 */
package fr.ans.psc.pscload.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fr.ans.psc.pscload.model.MapsHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class MaintenanceController.
 */
@Slf4j
@RestController
public class MaintenanceController {

    @Value("${files.directory}")
    private String filesDirectory;

    /**
     * Generate ser file.
     *
     * @param restoreFile the restore file
     * @return the response entity
     */
    @PostMapping(value = "/maintenance/regen-ser-file")
    public ResponseEntity<Void> generateSerFile(@RequestParam MultipartFile restoreFile) {

        log.info("generating new ser file");
        try {
            InputStream initialStream = restoreFile.getInputStream();
            byte[] buffer = new byte[initialStream.available()];
            initialStream.read(buffer);

            File origin = File.createTempFile(filesDirectory + File.separator + "restore_extract_RASS", "tmp");
            try (OutputStream out = new FileOutputStream(origin)) {
                out.write(buffer);
            }

            MapsHandler mapsToSerialize = new MapsHandler();
            mapsToSerialize.loadMapsFromFile(origin);
            mapsToSerialize.serializeMaps(filesDirectory + File.separator + "maps.ser");

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (IOException e) {
            log.error("Error while generating serialized file");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Generate txt file.
     *
     * @param serFile the ser file
     * @return the response entity
     */
    @PostMapping(value = "/maintenance/from-ser-to-txt")
    public ResponseEntity<String> generateTxtFile(@RequestParam MultipartFile serFile) {
        InputStream initialStream = null;
        try {
            initialStream = serFile.getInputStream();
            byte[] buffer = new byte[initialStream.available()];
            initialStream.read(buffer);
            File origin = File.createTempFile(filesDirectory + File.separator + "origin_ser_file", "");
            try (OutputStream out = new FileOutputStream(origin)) {
                out.write(buffer);
            }
            initialStream.close();

            MapsHandler mapsToDeserialize = new MapsHandler();
            mapsToDeserialize.deserializeMaps(origin.getAbsolutePath());
            File txtFile = mapsToDeserialize.generateTxtFile(filesDirectory + File.separator + "generated_txt_file");

            InputStream fileContent = new FileInputStream(txtFile);

            ZipEntry zipEntry = new ZipEntry("generated.txt");
            zipEntry.setTime(System.currentTimeMillis());
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(filesDirectory + "/" + "generated.zip"));
            zos.putNextEntry(zipEntry);
            StreamUtils.copy(fileContent, zos);

            fileContent.close();
            zos.closeEntry();
            zos.finish();
            zos.closeEntry();
            boolean deleted = txtFile.delete();
            assert deleted;

            return new ResponseEntity<>("new txt file generated", HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>("IO Exception", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>("error during deserialization", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "maintenance/get-new-txt")
    public ResponseEntity<FileSystemResource> getGeneratedTxtFile() {
        File zipFile = new File(filesDirectory + "/" + "generated.zip");
        FileSystemResource resource = new FileSystemResource(zipFile);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=generated.zip");
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, "application/zip");
        responseHeaders.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(zipFile.length()));

        return new ResponseEntity<>(resource, responseHeaders, HttpStatus.OK);
    }


}
