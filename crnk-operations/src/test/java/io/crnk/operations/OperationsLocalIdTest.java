package io.crnk.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.crnk.client.http.HttpAdapter;
import io.crnk.client.http.HttpAdapterRequest;
import io.crnk.core.engine.document.Relationship;
import io.crnk.core.engine.document.Resource;
import io.crnk.core.engine.document.ResourceIdentifier;
import io.crnk.core.engine.http.HttpHeaders;
import io.crnk.core.engine.http.HttpMethod;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ResourceRepository;
import io.crnk.operations.model.MovieEntity;
import io.crnk.operations.server.OperationsRequestProcessor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

public class OperationsLocalIdTest extends AbstractOperationsTest {

	private ResourceRepository<MovieEntity, UUID> movieRepo;
	private ObjectMapper mapper;
	private HttpAdapter adapter;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		this.movieRepo = client.getRepositoryForType(MovieEntity.class);
		this.mapper = client.getObjectMapper();
		this.adapter = client.getHttpAdapter();
	}

	@Test
	public void linkRelation_WhenInternalIdGenerated_LocalIdEstablishesRelation() throws IOException {
		String voteType = "vote";
		String localId = "99";

		ResourceIdentifier voteResourceId = new ResourceIdentifier();
		voteResourceId.setLid(localId);
		voteResourceId.setType(voteType);

		UUID uuid = UUID.randomUUID();
		Resource movie = newMovieResource(uuid);
		movie.setRelationships(ImmutableMap.of(voteType, new Relationship(voteResourceId)));

		// Post a vote with a given local ID, the vote has an internal id generated from the database
		Operation postVoteOperation = new Operation(HttpMethod.POST.name(), "/vote", newResource(voteType, localId));
		// A movie is posted with a vote entity that has a generated id from the database. The local id will resolve the internal id.
		Operation postMovieWithVote = new Operation(HttpMethod.POST.name(), "/movie", movie);

		String operationsJson = mapper.writer().writeValueAsString(new Operation[]{postVoteOperation, postMovieWithVote});
		sendOperationRequest(operationsJson);

		MovieEntity resultMovie = movieRepo.findOne(uuid, newMovieQuerySpec(voteType));
		Assert.assertNotNull(resultMovie.getVote());
	}

	private Resource newMovieResource(UUID uuid) throws IOException {
		Resource movie = newResource("movie", null);
		movie.setId(uuid.toString());
		movie.setAttribute("title", mapper.readTree("\"new title\""));
		return movie;
	}

	private static Resource newResource(String expectedType, String expectedLid) {
		Resource resource = new Resource();
		resource.setId(null);
		resource.setType(expectedType);
		resource.setLid(expectedLid);
		return resource;
	}

	private static QuerySpec newMovieQuerySpec(String voteType) {
		QuerySpec querySpec = new QuerySpec(MovieEntity.class);
		querySpec.includeRelation(PathSpec.of(voteType));
		return querySpec;
	}

	private void sendOperationRequest(String operationsJson) throws IOException {
		HttpAdapterRequest request = adapter.newRequest(
				client.getServiceUrlProvider().getUrl() + "/operations",
				HttpMethod.PATCH,
				operationsJson);
		request.header(HttpHeaders.HTTP_CONTENT_TYPE, OperationsRequestProcessor.JSONPATCH_CONTENT_TYPE);
		request.header(HttpHeaders.HTTP_HEADER_ACCEPT, OperationsRequestProcessor.JSONPATCH_CONTENT_TYPE);
		request.execute();
	}
}
