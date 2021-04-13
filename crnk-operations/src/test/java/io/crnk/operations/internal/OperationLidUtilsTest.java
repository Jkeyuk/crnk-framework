package io.crnk.operations.internal;

import io.crnk.core.engine.document.Resource;
import io.crnk.core.engine.http.HttpMethod;
import io.crnk.operations.Operation;
import io.crnk.operations.server.order.OrderedOperation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
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

	private static Resource newResource(String expectedType, String expectedLid) {
		Resource resource = new Resource();
		resource.setId(null);
		resource.setType(expectedType);
		resource.setLid(expectedLid);
		return resource;
	}
}