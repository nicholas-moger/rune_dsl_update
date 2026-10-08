package test.qfw.b.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.IQualifyFunctionExtension;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import test.qfw.b.QfwTerms;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(Qualify_QfwB1.Qualify_QfwB1Default.class)
public abstract class Qualify_QfwB1 implements RosettaFunction,IQualifyFunctionExtension<QfwTerms> {

	/**
	* @param terms 
	* @return is_product 
	*/
	@Override
	public Boolean evaluate(QfwTerms terms) {
		Boolean is_product = doEvaluate(terms);
		
		return is_product;
	}

	protected abstract Boolean doEvaluate(QfwTerms terms);

	public static class Qualify_QfwB1Default extends Qualify_QfwB1 {
		@Override
		protected Boolean doEvaluate(QfwTerms terms) {
			Boolean is_product = null;
			return assignOutput(is_product, terms);
		}
		
		protected Boolean assignOutput(Boolean is_product, QfwTerms terms) {
			is_product = areEqual(MapperS.of(terms).<String>map("getKind", qfwTerms -> qfwTerms.getKind()), MapperS.of("exotic"), CardinalityOperator.All).get();
			
			return is_product;
		}
	}
		
		@Override
		public String getNamePrefix() {
			return "Qualify";
		}
}
