package org.metadata.extraction.Exception;

import org.springframework.http.HttpStatus;

public class VideoExtractionException extends RuntimeException{
    private HttpStatus httpStatus;

    public VideoExtractionException(String message, HttpStatus httpStatus){
        super(message);
        this.httpStatus=httpStatus;
    }

    public HttpStatus getHttpStatus(){
        return httpStatus;
    }
}
