package ca.duldeb.sipcall;

public class ApplicationErrorException extends Exception {

    private static final long serialVersionUID = 2157229902301100266L;

    public ApplicationErrorException(String message) {
        super(message);
    }

    public ApplicationErrorException(String message, Throwable cause) {
        super(message, cause);
    }

}
