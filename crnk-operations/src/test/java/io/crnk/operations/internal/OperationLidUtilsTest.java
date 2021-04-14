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

		Map<String, Set<String>> result = OperationLidUtils.parseLidsPerType(Arrays.asList(operation, operation2, operation3));

		Assertions.assertTrue(result.containsKey(expectedType));
		Assertions.assertTrue(result.containsKey(expectedType2));
		Assertions.assertEquals(2, result.size());

		Assertions.assertEquals(1, result.get(expectedType).size());
		Assertions.assertEquals(2, result.get(expectedType2).size());

		Assertions.assertTrue(result.get(expectedType).contains(expectedLid));
		Assertions.assertTrue(result.get(expectedType2).contains(expectedLid2));
		Assertions.assertTrue(result.get(expectedType2).contains(expectedLid3));
	}

	@Test
	void resolveLidsForRelations_OnValidInput_RelationsIdsResolved() {
		String type = "fish";
		String localId = "localId";
		String expectedInternalId = "InternalId";

		Map<String, Map<String, String>> trackedLids = ImmutableMap.of(type, ImmutableMap.of(localId, expectedInternalId));

		ResourceIdentifier identifier = newResourceIdentifier(type, localId);
		ResourceIdentifier collectionIdentifier = newResourceIdentifier(type, localId);

		Relationship relationship = newRelationship(Nullable.of(identifier));
		Relationship collectionRelation = newRelationship(Nullable.of(Collections.singletonList(collectionIdentifier)));

		OperationLidUtils.resolveLidsForRelations(trackedLids, Arrays.asList(relationship, collectionRelation));

		Assertions.assertEquals(expectedInternalId, identifier.getId());
		Assertions.assertEquals(expectedInternalId, collectionIdentifier.getId());
	}

	private static Relationship newRelationship(Nullable<Object> data) {
		Relationship relationship = new Relationship();
		relationship.setData(data);
		return relationship;
	}

	private static ResourceIdentifier newResourceIdentifier(String type, String localId) {
		ResourceIdentifier identifier = new ResourceIdentifier();
		identifier.setType(type);
		identifier.setLid(localId);
		return identifier;
	}

	private static Resource newResource(String expectedType, String expectedLid) {
		Resource resource = new Resource();
		resource.setId(null);
		resource.setType(expectedType);
		resource.setLid(expectedLid);
		return resource;
	}
}