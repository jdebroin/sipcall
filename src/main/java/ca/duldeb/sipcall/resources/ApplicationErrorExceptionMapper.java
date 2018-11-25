package ca.duldeb.sipcall.resources;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.duldeb.sipcall.ApplicationErrorException;

@Provider
public class ApplicationErrorExceptionMapper implements ExceptionMapper<ApplicationErrorException> {
    private final Logger LOGGER = LoggerFactory.getLogger(ApplicationErrorExceptionMapper.class);

    @Override
    public Response toResponse(ApplicationErrorException ex) {
        LOGGER.error("Returning error", ex);
        return Response.status(500)
                .entity(new ApplicationErrorMessage(ex))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}