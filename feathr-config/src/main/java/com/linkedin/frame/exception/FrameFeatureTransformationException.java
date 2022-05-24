package com.linkedin.frame.exception;

/**
  * This exception is thrown when something wrong happened during feature transformation.
  */
public class FrameFeatureTransformationException extends FrameException {

  public FrameFeatureTransformationException(ErrorLabel errorLabel, String msg, Throwable cause) {
    super(errorLabel, msg, cause);
  }

  public FrameFeatureTransformationException(ErrorLabel errorLabel, String msg) {
    super(errorLabel, msg);
  }
}