package dev.turincomplete.quarkuscentralizederrorresponsehandlingexample.globalerrorresponsefilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import org.jboss.resteasy.util.MediaTypeHelper;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Provider
public class ErrorPageResponseFilter implements ContainerResponseFilter {
  // -- Class Fields ------------------------------------------------------------------------------------------------ //

  private static final List<MediaType> ERROR_MEDIA_TYPES        = List.of(MediaType.TEXT_PLAIN_TYPE, MediaType.TEXT_HTML_TYPE, MediaType.APPLICATION_JSON_TYPE);
  private static final MediaType       DEFAULT_ERROR_MEDIA_TYPE = MediaType.TEXT_PLAIN_TYPE;

  // -- Instance Fields --------------------------------------------------------------------------------------------- //

  @Inject
  ObjectMapper objectMapper;

  @Inject
  Template error;

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    int status = responseContext.getStatus();
    if (status >= Response.Status.BAD_REQUEST.getStatusCode()) {
      MediaType errorMediaType = determineErrorContentMediaType(requestContext);
      String errorDetails = Optional.ofNullable(responseContext.getEntity()).map(Object::toString).orElse(null);
      Object errorContent = createErrorContent(errorMediaType, responseContext.getStatusInfo(), errorDetails);

      responseContext.setEntity(errorContent, null, errorMediaType);
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private MediaType determineErrorContentMediaType(ContainerRequestContext containerRequestContext) {
    List<MediaType> acceptableMediaTypes = containerRequestContext.getAcceptableMediaTypes();
    // Both list parameters must be a sortable collection
    MediaType bestMatch = MediaTypeHelper.getBestMatch(new ArrayList<>(ERROR_MEDIA_TYPES), new ArrayList<>(acceptableMediaTypes));
    return bestMatch != null ? bestMatch : DEFAULT_ERROR_MEDIA_TYPE;
  }

  private Object createErrorContent(MediaType errorMediaType, Response.StatusType errorStatus, String errorDetails) {
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

  private ArrayNode createJsonErrorContent(Response.StatusType errorStatus, String errorDetails) {
    ObjectNode errorObject = objectMapper.createObjectNode();
    errorObject.put("status", errorStatus.getStatusCode());
    errorObject.put("title", errorStatus.getReasonPhrase());

    if (errorDetails != null) {
      errorObject.put("detail", errorDetails);
    }

    return objectMapper.createArrayNode().add(errorObject);
  }

  private TemplateInstance createHtmlErrorContent(Response.StatusType errorStatus, String errorDetails) {
    return error.data("errorStatus", errorStatus.getStatusCode())
                .data("errorTitle", errorStatus.getReasonPhrase())
                .data("errorDetails", errorDetails);
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
