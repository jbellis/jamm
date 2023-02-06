package org.github.jamm;

/**
 * {@code RuntimeException} thrown when Jamm cannot access successfully one of the fields from an object of the measured graph.
 */
public class CannotAccessFieldException extends RuntimeException
{
    public CannotAccessFieldException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
