package io.crnk.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.crnk.client.http.HttpAdapter;
import io.crnk.client.http.HttpAdapterRequest;
import io.crnk.client.http.HttpAdapterResponse;
import io.crnk.core.engine.document.Resource;
import io.crnk.core.engine.http.HttpHeaders;
import io.crnk.core.engine.http.HttpMethod;
import io.crnk.core.repository.ResourceRepository;
import io.crnk.operations.model.MovieEntity;
import io.crnk.operations.model.VoteEntity;
import io.crnk.operations.server.OperationsRequestProcessor;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

public class OperationsLocalIdTest extends AbstractOperationsTest {

	private ResourceRepository<MovieEntity, UUID> movieRepo;
	private ResourceRepository<VoteEntity, Long> voteRepo;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		this.movieRepo = client.getRepositoryForType(MovieEntity.class);
		this.voteRepo = client.getRepositoryForType(VoteEntity.class);
	}

	@Test
	public void name() throws IOException {
		String expectedType = "vote";
		String expectedLid = "1";

		Operation operation1 = new Operation(HttpMethod.POST.name(), "/vote", newResource(expectedType, expectedLid));

		HttpAdapter adapter = client.getHttpAdapter();
		String url = client.getServiceUrlProvider().getUrl() + "/operations";
		ObjectMapper mapper = client.getObjectMapper();

		String operationsJson = mapper.writer().writeValueAsString(new Operation[]{operation1});

		HttpAdapterRequest request = adapter.newRequest(url, HttpMethod.PATCH, operationsJson);
		request.header(HttpHeaders.HTTP_CONTENT_TYPE, OperationsRequestProcessor.JSONPATCH_CONTENT_TYPE);
		request.header(HttpHeaders.HTTP_HEADER_ACCEPT, OperationsRequestProcessor.JSONPATCH_CONTENT_TYPE);
		HttpAdapterResponse response = request.execute();
		System.out.println(response.body());
	}

	private static Resource newResource(String expectedType, String expectedLid) {
		Resource resource = new Resource();
		resource.setId(null);
		resource.setType(expectedType);
		resource.setLid(expectedLid);
		return resource;
	}
}
