package io.crnk.operations.internal;

import io.crnk.core.engine.document.Resource;
import io.crnk.core.engine.http.HttpMethod;
import io.crnk.operations.Operation;
import io.crnk.operations.server.order.OrderedOperation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class OperationLidUtils {

	public static Map<String, Set<String>> parseLidsPerType(List<OrderedOperation> operations) {
		Map<String, Set<String>> map = new HashMap<>();
		List<Operation> ops = operations.stream()
				.map(OrderedOperation::getOperation)
				.filter(o -> o.getOp().equalsIgnoreCase(HttpMethod.POST.name()))
				.collect(Collectors.toList());
		ops.forEach(o -> {
			Set<String> orDefault = map.getOrDefault(o.getValue().getType(), new HashSet<>());
			orDefault.add(o.getValue().getId());
			map.put(o.getValue().getType(), orDefault);
		});
		return map;
	}

	public static boolean hasLid(Resource resource, Map<String, Set<String>> lidsPerType) {
		return lidsPerType.containsKey(resource.getType())
				&& lidsPerType.get(resource.getType()).contains(resource.getId());
	}
}
