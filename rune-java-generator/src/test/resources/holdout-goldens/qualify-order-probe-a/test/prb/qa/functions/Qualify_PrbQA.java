package test.prb.qa.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.IQualifyFunctionExtension;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import test.prb.qa.PrbTerms;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(Qualify_PrbQA.Qualify_PrbQADefault.class)
public abstract class Qualify_PrbQA implements RosettaFunction,IQualifyFunctionExtension<PrbTerms> {

	/**
	* @param terms 
	* @return is_product 
	*/
	@Override
	public Boolean evaluate(PrbTerms terms) {
		Boolean is_product = doEvaluate(terms);
		
		return is_product;
	}

	protected abstract Boolean doEvaluate(PrbTerms terms);

	public static class Qualify_PrbQADefault extends Qualify_PrbQA {
		@Override
		protected Boolean doEvaluate(PrbTerms terms) {
			Boolean is_product = null;
			return assignOutput(is_product, terms);
		}
		
		protected Boolean assignOutput(Boolean is_product, PrbTerms terms) {
			is_product = areEqual(MapperS.of(terms).<String>map("getKind", prbTerms -> prbTerms.getKind()), MapperS.of("qa"), CardinalityOperator.All).get();
			
			return is_product;
		}
	}
		
		@Override
		public String getNamePrefix() {
			return "Qualify";
		}
}
