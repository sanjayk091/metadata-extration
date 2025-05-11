package org.metadata.extraction.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.metadata.extraction.Exception.VideoExtractionException;
import org.metadata.extraction.Model.VideoMetaDataModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

@Service
public class VideoService {

    @Value("${video.ffprobe-path}")
    private String ffprobePath;

    private final ObjectMapper mapper = new ObjectMapper();

    public VideoMetaDataModel extractMetadata(File videoFile) {
        // Check if file exists.
        if (!videoFile.exists()) {
            throw new VideoExtractionException("Video file not found: " + videoFile.getAbsolutePath(), HttpStatus.NOT_FOUND);
        }

        // Build command using ProcessBuilder and command list for better handling of file paths with spaces
        List<String> command = Arrays.asList(
                ffprobePath,
                "-v", "quiet",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                videoFile.getAbsolutePath()
        );

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            StringBuilder outputBuilder = new StringBuilder();

            // Read the output from ffprobe
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line);
                }
            }

            String output = outputBuilder.toString();
            return parseMetadata(output, videoFile);
        } catch (IOException e) {
            throw new VideoExtractionException("Error during video metadata extraction: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private VideoMetaDataModel parseMetadata(String output, File videoFile) {
        try {
            JsonNode root = mapper.readTree(output);
            JsonNode format = root.path("format");

            VideoMetaDataModel metadata = new VideoMetaDataModel();

            // General File Information
            metadata.setFileName(videoFile.getName());
            metadata.setFileSizeBytes(videoFile.length());
            metadata.setFilePath(videoFile.getAbsolutePath());
            metadata.setFormat(format.path("format_name").asText(""));
            metadata.setDurationSeconds((long) format.path("duration").asDouble(0));
            metadata.setCreationTime(format.path("tags").path("creation_time").asText(""));

            for (JsonNode stream : root.path("streams")) {
                String type = stream.path("codec_type").asText("");

                if ("video".equals(type)) {
                    metadata.setHasVideo(true);
                    metadata.setWidth(stream.path("width").asInt());
                    metadata.setHeight(stream.path("height").asInt());
                    metadata.setFrameRate(parseRational(stream.path("r_frame_rate").asText()));
                    metadata.setVideoCodec(stream.path("codec_name").asText(""));
                    metadata.setVideoProfile(stream.path("profile").asText(""));
                    metadata.setVideoBitRate(stream.path("bit_rate").asLong(0));
                    metadata.setAspectRatio(getAspectRatio(metadata.getWidth(), metadata.getHeight()));
                    metadata.setResolutionCategory(getResolutionCategory(metadata.getHeight()));
                    metadata.setVideoBitDepth(stream.path("bits_per_raw_sample").asText(""));

                    // Camera and Shooting
                    JsonNode tags = stream.path("tags");
                    metadata.setCameraLens(tags.path("lens").asText(""));
                    metadata.setShutterSpeed(parseRational(tags.path("shutter_speed").asText("")));
                    metadata.setAperture(parseRational(tags.path("aperture").asText("")));
                    metadata.setIso(tags.path("iso").asInt(0));
                    metadata.setLensFocalLength(tags.path("focal_length").asInt(0));
                    metadata.setLensType(tags.path("lens_type").asText(""));
                    metadata.setExposureMode(tags.path("exposure_mode").asText(""));
                    metadata.setWhiteBalance(tags.path("white_balance").asText(""));
                    metadata.setFocusMode(tags.path("focus_mode").asText(""));
                    metadata.setStabilizationType(tags.path("stabilization_type").asText(""));
                    metadata.setFrameRateMode(tags.path("frame_rate_mode").asText(""));

                    // Color Information
                    metadata.setColorSpace(stream.path("color_space").asText(""));
                    metadata.setColorRange(stream.path("color_range").asText(""));
                    metadata.setColorTransfer(stream.path("color_transfer").asText(""));
                    metadata.setColorDepth(stream.path("bits_per_raw_sample").asText(""));
                    metadata.setDynamicRange(tags.path("dynamic_range").asText(""));
                }

                if ("audio".equals(type)) {
                    metadata.setHasAudio(true);
                    metadata.setAudioCodec(stream.path("codec_name").asText(""));
                    metadata.setAudioChannels(stream.path("channels").asInt(0));
                    metadata.setAudioBitRate(stream.path("bit_rate").asLong(0));
                    metadata.setAudioSampleRate(stream.path("sample_rate").asInt(0));
                    metadata.setAudioMixType(stream.path("tags").path("mix_type").asText(""));
                    metadata.setChannelLayout(stream.path("channel_layout").asText(""));
                    metadata.setAudioBitDepth(stream.path("bits_per_sample").asInt(0));
                }

                // Timecode Info (if available in any stream)
                if (stream.has("timecode")) {
                    metadata.setTimecodeStart(stream.path("timecode").asText(""));
                }
            }

            // Timecode end/rate (derived or assumed)
            metadata.setTimecodeEnd(""); // Optional: set logic
            metadata.setTimecodeRate(metadata.getFrameRate()); // Simplified assumption

            // Camera General
            metadata.setCameraMake(format.path("tags").path("make").asText(""));
            metadata.setCameraModel(format.path("tags").path("model").asText(""));
            metadata.setCameraSerialNumber(format.path("tags").path("serial_number").asText(""));
            metadata.setCameraSensorType(format.path("tags").path("sensor_type").asText(""));

            // Project & Production
            metadata.setProjectName(format.path("tags").path("project_name").asText(""));
            metadata.setDirector(format.path("tags").path("director").asText(""));
            metadata.setEditor(format.path("tags").path("editor").asText(""));
            metadata.setProductionCompany(format.path("tags").path("production_company").asText(""));
            metadata.setShootDate(format.path("tags").path("shoot_date").asText(""));
            metadata.setVersion(format.path("tags").path("version").asText(""));

            // Spatial Info
            metadata.setGpsCoordinates(format.path("tags").path("location").asText(""));
            metadata.setLocation(format.path("tags").path("location_description").asText(""));

            // Subtitle & Language
            metadata.setLanguage(format.path("tags").path("language").asText(""));
            metadata.setSubtitleLanguage(format.path("tags").path("subtitle_language").asText(""));

            // Compression
            metadata.setVideoCompression(format.path("tags").path("video_compression").asText(""));
            metadata.setCompressionLevel(format.path("tags").path("compression_level").asText(""));

            return metadata;

        } catch (IOException e) {
            throw new VideoExtractionException("Failed to parse metadata: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }



    private double parseRational(String rational) {
        if (rational == null || rational.trim().isEmpty()) {
            return 0.0;
        }
        try {
            if (rational.contains("/")) {
                String[] parts = rational.split("/");
                if (parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                    return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
                }
            }
            return Double.parseDouble(rational);
        } catch (NumberFormatException e) {
            return 0.0; // default fallback
        }
    }


    private String getAspectRatio(int width, int height) {
        double ratio = (double) width / height;
        if (Math.abs(ratio - 16.0 / 9) < 0.1) return "16:9";
        if (Math.abs(ratio - 4.0 / 3) < 0.1) return "4:3";
        return String.format("%.2f:1", ratio);
    }

    private String getResolutionCategory(int height) {
        if (height >= 2160) return "4K";
        if (height >= 1440) return "2K";
        if (height >= 1080) return "1080p";
        if (height >= 720) return "720p";
        if (height >= 480) return "480p";
        return "SD";
    }

    public void saveMetadataToFile(VideoMetaDataModel metadata) {
        String baseName = metadata.getFileName().replaceAll("[^a-zA-Z0-9-_\\.]", "_");

        // Ensure the base name has no file extension
        if (baseName.contains(".")) {
            baseName = baseName.substring(0, baseName.lastIndexOf('.'));
        }

        // Locate the resources directory
        File resourceDir = new File("src/main/resources/metadata");
        if (!resourceDir.exists() && !resourceDir.mkdirs()) {
            throw new VideoExtractionException("Failed to create resource directory.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        File outputFile = new File(resourceDir, baseName + ".json");

        try {
            // Write metadata to file
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, metadata);
        } catch (IOException e) {
            throw new VideoExtractionException("Failed to save metadata file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
