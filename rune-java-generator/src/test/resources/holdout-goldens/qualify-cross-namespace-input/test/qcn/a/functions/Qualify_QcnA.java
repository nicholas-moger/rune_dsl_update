package test.qcn.a.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.IQualifyFunctionExtension;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import test.qcn.a.QcnTerms;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(Qualify_QcnA.Qualify_QcnADefault.class)
public abstract class Qualify_QcnA implements RosettaFunction,IQualifyFunctionExtension<QcnTerms> {

	/**
	* @param terms 
	* @return is_product 
	*/
	@Override
	public Boolean evaluate(QcnTerms terms) {
		Boolean is_product = doEvaluate(terms);
		
		return is_product;
	}

	protected abstract Boolean doEvaluate(QcnTerms terms);

	public static class Qualify_QcnADefault extends Qualify_QcnA {
		@Override
		protected Boolean doEvaluate(QcnTerms terms) {
			Boolean is_product = null;
			return assignOutput(is_product, terms);
		}
		
		protected Boolean assignOutput(Boolean is_product, QcnTerms terms) {
			is_product = areEqual(MapperS.of(terms).<String>map("getKind", qcnTerms -> qcnTerms.getKind()), MapperS.of("vanilla"), CardinalityOperator.All).get();
			
			return is_product;
		}
	}
		
		@Override
		public String getNamePrefix() {
			return "Qualify";
		}
}
