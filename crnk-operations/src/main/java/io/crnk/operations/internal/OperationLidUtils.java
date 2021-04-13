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
	 * @param localIdPerType   local identifiers grouped by type
	 * @param relationships    relations to resolve
	 * @param InternalIdPerLid internal identifiers grouped by their associated local Identifier
	 */
	public static void resolveLidsForRelations(
			Map<String, Set<String>> localIdPerType,
			Map<String, Relationship> relationships,
			Map<String, String> InternalIdPerLid
	) {
		if (isEmpty(localIdPerType) || isEmpty(relationships) || isEmpty(InternalIdPerLid)) {
			return;
		}

		relationships.forEach((f, r) -> {
			Nullable<Object> data = r.getData();
			if (data.isPresent()) {
				if (data.get() instanceof Collection) {
					r.getCollectionData().get().forEach(rId -> resolveLid(localIdPerType, InternalIdPerLid, rId));
				} else {
					resolveLid(localIdPerType, InternalIdPerLid, r.getSingleData().get());
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

	private static boolean containsLid(Map<String, Set<String>> lidsPerType, String type, String lid) {
		if (StringUtils.isBlank(type) || StringUtils.isBlank(lid) || isEmpty(lidsPerType)) {
			return false;
		}
		return lidsPerType.containsKey(type) && lidsPerType.get(type).contains(lid);
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
