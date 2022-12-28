package dev.turincomplete.quarkuscentralizederrorresponsehandlingexample.globalexceptionmapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.qute.Template;
import io.quarkus.resteasy.runtime.standalone.VertxUtil;
import io.vertx.core.http.HttpServerRequest;
import org.jboss.logging.Logger;
import org.jboss.resteasy.util.MediaTypeHelper;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

@Provider
public class ErrorPageResponseExceptionMapper implements ExceptionMapper<Exception> {
  // -- Class Fields ------------------------------------------------------------------------------------------------ //

  private static final List<MediaType> ERROR_MEDIA_TYPES        = List.of(MediaType.TEXT_PLAIN_TYPE, MediaType.TEXT_HTML_TYPE, MediaType.APPLICATION_JSON_TYPE);
  private static final MediaType       DEFAULT_ERROR_MEDIA_TYPE = MediaType.TEXT_PLAIN_TYPE;

  // -- Instance Fields --------------------------------------------------------------------------------------------- //

  @Inject
  ObjectMapper objectMapper;

  @Inject
  Template error;

  @Inject
  Logger logger;

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  @Inject
  javax.inject.Provider<HttpServerRequest> httpServerRequestProvider;

  @Override
  public Response toResponse(Exception exception) {
    Response errorResponse = mapExceptionToResponse(exception);
    List<MediaType> acceptableMediaTypes = VertxUtil.extractAccepts(VertxUtil.extractRequestHeaders(httpServerRequestProvider.get()));
    MediaType errorMediaType = determineErrorContentMediaType(acceptableMediaTypes);
    String errorContent = createErrorContent(errorMediaType, errorResponse.getStatusInfo(), errorResponse.getEntity().toString());

    return Response.fromResponse(errorResponse)
                   .type(errorMediaType)
                   .entity(errorContent)
                   .build();
  }


  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private Response mapExceptionToResponse(Exception exception) {
    // Use response from WebApplicationException as they are
    if (exception instanceof WebApplicationException) {
      // Overwrite error message
      Response originalErrorResponse = ((WebApplicationException) exception).getResponse();
      return Response.fromResponse(originalErrorResponse)
                     .entity(exception.getMessage())
                     .build();
    }
    // Special mappings
    else if (exception instanceof IllegalArgumentException) {
      return Response.status(400).entity(exception.getMessage()).build();
    }
    // Use 500 (Internal Server Error) for all other
    else {
      logger.fatalf(exception,
                    "Failed to process request to: {}",
                    httpServerRequestProvider.get().absoluteURI());
      return Response.serverError().entity("Internal Server Error").build();
    }
  }

  private MediaType determineErrorContentMediaType(List<MediaType> acceptableMediaTypes) {
    // Both list parameters must be a sortable collection
    MediaType bestMatch = MediaTypeHelper.getBestMatch(new ArrayList<>(ERROR_MEDIA_TYPES), new ArrayList<>(acceptableMediaTypes));
    return bestMatch != null ? bestMatch : DEFAULT_ERROR_MEDIA_TYPE;
  }

  private String createErrorContent(MediaType errorMediaType, Response.StatusType errorStatus, String errorDetails) {
    // as JSON
    if (errorMediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
      return createJsonErrorContent(errorStatus, errorDetails);
    }
    // as HTML
    else if (errorMediaType.equals(MediaType.TEXT_HTML_TYPE)) {
      return createHtmlErrorContent(errorStatus, errorDetails);
    }
    // as text; also the fallback case
    else {
      return createTextErrorContent(errorStatus, errorDetails);
    }
  }

  private String createJsonErrorContent(Response.StatusType errorStatus, String errorDetails) {
    ObjectNode errorObject = objectMapper.createObjectNode();
    errorObject.put("status", errorStatus.getStatusCode());
    errorObject.put("title", errorStatus.getReasonPhrase());

    if (errorDetails != null) {
      errorObject.put("detail", errorDetails);
    }

    ArrayNode errorsArray = objectMapper.createArrayNode().add(errorObject);

    try {
      return objectMapper.writeValueAsString(errorsArray);
    }
    catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String createHtmlErrorContent(Response.StatusType errorStatus, String errorDetails) {
    return error.data("errorStatus", errorStatus.getStatusCode())
                .data("errorTitle", errorStatus.getReasonPhrase())
                .data("errorDetails", errorDetails)
                .render();
  }

  private static String createTextErrorContent(Response.StatusType errorStatus, String errorDetails) {
    var errorText = new StringBuilder();
    errorText.append("Error ")
             .append(errorStatus.getStatusCode())
             .append(" (").append(errorStatus.getReasonPhrase()).append(")");

    if (errorDetails != null) {
      errorText.append("\n\n").append(errorDetails);
    }

    return errorText.toString();
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
