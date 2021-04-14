package io.crnk.operations.internal;

import com.google.common.collect.ImmutableMap;
import io.crnk.core.engine.document.Relationship;
import io.crnk.core.engine.document.Resource;
import io.crnk.core.engine.document.ResourceIdentifier;
import io.crnk.core.engine.http.HttpMethod;
import io.crnk.core.utils.Nullable;
import io.crnk.operations.Operation;
import io.crnk.operations.server.order.OrderedOperation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

class OperationLidUtilsTest {

	@Test
	void parseLidsPerType_OnValidInput_ReturnsLidsPerType() {
		String expectedType = "person";
		String expectedLid = "1";

		String expectedType2 = "fish";
		String expectedLid2 = "2";
		String expectedLid3 = "3";

		OrderedOperation operation = new OrderedOperation(
				new Operation(HttpMethod.POST.name(), "/person", newResource(expectedType, expectedLid)), 1);
		OrderedOperation operation2 = new OrderedOperation(
				new Operation(HttpMethod.POST.name(), "/fish", newResource(expectedType2, expectedLid2)), 2);
		OrderedOperation operation3 = new OrderedOperation(
				new Operation(HttpMethod.POST.name(), "/fish", newResource(expectedType2, expectedLid3)), 3);

		Map<String, Set<String>> stringSetMap = OperationLidUtils.parseLidsPerType(Arrays.asList(operation, operation2, operation3));

		Assertions.assertTrue(stringSetMap.containsKey(expectedType));
		Assertions.assertTrue(stringSetMap.containsKey(expectedType2));
		Assertions.assertEquals(2, stringSetMap.size());

		Assertions.assertEquals(1, stringSetMap.get(expectedType).size());
		Assertions.assertEquals(2, stringSetMap.get(expectedType2).size());

		Assertions.assertTrue(stringSetMap.get(expectedType).contains(expectedLid));
		Assertions.assertTrue(stringSetMap.get(expectedType2).contains(expectedLid2));
		Assertions.assertTrue(stringSetMap.get(expectedType2).contains(expectedLid3));
	}

	@Test
	void resolveLidsForRelations_OnValidInput_RelationsIdsResolved() {
		String type = "fish";
		String localId = "localId";
		String expectedInternalId = "InternalId";

		Map<String, Map<String, String>> trackedLids = ImmutableMap.of(type, ImmutableMap.of(localId, expectedInternalId));

		ResourceIdentifier identifier = new ResourceIdentifier();
		identifier.setType(type);
		identifier.setLid(localId);

		ResourceIdentifier collectionIdentifier = new ResourceIdentifier();
		collectionIdentifier.setType(type);
		collectionIdentifier.setLid(localId);

		Relationship relationship = new Relationship();
		relationship.setData(Nullable.of(identifier));

		Relationship collectionRelation = new Relationship();
		collectionRelation.setData(Nullable.of(Collections.singletonList(collectionIdentifier)));

		Map<String, Relationship> relData = ImmutableMap.of(
				"fishType", relationship,
				"listOfFishTypes", collectionRelation);

		OperationLidUtils.resolveLidsForRelations(trackedLids, relData);

		Assertions.assertEquals(expectedInternalId, identifier.getId());
		Assertions.assertEquals(expectedInternalId, collectionIdentifier.getId());
	}

	private static Resource newResource(String expectedType, String expectedLid) {
		Resource resource = new Resource();
		resource.setId(null);
		resource.setType(expectedType);
		resource.setLid(expectedLid);
		return resource;
	}
}