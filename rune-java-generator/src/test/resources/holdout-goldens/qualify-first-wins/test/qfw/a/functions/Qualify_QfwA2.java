package test.qfw.a.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.IQualifyFunctionExtension;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import test.qfw.a.QfwTerms;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(Qualify_QfwA2.Qualify_QfwA2Default.class)
public abstract class Qualify_QfwA2 implements RosettaFunction,IQualifyFunctionExtension<QfwTerms> {

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

	public static class Qualify_QfwA2Default extends Qualify_QfwA2 {
		@Override
		protected Boolean doEvaluate(QfwTerms terms) {
			Boolean is_product = null;
			return assignOutput(is_product, terms);
		}
		
		protected Boolean assignOutput(Boolean is_product, QfwTerms terms) {
			is_product = exists(MapperS.of(terms).<BigDecimal>map("getNotional", qfwTerms -> qfwTerms.getNotional())).get();
			
			return is_product;
		}
	}
		
		@Override
		public String getNamePrefix() {
			return "Qualify";
		}
}
