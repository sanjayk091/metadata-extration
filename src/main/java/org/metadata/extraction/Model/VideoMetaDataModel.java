package org.metadata.extraction.Model;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
public class VideoMetaDataModel {
    // General File Information
    private String fileName;
    private long fileSizeBytes;
    private String filePath;
    private String format;
    private long durationSeconds;
    private String creationTime;

    // Video Information
    private boolean hasVideo;
    private int width;
    private int height;
    private double frameRate;
    private String videoCodec;
    private String videoProfile;
    private long videoBitRate;
    private String aspectRatio;
    private String resolutionCategory;
    private String videoBitDepth;

    // Audio Information
    private boolean hasAudio;
    private String audioCodec;
    private int audioChannels;
    private long audioBitRate;
    private int audioSampleRate;
    private String audioMixType;
    private String channelLayout;
    private int audioBitDepth;

    // Timecode Information
    private String timecodeStart;
    private String timecodeEnd;
    private double timecodeRate;

    // Camera and Shooting Information
    private String cameraMake;
    private String cameraModel;
    private String cameraSerialNumber; // New field
    private String cameraSensorType; // New field
    private String cameraLens;
    private double shutterSpeed;
    private double aperture;
    private int iso;
    private int lensFocalLength; // New field (in mm)
    private String lensType; // New field (e.g., prime, zoom)
    private String exposureMode; // New field (e.g., manual, automatic)
    private String whiteBalance; // New field (e.g., Auto, Daylight)
    private String focusMode; // New field (e.g., Auto, Manual)
    private String stabilizationType; // New field (e.g., Optical, Digital)
    private String frameRateMode; // New field (e.g., constant, variable)

    // Color Information
    private String colorSpace;
    private String colorRange;
    private String colorTransfer;
    private String colorDepth;
    private String dynamicRange;

    // Project and Production Metadata
    private String projectName;
    private String director;
    private String editor;
    private String productionCompany;
    private String shootDate;
    private String version;

    // Spatial Information
    private String gpsCoordinates;
    private String location;

    // Subtitle and Language Information
    private String language;
    private String subtitleLanguage;

    // Compression Information
    private String videoCompression;
    private String compressionLevel;
}
