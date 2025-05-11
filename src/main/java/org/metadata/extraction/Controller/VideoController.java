package org.metadata.extraction.Controller;

import org.metadata.extraction.Exception.VideoExtractionException;
import org.metadata.extraction.Model.VideoMetaDataModel;
import org.metadata.extraction.Service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    @Autowired
    private VideoService videoService;
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoMetaDataModel> uploadVideo(@RequestParam("file") MultipartFile file) {
        System.out.println("Uploaded file size: " + file.getSize() / (1024 * 1024) + " MB");

        File tempFile = null;
        try {
            // Create a temporary file for storing the uploaded file
            tempFile = Files.createTempFile("upload_", "_" + file.getOriginalFilename()).toFile();
            file.transferTo(tempFile);

            // Extract metadata and save it to file
            VideoMetaDataModel metadata = videoService.extractMetadata(tempFile);
            videoService.saveMetadataToFile(metadata);

            return ResponseEntity.ok(metadata);
        } catch (Exception e) {

            // Return a detailed error message in the response
            String errorMessage = "An error occurred while processing the video file. Please try again later.";
            throw new VideoExtractionException(errorMessage, HttpStatus.BAD_REQUEST);
        } finally {
            // Ensure the temporary file is deleted after processing, even in case of an exception
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

}
