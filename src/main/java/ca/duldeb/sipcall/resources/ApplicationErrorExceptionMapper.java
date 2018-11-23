package ca.duldeb.sipcall.resources;

import static org.apache.commons.logging.LogFactory.getLog;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;

import ca.duldeb.sipcall.ApplicationErrorException;

@Provider
public class ApplicationErrorExceptionMapper implements ExceptionMapper<ApplicationErrorException> {
    private static final Log LOGGER = getLog(ApplicationErrorExceptionMapper.class);

    @Override
    public Response toResponse(ApplicationErrorException ex) {
        LOGGER.error("Returning error", ex);
        return Response.status(500)
                .entity(new ApplicationErrorMessage(ex))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}