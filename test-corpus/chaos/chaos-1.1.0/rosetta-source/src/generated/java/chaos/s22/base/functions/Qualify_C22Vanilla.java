package chaos.s22.base.functions;

import chaos.s22.base.C22Terms;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.IQualifyFunctionExtension;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(Qualify_C22Vanilla.Qualify_C22VanillaDefault.class)
public abstract class Qualify_C22Vanilla implements RosettaFunction,IQualifyFunctionExtension<C22Terms> {

	/**
	* @param c22terms 
	* @return is_product 
	*/
	@Override
	public Boolean evaluate(C22Terms c22terms) {
		Boolean is_product = doEvaluate(c22terms);
		
		return is_product;
	}

	protected abstract Boolean doEvaluate(C22Terms c22terms);

	public static class Qualify_C22VanillaDefault extends Qualify_C22Vanilla {
		@Override
		protected Boolean doEvaluate(C22Terms c22terms) {
			Boolean is_product = null;
			return assignOutput(is_product, c22terms);
		}
		
		protected Boolean assignOutput(Boolean is_product, C22Terms c22terms) {
			is_product = areEqual(MapperS.of(c22terms).<String>map("getKind", c22Terms -> c22Terms.getKind()), MapperS.of("vanilla"), CardinalityOperator.All).andNullSafe(exists(MapperS.of(c22terms).<BigDecimal>map("getNotional", c22Terms -> c22Terms.getNotional()))).get();
			
			return is_product;
		}
	}
		
		@Override
		public String getNamePrefix() {
			return "Qualify";
		}
}
