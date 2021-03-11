package io.crnk.operations.internal;

import io.crnk.core.engine.document.Relationship;
import io.crnk.core.engine.document.ResourceIdentifier;
import io.crnk.core.engine.http.HttpMethod;
import io.crnk.core.engine.internal.utils.StringUtils;
import io.crnk.core.utils.Nullable;
import io.crnk.operations.server.order.OrderedOperation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OperationLidUtils {

	public static Map<String, Set<String>> parseLidsPerType(List<OrderedOperation> operations) {
		Map<String, Set<String>> map = new HashMap<>();
		if (operations != null && !operations.isEmpty()) {
			operations.stream()
					.map(OrderedOperation::getOperation)
					.filter(o -> isPostOperation(o) && isUsingLid(o))
					.forEach(o -> {
						Set<String> orDefault = map.getOrDefault(o.getValue().getType(), new HashSet<>());
						orDefault.add(o.getValue().getLid());
						map.put(o.getValue().getType(), orDefault);
					});
		}
		return map;
	}

	public static boolean containsLid(Map<String, Set<String>> lidsPerType, String type, String lid) {
		if (StringUtils.isBlank(type) || StringUtils.isBlank(lid) || isEmpty(lidsPerType)) {
			return false;
		}
		return lidsPerType.containsKey(type) && lidsPerType.get(type).contains(lid);
	}

	public static void resolveLidsForRelations(
			Map<String, Set<String>> lidsPerType,
			Map<String, Relationship> relationships,
			Map<String, String> lidPerId
	) {
		if (isEmpty(lidsPerType) || isEmpty(relationships) || isEmpty(lidPerId)) {
			return;
		}

		relationships.forEach((f, r) -> {
			Nullable<Object> data = r.getData();
			if (data.isPresent()) {
				if (data.get() instanceof Collection) {
					r.getCollectionData().get().forEach(rId -> resolveLid(lidsPerType, lidPerId, rId));
				} else {
					resolveLid(lidsPerType, lidPerId, r.getSingleData().get());
				}
			}
		});
	}

	private static void resolveLid(
			Map<String, Set<String>> lidsPerType,
			Map<String, String> lidPerId,
			ResourceIdentifier resourceIdentifier
	) {
		if (containsLid(lidsPerType, resourceIdentifier.getType(), resourceIdentifier.getLid())) {
			resourceIdentifier.setId(lidPerId.get(resourceIdentifier.getLid()));
		}
	}

	private static <T, K> boolean isEmpty(Map<T, K> col) {
		return col == null || col.isEmpty();
	}

	private static boolean isUsingLid(io.crnk.operations.Operation o) {
		return o.getValue() != null && StringUtils.isBlank(o.getValue().getId()) && !StringUtils.isBlank(o.getValue().getLid());
	}

	private static boolean isPostOperation(io.crnk.operations.Operation o) {
		return o.getOp().equalsIgnoreCase(HttpMethod.POST.name());
	}

}
