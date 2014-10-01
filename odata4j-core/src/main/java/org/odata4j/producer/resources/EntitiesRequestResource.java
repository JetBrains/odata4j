package org.odata4j.producer.resources;

import org.odata4j.core.*;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.exceptions.NotFoundException;
import org.odata4j.exceptions.UnsupportedMediaTypeException;
import org.odata4j.format.FormatWriter;
import org.odata4j.format.FormatWriterFactory;
import org.odata4j.internal.InternalUtil;
import org.odata4j.producer.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Providers;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// ignoreParens below is there to trim the parentheses from the entity set name when they are present - e.g. '/my.svc/Users()'.
@Path("{entitySetName: [^/()]+?}{ignoreParens: (?:\\(\\))?}")
public class EntitiesRequestResource extends BaseResource {

  private static final Logger log = Logger.getLogger(EntitiesRequestResource.class.getName());
  private static final String POSSIBLE_FUNCTION_NAME_SUFFIX = "()";

  @POST
  @Produces({ ODataConstants.APPLICATION_ATOM_XML_CHARSET_UTF8, ODataConstants.TEXT_JAVASCRIPT_CHARSET_UTF8, ODataConstants.APPLICATION_JAVASCRIPT_CHARSET_UTF8 })
  public Response createEntity(
      @Context HttpHeaders httpHeaders,
      @Context UriInfo uriInfo,
      @Context Providers providers,
      @Context SecurityContext securityContext,
      @QueryParam("$format") String format,
      @QueryParam("$callback") String callback,
      @PathParam("entitySetName") String entitySetName,
      InputStream payload) throws Exception {

    // visual studio will send a soap mex request
    if (entitySetName.equals("mex") && httpHeaders.getMediaType() != null && httpHeaders.getMediaType().toString().startsWith("application/soap+xml"))
      throw new UnsupportedMediaTypeException("SOAP mex requests are not supported");

    log("createEntity", "entitySetName", entitySetName);

    ODataProducer producer = getODataProducer(providers);

    // the OData URI scheme makes it impossible to have unique @Paths that refer
    // to functions and entity sets
    if (producer.getMetadata().findEdmFunctionImport(entitySetName) != null) {
      // functions that return collections of entities should support the
      // same set of query options as entity set queries so give them everything.

      ODataHttpMethod callingMethod = ODataHttpMethod.POST;
      List<String> xheader = httpHeaders.getRequestHeader("X-HTTP-METHOD");
      if (xheader != null && xheader.size() > 0) {
        callingMethod = ODataHttpMethod.fromString(xheader.get(0));
      }

      QueryInfo query = QueryInfo.newBuilder().setCustomOptions(OptionsQueryParser.parseCustomOptions(uriInfo)).build();
      return FunctionResource.callFunction(callingMethod, httpHeaders, uriInfo, securityContext, producer, entitySetName, format, callback, query);
    }

    // is this a new media resource?
    // check for HasStream
    EdmEntitySet entitySet = producer.getMetadata().findEdmEntitySet(entitySetName);
    if (entitySet == null) {
      throw new NotFoundException();
    }

    ODataContext odataContext = ODataContextImpl.builder()
        .aspect(httpHeaders)
        .aspect(securityContext)
        .aspect(producer)
        .aspect(entitySet)
        .aspect(uriInfo)
        .build();

    if (Boolean.TRUE.equals(entitySet.getType().getHasStream())) { // getHasStream can return null
      // yes it is!
      return createMediaLinkEntry(httpHeaders, uriInfo, securityContext, producer, entitySet, payload, odataContext);
    }

    // also on the plus side we can now parse the stream directly off the wire....
    return createEntity(httpHeaders, uriInfo, securityContext, producer, entitySetName,
            this.getRequestEntity(httpHeaders, uriInfo, payload, producer.getMetadata(), entitySetName, null), odataContext);
  }

  protected Response createMediaLinkEntry(
          HttpHeaders httpHeaders,
          UriInfo uriInfo,
          SecurityContext securityContext,
          ODataProducer producer,
          EdmEntitySet entitySet,
          InputStream payload,
          ODataContext odataContext) throws Exception {

    log("createMediaLinkEntity", "entitySetName", entitySet.getName());

    OEntity mle = super.createOrUpdateMediaLinkEntry(httpHeaders, uriInfo, entitySet, producer, payload, null, odataContext);

    // return the mle
    return createEntity(httpHeaders,
            uriInfo,
            securityContext,
            producer,
            entitySet.getName(),
            mle,
            odataContext);
  }
  
  protected Response createEntity(
      HttpHeaders httpHeaders,
      UriInfo uriInfo,
      SecurityContext securityContext,
      ODataProducer producer,
      String entitySetName,
      OEntity entity,
      ODataContext odataContext) throws Exception {

    EntityResponse response = producer.createEntity(odataContext, entitySetName, entity);

    FormatWriter<EntityResponse> writer = FormatWriterFactory
        .getFormatWriter(EntityResponse.class, httpHeaders.getAcceptableMediaTypes(), null, null);
    StringWriter sw = new StringWriter();
    writer.write(uriInfo, sw, response);

    String relid = InternalUtil.getEntityRelId(response.getEntity());
    String entryId = uriInfo.getBaseUri().toString() + relid;

    String responseEntity = sw.toString();

    return Response
        .ok(responseEntity, writer.getContentType())
        .status(Status.CREATED)
        .location(URI.create(entryId))
        .header(ODataConstants.Headers.DATA_SERVICE_VERSION,
            ODataConstants.DATA_SERVICE_VERSION_HEADER).build();
  }

  @GET
  @Produces({ ODataConstants.APPLICATION_ATOM_XML_CHARSET_UTF8,
      ODataConstants.TEXT_JAVASCRIPT_CHARSET_UTF8,
      ODataConstants.TEXT_PLAIN_CHARSET_UTF8,
      ODataConstants.APPLICATION_JAVASCRIPT_CHARSET_UTF8 })
  public Response getEntities(
      @Context HttpHeaders httpHeaders,
      @Context UriInfo uriInfo,
      @Context Providers providers,
      @Context SecurityContext securityContext,
      @QueryParam("$format") String format,
      @QueryParam("$callback") String callback,
      @PathParam("entitySetName") String functionName,
      InputStream payload) throws Exception {

    Response response;
    log("functionCallDelete", "function", functionName);

    ODataProducer producer = getODataProducer(providers);

    // the OData URI scheme makes it impossible to have unique @Paths that refer
    // to functions and entity sets
    if (producer.getMetadata().findEdmFunctionImport(functionName) != null) {
      // functions that return collections of entities should support the
      // same set of query options as entity set queries so give them everything.

      QueryInfo query = QueryInfo.newBuilder().setCustomOptions(OptionsQueryParser.parseCustomOptions(uriInfo)).build();
      response = FunctionResource.callFunction(ODataHttpMethod.PUT, httpHeaders, uriInfo, securityContext, producer, functionName, format, callback, query);
    } else {
      throw new NotFoundException(functionName);
    }

    return response;
  }

  @DELETE
  public Response functionCallDelete(
      @Context HttpHeaders httpHeaders,
      @Context UriInfo uriInfo,
      @Context Providers providers,
      @Context SecurityContext securityContext,
      @QueryParam("$format") String format,
      @QueryParam("$callback") String callback,
      @PathParam("entitySetName") String functionName,
      InputStream payload) throws Exception {

    Response response;
    log("functionCallDelete", "function", functionName);

    ODataProducer producer = getODataProducer(providers);

    // the OData URI scheme makes it impossible to have unique @Paths that refer
    // to functions and entity sets
    if (producer.getMetadata().findEdmFunctionImport(functionName) != null) {
      // functions that return collections of entities should support the
      // same set of query options as entity set queries so give them everything.

      QueryInfo query = QueryInfo.newBuilder().setCustomOptions(OptionsQueryParser.parseCustomOptions(uriInfo)).build();
      response = FunctionResource.callFunction(ODataHttpMethod.DELETE, httpHeaders, uriInfo, securityContext, producer, functionName, format, callback, query);
    } else {
      throw new NotFoundException(functionName);
    }

    return response;
  }

  @GET
  @Produces({ ODataConstants.APPLICATION_ATOM_XML_CHARSET_UTF8,
      ODataConstants.TEXT_JAVASCRIPT_CHARSET_UTF8,
      ODataConstants.APPLICATION_JAVASCRIPT_CHARSET_UTF8 })
  public Response getEntities(
      @Context HttpHeaders httpHeaders,
      @Context UriInfo uriInfo,
      @Context Providers providers,
      @Context SecurityContext securityContext,
      @PathParam("entitySetName") String entitySetName,
      @QueryParam("$inlinecount") String inlineCount,
      @QueryParam("$top") String top,
      @QueryParam("$skip") String skip,
      @QueryParam("$filter") String filter,
      @QueryParam("$orderby") String orderBy,
      @QueryParam("$format") String format,
      @QueryParam("$callback") String callback,
      @QueryParam("$skiptoken") String skipToken,
      @QueryParam("$expand") String expand,
      @QueryParam("$select") String select)
      throws Exception {

    log("getEntities",
        "entitySetName", entitySetName,
        "inlineCount", inlineCount,
        "top", top,
        "skip", skip,
        "filter", filter,
        "orderBy", orderBy,
        "format", format,
        "callback", callback,
        "skipToken", skipToken,
        "expand", expand,
        "select", select);

    ODataProducer producer = getODataProducer(providers);

    return getEntitiesImpl(httpHeaders, uriInfo, securityContext, producer, entitySetName, false, inlineCount, top, skip,
        filter, orderBy, format, callback, skipToken, expand, select);
  }

  @GET
  @Path("{count: [$]count}")
  @Produces({ ODataConstants.APPLICATION_ATOM_XML_CHARSET_UTF8,
      ODataConstants.TEXT_JAVASCRIPT_CHARSET_UTF8,
      ODataConstants.TEXT_PLAIN_CHARSET_UTF8,
      ODataConstants.APPLICATION_JAVASCRIPT_CHARSET_UTF8 })
  public Response getEntitiesCount(
      @Context HttpHeaders httpHeaders,
      @Context UriInfo uriInfo,
      @Context Providers providers,
      @Context SecurityContext securityContext,
      @PathParam("entitySetName") String entitySetName,
      @QueryParam("$inlinecount") String inlineCount,
      @QueryParam("$top") String top,
      @QueryParam("$skip") String skip,
      @QueryParam("$filter") String filter,
      @QueryParam("$orderby") String orderBy,
      @QueryParam("$format") String format,
      @QueryParam("$callback") String callback,
      @QueryParam("$skiptoken") String skipToken,
      @QueryParam("$expand") String expand,
      @QueryParam("$select") String select) throws Exception {

    log("getEntitiesCount",
        "entitySetName", entitySetName,
        "inlineCount", inlineCount,
        "top", top,
        "skip", skip,
        "filter", filter,
        "orderBy", orderBy,
        "format", format,
        "callback", callback,
        "skipToken", skipToken,
        "expand", expand,
        "select", select);

    ODataProducer producer = getODataProducer(providers);

    return getEntitiesImpl(httpHeaders, uriInfo, securityContext, producer, entitySetName, true, inlineCount, top, skip,
        filter, orderBy, format, callback, skipToken, expand, select);
  }

  protected Response getEntitiesImpl(
      HttpHeaders httpHeaders,
      UriInfo uriInfo,
      SecurityContext securityContext,
      ODataProducer producer,
      String entitySetName,
      boolean isCount,
      String inlineCount,
      String top,
      String skip,
      String filter,
      String orderBy,
      String format,
      String callback,
      String skipToken,
      String expand,
      String select) throws Exception {

    // the OData URI scheme makes it impossible to have unique @Paths that refer
    // to functions and entity sets
    final EdmDataServices metadata = producer.getMetadata();
    String functionName = null;
    if(metadata.findEdmFunctionImport(entitySetName) != null)
      functionName = entitySetName;
    else if(entitySetName.endsWith(POSSIBLE_FUNCTION_NAME_SUFFIX)){
      final String normalisedFunctionImportName = entitySetName.substring(0, entitySetName.length() - 2);
      if(metadata.findEdmFunctionImport(normalisedFunctionImportName) != null)
        functionName = normalisedFunctionImportName;
    }
    if(functionName != null){
      final QueryInfo queryInfo = QueryInfo.newBuilder().setCustomOptions(OptionsQueryParser.parseCustomOptions(uriInfo)).build();
      return FunctionResource.callFunction(ODataHttpMethod.GET, httpHeaders, uriInfo, securityContext, producer, functionName, format, callback, queryInfo);
    }

    QueryInfo query = new QueryInfo(
        OptionsQueryParser.parseInlineCount(inlineCount),
        OptionsQueryParser.parseTop(top),
        OptionsQueryParser.parseSkip(skip),
        OptionsQueryParser.parseFilter(filter),
        OptionsQueryParser.parseOrderBy(orderBy),
        OptionsQueryParser.parseSkipToken(skipToken),
        OptionsQueryParser.parseCustomOptions(uriInfo),
        OptionsQueryParser.parseExpand(expand),
        OptionsQueryParser.parseSelect(select));

    ODataContextImpl odataContext = ODataContextImpl.builder()
        .aspect(httpHeaders)
        .aspect(uriInfo)
        .aspect(securityContext)
        .aspect(producer)
        .build();

    // the OData URI scheme makes it impossible to have unique @Paths that refer
    // to functions and entity sets
    if (producer.getMetadata().findEdmFunctionImport(entitySetName) != null) {
      // functions that return collections of entities should support the
      // same set of query options as entity set queries so give them everything.
      return FunctionResource.callFunction(ODataHttpMethod.GET, httpHeaders, uriInfo, securityContext, producer, entitySetName, format, callback, query);
    }

    Response response = null;
    if (isCount) {
      CountResponse countResponse = producer.getEntitiesCount(odataContext, entitySetName, query);

      String entity = Long.toString(countResponse.getCount());

      // TODO remove this hack, check whether we are Version 2.0 compatible anyway
      ODataVersion version = ODataVersion.V2;

      response = Response
          .ok(entity, ODataConstants.TEXT_PLAIN_CHARSET_UTF8)
          .header(ODataConstants.Headers.DATA_SERVICE_VERSION, version.asString)
          .build();
    }
    else {
      EntitiesResponse entitiesResponse = producer.getEntities(odataContext, entitySetName, query);

      if (entitiesResponse == null) {
        throw new NotFoundException(entitySetName);
      }

      StringWriter sw = new StringWriter();
      FormatWriter<EntitiesResponse> fw =
          FormatWriterFactory.getFormatWriter(
              EntitiesResponse.class,
              httpHeaders.getAcceptableMediaTypes(),
              format,
              callback);

      fw.write(uriInfo, sw, entitiesResponse);
      String entity = sw.toString();

      // TODO remove this hack, check whether we are Version 2.0 compatible anyway
      ODataVersion version = MediaType.valueOf(fw.getContentType()).isCompatible(MediaType.APPLICATION_JSON_TYPE)
          ? ODataVersion.V2 : ODataVersion.V2;

      response = Response
          .ok(entity, fw.getContentType())
          .header(ODataConstants.Headers.DATA_SERVICE_VERSION, version.asString)
          .build();
    }
    return response;
  }

  @POST
  @Path("{batch: [$]batch}")
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

    log("processBatch", "bodyParts.size", bodyParts.size());

    EntityRequestResource er = new EntityRequestResource();

    String changesetBoundary = "changesetresponse_"
        + Guid.randomGuid().toString();
    String batchBoundary = "batchresponse_" + Guid.randomGuid().toString();
    StringBuilder batchResponse = new StringBuilder("\n--");
    batchResponse.append(batchBoundary);

    batchResponse
        .append("\n").append(ODataConstants.Headers.CONTENT_TYPE).append(": multipart/mixed; boundary=")
        .append(changesetBoundary);

    batchResponse.append('\n');

    ODataProducer producer = getODataProducer(providers);

    ODataContext odataContext = ODataContextImpl.builder()
        .aspect(headers)
        .aspect(securityContext)
        .aspect(producer)
        .build();

    for (BatchBodyPart bodyPart : bodyParts) {
      HttpHeaders httpHeaders = bodyPart.getHttpHeaders();
      UriInfo uriInfo = bodyPart.getUriInfo();
      String entitySetName = bodyPart.getEntitySetName();
      String entityId = bodyPart.getEntityKey();
      String entityString = bodyPart.getEntity();
      Response response = null;

      switch (bodyPart.getHttpMethod()) {
      case POST:
        response = this.createEntity(httpHeaders, uriInfo, securityContext, producer,
            entitySetName,
            getRequestEntity(httpHeaders, uriInfo, entityString, producer.getMetadata(), entitySetName, null), odataContext);
        break;
      case PUT:
        response = er.updateEntity(httpHeaders, uriInfo, securityContext, providers,
            entitySetName, entityId, entityString, odataContext);
        break;
      case MERGE:
        response = er.mergeEntity(httpHeaders, uriInfo, providers, securityContext, entitySetName,
            entityId, entityString);
        break;
      case DELETE:
        response = er.deleteEntity(httpHeaders, uriInfo, providers, securityContext, format, callback, entitySetName, entityId);
        break;
      case GET:
        throw new UnsupportedOperationException("Not supported yet.");
      }

      batchResponse.append("\n--").append(changesetBoundary);
      batchResponse.append("\n").append(ODataConstants.Headers.CONTENT_TYPE).append(": application/http");
      batchResponse.append("\nContent-Transfer-Encoding: binary\n");

      batchResponse.append(ODataBatchProvider.createResponseBodyPart(
          bodyPart,
          response));
    }

    batchResponse.append("--").append(changesetBoundary).append("--\n");
    batchResponse.append("--").append(batchBoundary).append("--\n");

    return Response
        .status(Status.ACCEPTED)
        .type(ODataBatchProvider.MULTIPART_MIXED + ";boundary="
            + batchBoundary).header(
            ODataConstants.Headers.DATA_SERVICE_VERSION,
            ODataConstants.DATA_SERVICE_VERSION_HEADER)
        .entity(batchResponse.toString()).build();
  }

  private static void log(String operation, Object... namedArgs) {
    if (!log.isLoggable(Level.FINE))
      return;
    StringBuilder sb = new StringBuilder(operation).append('(');
    if (namedArgs != null && namedArgs.length > 0) {
      for (int i = 0; i < namedArgs.length; i += 2) {
        if (i > 0)
          sb.append(',');
        sb.append(namedArgs[i]).append('=').append(namedArgs[i + 1]);
      }
    }
    log.fine(sb.append(')').toString());
  }

}