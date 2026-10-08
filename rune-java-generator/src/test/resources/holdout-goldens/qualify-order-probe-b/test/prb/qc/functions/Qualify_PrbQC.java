package test.prb.qc.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.IQualifyFunctionExtension;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import test.prb.qc.PrbTerms;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(Qualify_PrbQC.Qualify_PrbQCDefault.class)
public abstract class Qualify_PrbQC implements RosettaFunction,IQualifyFunctionExtension<PrbTerms> {

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

	public static class Qualify_PrbQCDefault extends Qualify_PrbQC {
		@Override
		protected Boolean doEvaluate(PrbTerms terms) {
			Boolean is_product = null;
			return assignOutput(is_product, terms);
		}
		
		protected Boolean assignOutput(Boolean is_product, PrbTerms terms) {
			is_product = areEqual(MapperS.of(terms).<String>map("getKind", prbTerms -> prbTerms.getKind()), MapperS.of("qc"), CardinalityOperator.All).get();
			
			return is_product;
		}
	}
		
		@Override
		public String getNamePrefix() {
			return "Qualify";
		}
}
