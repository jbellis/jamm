package org.github.jamm;

/**
 * {@code RuntimeException} thrown when Jamm fail to measure an object.
 */
public class CannotMeasureObjectException extends RuntimeException
{
    private static final long serialVersionUID = -3880720440309336955L;

    public CannotMeasureObjectException(String message)
    {
        super(message);
    }

    public CannotMeasureObjectException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
