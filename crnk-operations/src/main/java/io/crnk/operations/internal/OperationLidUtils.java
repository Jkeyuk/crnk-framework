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

	/**
	 * Returns a map where a set of local identifiers are grouped per resource type, parsed from a given list of operations.
	 *
	 * @param operations operations to parse
	 * @return Returns a map where a set of local identifiers are grouped per resource type
	 */
	public static Map<String, Set<String>> parseLidsPerType(List<OrderedOperation> operations) {
		Map<String, Set<String>> map = new HashMap<>();
		if (operations != null && !operations.isEmpty()) {
			operations.stream().map(OrderedOperation::getOperation)
					.filter(o -> isPostOperation(o) && isUsingLid(o))
					.forEach(o -> {
						Set<String> localIdSet = map.getOrDefault(o.getValue().getType(), new HashSet<>());
						localIdSet.add(o.getValue().getLid());
						map.put(o.getValue().getType(), localIdSet);
					});
		}
		return map;
	}

	/**
	 * Resolves internalized identifiers based on the given local Identifiers for the given relationships.
	 *
	 * @param trackedLids   local identifiers grouped by type
	 * @param relationships relations to resolve
	 */
	public static void resolveLidsForRelations(
			Map<String, Map<String, String>> trackedLids,
			Collection<Relationship> relationships
	) {
		if (relationships == null || relationships.size() == 0 || isEmpty(trackedLids)) {
			return;
		}

		relationships.forEach((r) -> {
			Nullable<Object> data = r.getData();
			if (data.isPresent()) {
				if (data.get() instanceof Collection) {
					r.getCollectionData().get().forEach(rId -> resolveLid(trackedLids, rId));
				} else {
					resolveLid(trackedLids, r.getSingleData().get());
				}
			}
		});
	}

	private static void resolveLid(Map<String, Map<String, String>> trackedLids, ResourceIdentifier resourceIdentifier) {
		String lid = resourceIdentifier.getLid();
		String type = resourceIdentifier.getType();

		if (containsLid(trackedLids, type, lid)) {
			resourceIdentifier.setId(trackedLids.get(type).get(lid));
		}
	}

	private static boolean containsLid(Map<String, Map<String, String>> trackedLids, String type, String lid) {
		if (StringUtils.isBlank(type) || StringUtils.isBlank(lid) || isEmpty(trackedLids)) {
			return false;
		}
		return trackedLids.containsKey(type) && trackedLids.get(type).containsKey(lid);
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
