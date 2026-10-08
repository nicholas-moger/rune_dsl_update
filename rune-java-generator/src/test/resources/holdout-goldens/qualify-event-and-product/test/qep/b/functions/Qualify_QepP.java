package test.qep.b.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.IQualifyFunctionExtension;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import test.qep.b.QepProduct;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(Qualify_QepP.Qualify_QepPDefault.class)
public abstract class Qualify_QepP implements RosettaFunction,IQualifyFunctionExtension<QepProduct> {

	/**
	* @param terms 
	* @return is_product 
	*/
	@Override
	public Boolean evaluate(QepProduct terms) {
		Boolean is_product = doEvaluate(terms);
		
		return is_product;
	}

	protected abstract Boolean doEvaluate(QepProduct terms);

	public static class Qualify_QepPDefault extends Qualify_QepP {
		@Override
		protected Boolean doEvaluate(QepProduct terms) {
			Boolean is_product = null;
			return assignOutput(is_product, terms);
		}
		
		protected Boolean assignOutput(Boolean is_product, QepProduct terms) {
			is_product = areEqual(MapperS.of(terms).<String>map("getKind", qepProduct -> qepProduct.getKind()), MapperS.of("vanilla"), CardinalityOperator.All).get();
			
			return is_product;
		}
	}
		
		@Override
		public String getNamePrefix() {
			return "Qualify";
		}
}
