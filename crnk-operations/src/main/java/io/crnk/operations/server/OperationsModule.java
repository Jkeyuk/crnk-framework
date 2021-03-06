package io.crnk.operations.server;

import io.crnk.core.engine.dispatcher.RequestDispatcher;
import io.crnk.core.engine.dispatcher.Response;
import io.crnk.core.engine.document.Document;
import io.crnk.core.engine.document.Relationship;
import io.crnk.core.engine.document.Resource;
import io.crnk.core.engine.filter.FilterBehavior;
import io.crnk.core.engine.filter.ResourceFilterDirectory;
import io.crnk.core.engine.http.HttpMethod;
import io.crnk.core.engine.http.HttpStatus;
import io.crnk.core.engine.information.resource.ResourceInformation;
import io.crnk.core.engine.internal.dispatcher.path.JsonPath;
import io.crnk.core.engine.internal.dispatcher.path.PathBuilder;
import io.crnk.core.engine.internal.dispatcher.path.ResourcePath;
import io.crnk.core.engine.internal.http.JsonApiRequestProcessorHelper;
import io.crnk.core.engine.internal.utils.PreconditionUtil;
import io.crnk.core.engine.internal.utils.StringUtils;
import io.crnk.core.engine.parser.TypeParser;
import io.crnk.core.engine.query.QueryContext;
import io.crnk.core.engine.registry.RegistryEntry;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.exception.BadRequestException;
import io.crnk.core.exception.ForbiddenException;
import io.crnk.core.exception.RepositoryNotFoundException;
import io.crnk.core.exception.UnauthorizedException;
import io.crnk.core.module.Module;
import io.crnk.core.module.discovery.ServiceDiscovery;
import io.crnk.core.repository.BulkResourceRepository;
import io.crnk.core.repository.decorate.Wrapper;
import io.crnk.core.utils.Nullable;
import io.crnk.operations.Operation;
import io.crnk.operations.OperationResponse;
import io.crnk.operations.internal.OperationLidUtils;
import io.crnk.operations.internal.OperationParameterUtils;
import io.crnk.operations.server.order.DependencyOrderStrategy;
import io.crnk.operations.server.order.OperationOrderStrategy;
import io.crnk.operations.server.order.OrderedOperation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class OperationsModule implements Module {

    private OperationOrderStrategy orderStrategy = new DependencyOrderStrategy();

    private List<io.crnk.operations.server.OperationFilter> filters = new CopyOnWriteArrayList<>();

    private ModuleContext moduleContext;

    private boolean resumeOnError = false;

    private boolean includeChangedRelationships = true;

    private boolean displayOperationResponseOnSuccess = true;

    private JsonApiRequestProcessorHelper helper;

    public static OperationsModule create() {
        return new OperationsModule();
    }

    // protected for CDI
    protected OperationsModule() {
    }

    public void addFilter(io.crnk.operations.server.OperationFilter filter) {
        this.filters.add(filter);
    }

    public void removeFilter(io.crnk.operations.server.OperationFilter filter) {
        this.filters.remove(filter);
    }

    public OperationOrderStrategy getOrderStrategy() {
        return orderStrategy;
    }

    public void setOrderStrategy(OperationOrderStrategy orderStrategy) {
        this.orderStrategy = orderStrategy;
    }

    public List<io.crnk.operations.server.OperationFilter> getFilters() {
        return filters;
    }

    @Override
    public String getModuleName() {
        return "operations";
    }

    @Override
    public void setupModule(ModuleContext context) {
        this.moduleContext = context;
        this.helper = new JsonApiRequestProcessorHelper(context);
        context.addHttpRequestProcessor(new io.crnk.operations.server.OperationsRequestProcessor(this, context));
    }

    /**
     * Applies the given set of operations.
     *
     * @return responses
     */
    public List<OperationResponse> apply(List<Operation> operations, QueryContext queryContext) {
        checkAccess(operations, queryContext);
        enrichTypeIdInformation(operations, queryContext);

        List<OrderedOperation> orderedOperations = orderStrategy.order(operations);

        DefaultOperationFilterChain chain = new DefaultOperationFilterChain();
        return chain.doFilter(new DefaultOperationFilterContext(orderedOperations, queryContext));
    }

    /**
     * This is not strictly necessary, but allows to catch security issues early before accessing the individual repositories
     */
    private void checkAccess(List<Operation> operations, QueryContext queryContext) {
        for (Operation operation : operations) {
            checkAccess(operation, queryContext);
        }
    }

    private void checkAccess(Operation operation, QueryContext queryContext) {
        HttpMethod httpMethod = HttpMethod.valueOf(operation.getOp());

        String path = OperationParameterUtils.parsePath(operation.getPath());
        PathBuilder pathBuilder = new PathBuilder(moduleContext.getResourceRegistry(), moduleContext.getTypeParser());
        JsonPath jsonPath = pathBuilder.build(path, queryContext);
        if (jsonPath == null) {
            throw new RepositoryNotFoundException(path);
        }

        RegistryEntry entry = jsonPath.getRootEntry();
        ResourceInformation resourceInformation = entry.getResourceInformation();

        ResourceFilterDirectory filterDirectory = moduleContext.getResourceFilterDirectory();
        FilterBehavior filterBehavior = filterDirectory.get(resourceInformation, httpMethod, queryContext);
        if (filterBehavior == FilterBehavior.FORBIDDEN) {
            throw new ForbiddenException(resourceInformation, httpMethod);
        }
        if (filterBehavior == FilterBehavior.UNAUTHORIZED) {
            throw new UnauthorizedException(resourceInformation, httpMethod);
        }
    }

    private void enrichTypeIdInformation(List<Operation> operations, QueryContext queryContext) {
        ResourceRegistry resourceRegistry = moduleContext.getResourceRegistry();
        for (Operation operation : operations) {
            if (Arrays.asList(HttpMethod.DELETE.toString(), HttpMethod.GET.toString()).contains(operation.getOp())) {
                String path = OperationParameterUtils.parsePath(operation.getPath());
                PathBuilder pathBuilder = new PathBuilder(resourceRegistry, moduleContext.getTypeParser());
                JsonPath jsonPath = pathBuilder.build(path, queryContext);

                RegistryEntry entry = jsonPath.getRootEntry();
                String idString = entry.getResourceInformation().toIdString(jsonPath.getId());

                Resource resource = new Resource();
                resource.setType(jsonPath.getRootEntry().getResourceInformation().getResourceType());
                resource.setId(idString);
                operation.setValue(resource);
            }
        }
    }

    protected void fillinIgnoredOperations(OperationResponse[] responses) {
        for (int i = 0; i < responses.length; i++) {
            if (responses[i] == null) {
                OperationResponse operationResponse = new OperationResponse();
                operationResponse.setStatus(HttpStatus.PRECONDITION_FAILED_412);
                responses[i] = operationResponse;
            }
        }
    }


    protected PathBuilder getPathBuilder() {
        ResourceRegistry resourceRegistry = moduleContext.getResourceRegistry();
        TypeParser typeParser = moduleContext.getTypeParser();
        return new PathBuilder(resourceRegistry, typeParser);
    }

    protected List<OperationResponse> executeOperations(List<OrderedOperation> orderedOperations, QueryContext queryContext) {
        OperationResponse[] responses = new OperationResponse[orderedOperations.size()];
        boolean successful = true;

        int index = 0;

        String bulkMethod = null;
        String bulkType = null;
        List<OrderedOperation> bulk = new ArrayList<>();

        PathBuilder pathBuilder = getPathBuilder();

		// We parse the local identifiers for the incoming operations and group them by resource type
		Map<String, Set<String>> lidsPerType = OperationLidUtils.parseLidsPerType(orderedOperations);
		// Used to pair internalized identifiers by their local identifiers and group them by resource type.
		Map<String, Map<String, String>> trackedLids = new HashMap<>();

        while (index < orderedOperations.size()) {
            OrderedOperation orderedOperation = orderedOperations.get(index);
            index++;

            Operation operation = orderedOperation.getOperation();
            String method = operation.getOp();
            String path = OperationParameterUtils.parsePath(operation.getPath());
            JsonPath jsonPath = pathBuilder.build(path, queryContext);
            orderedOperation.setPath(jsonPath);
            if (jsonPath == null) {
                throw new BadRequestException("invalid path: " + operation.getPath());
            }
            String type = jsonPath.getRootEntry().getResourceInformation().getResourceType();

            if (!method.equals(bulkMethod) || !type.equals(bulkType)) {
                if (!bulk.isEmpty()) {
                    boolean success = bulkExecuteOperations(bulk, responses, lidsPerType, trackedLids);
                    if (!success) {
                        successful = false;
                        if (!resumeOnError) {
                            break;
                        }
                    }
                    bulk.clear();
                }
                bulkMethod = method;
                bulkType = type;
            }

            bulk.add(orderedOperation);
        }

        if (!bulk.isEmpty()) {
            boolean success = bulkExecuteOperations(bulk, responses, lidsPerType, trackedLids);
            if (!success) {
                successful = false;
            }
        }

        if (orderedOperations.size() > 1 && ((!successful && resumeOnError) || (successful && displayOperationResponseOnSuccess))) {
            fetchUpToDateResponses(orderedOperations, responses);
        }

        fillinIgnoredOperations(responses);
        return Arrays.asList(responses);
    }

    private String getType(Operation operation) {
        return operation.getValue().getType();
    }

    private boolean legacySetup;

    private boolean bulkExecuteOperations(
			List<OrderedOperation> operations,
			OperationResponse[] responses,
			Map<String, Set<String>> lidsPerType,
			Map<String, Map<String, String>> trackedLids
	) {
        RequestDispatcher requestDispatcher = moduleContext.getRequestDispatcher();

        OrderedOperation firstOperation = operations.get(0);

        RegistryEntry rootEntry = firstOperation.getPath().getRootEntry();

		if (supportsBulk(rootEntry)) {
            String method = firstOperation.getOperation().getOp();

            JsonPath repositoryPath = new ResourcePath(rootEntry, null);

            PreconditionUtil.assertTrue(
                method.equals(HttpMethod.POST.toString()) || method.equals(HttpMethod.DELETE.toString()) ,
                "experimental bulk support is currently limited to POST and DELETE"
            );

            Document requestBody = new Document();
            requestBody.setData(Nullable.of(operations.stream().map(it -> it.getOperation().getValue()).collect(Collectors.toList())));

            Map<String, Set<String>> parameters = new HashMap<>();
            Response response = requestDispatcher.dispatchRequest(repositoryPath.toString(), method, parameters, requestBody);

            boolean success = response.getHttpStatus() < 400;
            boolean hasContent = response.getHttpStatus() != 204;
            for (int i = 0; i < operations.size(); i++) {
                OrderedOperation orderedOperation = operations.get(i);
                OperationResponse operationResponse = new OperationResponse();
                operationResponse.setStatus(response.getHttpStatus());
                if (displayOperationResponseOnSuccess || !success) {
                    if (success && hasContent) {
                        List<Resource> collectionData = response.getDocument().getCollectionData().get();
                        Resource responseResourceBody = collectionData.get(0);
                        operationResponse.setData(Nullable.of(responseResourceBody));
                    }
                    copyDocument(operationResponse, response.getDocument(), false);
                }
                responses[orderedOperation.getOrdinal()] = operationResponse;
            }
            return success;
        } else {
            boolean successful = true;
            for (OrderedOperation orderedOperation : operations) {
                Operation operation = orderedOperation.getOperation();
                String path = OperationParameterUtils.parsePath(operation.getPath());
                Map<String, Set<String>> parameters = OperationParameterUtils.parseParameters(operation.getPath());
                String method = operation.getOp();

                // Resolve local identifiers with tracked internalized identifiers
				OperationLidUtils.resolveLidsForRelations(trackedLids, operation.getValue().getRelationships().values());

                Document requestBody = new Document();
                requestBody.setData(Nullable.of(operation.getValue()));

                Response response = requestDispatcher.dispatchRequest(path, method, parameters, requestBody);
                boolean success = response.getHttpStatus() < 400;

                // Track local identifiers with their associated internal identifiers returned from the response.
				if (success && !StringUtils.isBlank(operation.getValue().getLid())) {
					trackLids(lidsPerType, trackedLids, operation.getValue(), response.getDocument().getData());
				}

				OperationResponse operationResponse = new OperationResponse();
                operationResponse.setStatus(response.getHttpStatus());
                if (displayOperationResponseOnSuccess || !success) {
                    copyDocument(operationResponse, response.getDocument(), true);
                }
                responses[orderedOperation.getOrdinal()] = operationResponse;

                successful = successful && operationResponse.getStatus() < 400;
                if (!successful && !resumeOnError) {
                    return false;
                }
            }
            return successful;
        }

    }

	private static void trackLids(
			Map<String, Set<String>> lidsPerType,
			Map<String, Map<String, String>> trackedLids,
			Resource resource,
			Nullable<Object> responseData
	) {
		if (responseData.isPresent() && responseData.get() instanceof Resource) {
			Resource response = (Resource) responseData.get();
			String internalId = response.getId();
			String type = resource.getType();
			String lid = resource.getLid();

			if (StringUtils.isBlank(internalId) || StringUtils.isBlank(type) || StringUtils.isBlank(lid)) {
				return;
			}

			if (lidsPerType.containsKey(type) && lidsPerType.get(type).contains(lid)) {
				Map<String, String> lidsOrDefault = trackedLids.getOrDefault(type, new HashMap<>());
				lidsOrDefault.put(lid, internalId);
				trackedLids.put(type, lidsOrDefault);
			}
		}
	}

	private boolean supportsBulk(RegistryEntry rootEntry) {
        Object implementation = rootEntry.getResourceRepository().getImplementation();
        boolean supported = implementation instanceof BulkResourceRepository;
        if (!supported) {
            Object wrapped = implementation;
            while (wrapped instanceof Wrapper) {
                wrapped = ((Wrapper) wrapped).getWrappedObject();
                PreconditionUtil.verify(!(wrapped instanceof BulkResourceRepository),
                        "non-bulk wrapper " + implementation + " wraps bulk repository " + wrapped + ", fix wrappers to support bulk operations to avoid performance issues");
            }
        }
        return supported;
    }

    protected void fetchUpToDateResponses(List<OrderedOperation> orderedOperations, OperationResponse[] responses) {
        RequestDispatcher requestDispatcher = moduleContext.getRequestDispatcher();
        ResourceRegistry resourceRegistry = moduleContext.getResourceRegistry();

        // get current set of resources after all the updates have been applied
        for (OrderedOperation orderedOperation : orderedOperations) {
            Operation operation = orderedOperation.getOperation();
            OperationResponse operationResponse = responses[orderedOperation.getOrdinal()];

            boolean isPost = operation.getOp().equalsIgnoreCase(HttpMethod.POST.toString());
            boolean isPatch = operation.getOp().equalsIgnoreCase(HttpMethod.PATCH.toString());
            boolean success = operationResponse.getStatus() < 400;
            if (success && (isPost || isPatch)) {
                Resource resource = operationResponse.getSingleData().get();

                ResourceInformation resourceInformation = resourceRegistry.getBaseResourceInformation(resource.getType());
                String path = resourceRegistry.getResourcePath(resourceInformation, resource.getId());
                String method = HttpMethod.GET.toString();

                Map<String, Set<String>> parameters = new HashMap<>();
                if (includeChangedRelationships) {
                    parameters.put("include", getLoadedRelationshipNames(resource));
                }

                Response response =
                        requestDispatcher.dispatchRequest(path, method, parameters, null);
                copyDocument(operationResponse, response.getDocument(), true);
                operationResponse.setIncluded(null);
            }
        }
    }

    private Set<String> getLoadedRelationshipNames(Resource resourceBody) {
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, Relationship> entry : resourceBody.getRelationships().entrySet()) {
            if (entry.getValue() != null && entry.getValue().getData() != null) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private void copyDocument(OperationResponse operationResponse, Document document, boolean copyData) {
        if (document != null) {
            if (copyData) {
                operationResponse.setData(document.getData());
            }
            operationResponse.setMeta(document.getMeta());
            operationResponse.setLinks(document.getLinks());
            operationResponse.setErrors(document.getErrors());
            operationResponse.setIncluded(document.getIncluded());
        }
    }

    public boolean isResumeOnError() {
        return resumeOnError;
    }

    public void setResumeOnError(boolean resumeOnError) {
        this.resumeOnError = resumeOnError;
    }

    public boolean isIncludeChangedRelationships() {
        return includeChangedRelationships;
    }

    public void setIncludeChangedRelationships(boolean includeChangedRelationships) {
        this.includeChangedRelationships = includeChangedRelationships;
    }

    public boolean isDisplayOperationResponseOnSuccess() {
        return displayOperationResponseOnSuccess;
    }

    public void setDisplayOperationResponseOnSuccess(boolean displayOperationResponseOnSuccess) {
        this.displayOperationResponseOnSuccess = displayOperationResponseOnSuccess;
    }

    protected class DefaultOperationFilterContext implements OperationFilterContext {

        private final QueryContext queryContext;

        private List<OrderedOperation> orderedOperations;

        DefaultOperationFilterContext(List<OrderedOperation> orderedOperations, QueryContext queryContext) {
            this.orderedOperations = orderedOperations;
            this.queryContext = queryContext;
        }

        public List<OrderedOperation> getOrderedOperations() {
            return orderedOperations;
        }

        @Override
        public ServiceDiscovery getServiceDiscovery() {
            return moduleContext.getServiceDiscovery();
        }

        @Override
        public QueryContext getQueryContext() {
            return queryContext;
        }
    }

    protected class DefaultOperationFilterChain implements OperationFilterChain {

        protected int filterIndex = 0;

        @Override
        public List<OperationResponse> doFilter(OperationFilterContext context) {
            List<OperationFilter> filters = getFilters();
            if (filterIndex == filters.size()) {
                return executeOperations(context.getOrderedOperations(), context.getQueryContext());
            } else {
                OperationFilter filter = filters.get(filterIndex);
                filterIndex++;
                return filter.filter(context, this);
            }
        }
    }
}
