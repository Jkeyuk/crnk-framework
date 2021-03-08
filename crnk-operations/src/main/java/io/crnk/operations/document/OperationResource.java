package io.crnk.operations.document;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.crnk.core.engine.document.Resource;

public class OperationResource extends Resource {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String lid;

	public OperationResource() {
		this(null);
	}

	public OperationResource(Resource resource) {
		if (resource == null) {
			return;
		}
		setLinks(resource.getLinks());
		setMeta(resource.getMeta());
		setAttributes(resource.getAttributes());
		setRelationships(resource.getRelationships());
		setId(resource.getId());
		setType(resource.getType());
	}

	public String getLid() {
		return lid;
	}

	public void setLid(String lid) {
		this.lid = lid;
	}
}
