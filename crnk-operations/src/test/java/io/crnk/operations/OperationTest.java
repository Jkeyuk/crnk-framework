package io.crnk.operations;

import io.crnk.operations.document.OperationResource;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Assert;
import org.junit.Test;

public class OperationTest {


	@Test
	public void testEquals() {
		EqualsVerifier.forClass(Operation.class).usingGetClass().suppress(Warning.NONFINAL_FIELDS).verify();

	}

	@Test
	public void testHashCode() {
		Operation op1 = new Operation("a", "b", new OperationResource());
		Operation op2 = new Operation("a", "b", new OperationResource());
		Operation op3 = new Operation("x", "b", new OperationResource());
		Assert.assertEquals(op1, op2);
		Assert.assertNotEquals(op3.hashCode(), op2.hashCode());
	}

}
