package org.odata4j.producer.resources;

import org.odata4j.core.Guid;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.OEntity;
import org.odata4j.producer.ODataContext;
import org.odata4j.producer.ODataContextImpl;
import org.odata4j.producer.ODataProducer;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Providers;
import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

@Path("{first: \\$}batch")
public class BatchRequestResource extends BaseResource {

  private static final Logger log = Logger.getLogger(BatchRequestResource.class.getName());

  @POST
  @Consumes(ODataBatchProvider.MULTIPART_MIXED)
  @Produces(ODataConstants.APPLICATION_ATOM_XML_CHARSET_UTF8)
  public Response processBatch(
      @Context Providers providers,
      @Context HttpHeaders headers,
      @Context Request request,
      @Context SecurityContext securityContext,
      @QueryParam("$format") String format,
      @QueryParam("$callback") String callback,
      List<BatchBodyPart> bodyParts) throws Exception {

    LoggingUtils.log(log, "processBatch", "bodyParts.size", bodyParts.size());

    String batchBoundary = "batchresponse_" + Guid.randomGuid().toString();
    StringBuilder batchResponse = new StringBuilder();

    ODataProducer producer = getODataProducer(providers);

    ODataContext odataContext = ODataContextImpl.builder()
        .aspect(headers)
        .aspect(securityContext)
        .aspect(producer)
        .build();

    for (BatchBodyPart bodyPart : bodyParts) {
      HttpHeaders httpHeaders = bodyPart.getHttpHeaders();
      URI requestUri = new URI(bodyPart.getUri());
      UriInfo uriInfo = new BatchUriInfo(requestUri, bodyPart.getUriInfo());
      String entitySetName = bodyPart.getEntitySetName();
      String entityId = bodyPart.getEntityKey();
      String entityString = bodyPart.getEntity();
      Response response = null;

      switch (bodyPart.getHttpMethod()) {
      case POST:
        OEntity entity = getRequestEntity(httpHeaders, uriInfo, entityString, producer.getMetadata(), entitySetName, null);
        response = new EntitiesRequestResource().createEntity(httpHeaders, uriInfo, securityContext, producer,
                entitySetName, entity, odataContext);
        break;
      case PUT:
        response = new EntityRequestResource()  .updateEntity(httpHeaders, uriInfo, securityContext, providers,
                entitySetName, entityId, entityString, odataContext);
        break;
      case MERGE:
        response = new EntityRequestResource().mergeEntity(httpHeaders, uriInfo, providers, securityContext,
                entitySetName, entityId, entityString);
        break;
      case DELETE:
        response = new EntityRequestResource().deleteEntity(httpHeaders, uriInfo, providers, securityContext,
                format, callback, entitySetName, entityId);
        break;
      case GET:
        final MultivaluedMap<String, String> parameters = uriInfo.getQueryParameters();
        response = new EntitiesRequestResource()
                .getEntities(httpHeaders, uriInfo, providers, securityContext, entitySetName,
                        parameters.getFirst("$inlinecount"),
                        parameters.getFirst("$top"),
                        parameters.getFirst("$skip"),
                        parameters.getFirst("$filter"),
                        parameters.getFirst("$orderby"),
                        format,
                        callback,
                        parameters.getFirst("$skiptoken"),
                        parameters.getFirst("$expand"),
                        parameters.getFirst("$select"));
      }

      batchResponse.append("\n--").append(batchBoundary).append("\n");
      batchResponse.append(ODataConstants.Headers.CONTENT_TYPE).append(": application/http\n");
      batchResponse.append("Content-Transfer-Encoding: binary\n");

      batchResponse.append(ODataBatchProvider.createResponseBodyPart(bodyPart, response));
      batchResponse.append("--").append(batchBoundary).append("--\n");
    }

    return Response
            .status(Status.ACCEPTED)
            .type(ODataBatchProvider.MULTIPART_MIXED + ";boundary=" + batchBoundary)
            .header(ODataConstants.Headers.DATA_SERVICE_VERSION,
                    ODataConstants.DATA_SERVICE_VERSION_HEADER)
            .entity(batchResponse.toString()).build();
  }
}