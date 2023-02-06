package org.github.jamm;

/**
 * {@code RuntimeException} thrown when Jamm fail to measure an object.
 */
public class CannotMeasureObjectException extends RuntimeException
{
    public CannotMeasureObjectException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
