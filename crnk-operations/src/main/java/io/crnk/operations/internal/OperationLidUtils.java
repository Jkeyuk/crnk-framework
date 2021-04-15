package io.crnk.operations.internal;

import io.crnk.core.engine.document.Relationship;
import io.crnk.core.engine.document.Resource;
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

/**
 * Utility class to support the resolution of local identifiers to internal generated identifiers.
 */
public final class OperationLidUtils {

	private OperationLidUtils() {
		// We cannot create an instance of a utility class
	}

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
					.filter(o -> isPostOperation(o) && isUsingLid(o.getValue()))
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
		if (relationships == null || relationships.size() == 0 || isBlank(trackedLids)) {
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

	/**
	 * Returns true if the given type and local identifier is tracked by a given map.
	 *
	 * @param trackedLids map to check
	 * @param type        type to check is present
	 * @param lid         local id to check is present
	 * @return Returns true if the given type and local identifier is tracked by a given map.
	 */
	private static boolean containsLid(Map<String, Map<String, String>> trackedLids, String type, String lid) {
		if (StringUtils.isBlank(type) || StringUtils.isBlank(lid) || isBlank(trackedLids)) {
			return false;
		}
		return trackedLids.containsKey(type) && trackedLids.get(type).containsKey(lid);
	}

	/**
	 * Returns true if the given collection is null or empty
	 *
	 * @param col collection to check
	 * @return Returns true if the given collection is null or empty
	 */
	private static <T, K> boolean isBlank(Map<T, K> col) {
		return col == null || col.isEmpty();
	}

	/**
	 * Returns true if a given Resource is using a local identifier and not a normal Identifier.
	 *
	 * @param value value to check
	 * @return Returns true if a given Resource is using a local identifier and not a normal Identifier.
	 */
	private static boolean isUsingLid(Resource value) {
		return value != null && StringUtils.isBlank(value.getId()) && !StringUtils.isBlank(value.getLid());
	}

	/**
	 * returns true if a given operation is a POST operation
	 *
	 * @param o operation to check
	 * @return returns true if a given operation is a POST operation
	 */
	private static boolean isPostOperation(io.crnk.operations.Operation o) {
		return o.getOp().equalsIgnoreCase(HttpMethod.POST.name());
	}

}
