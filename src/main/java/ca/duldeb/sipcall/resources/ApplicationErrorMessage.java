package ca.duldeb.sipcall.resources;

import ca.duldeb.sipcall.ApplicationErrorException;

public class ApplicationErrorMessage {

    private String message;

    public ApplicationErrorMessage(ApplicationErrorException ex) {
        String message = ex.getMessage();
        this.message = message;
        Throwable cause = ex.getCause();
        for (int i = 0; i < 2 && cause != null; i++) {
            if (! message.equals(cause.getMessage())) {
                this.message += " -> " + cause.getMessage();
                message = cause.getMessage();
            }
            cause = cause.getCause();
        }
    }

    public String getMessage() {
        return message;
    }

}
