package test.qcn.b.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.IQualifyFunctionExtension;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import test.qcn.a.QcnTerms;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(Qualify_QcnB.Qualify_QcnBDefault.class)
public abstract class Qualify_QcnB implements RosettaFunction,IQualifyFunctionExtension<QcnTerms> {

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

	public static class Qualify_QcnBDefault extends Qualify_QcnB {
		@Override
		protected Boolean doEvaluate(QcnTerms terms) {
			Boolean is_product = null;
			return assignOutput(is_product, terms);
		}
		
		protected Boolean assignOutput(Boolean is_product, QcnTerms terms) {
			is_product = exists(MapperS.of(terms).<BigDecimal>map("getNotional", qcnTerms -> qcnTerms.getNotional())).get();
			
			return is_product;
		}
	}
		
		@Override
		public String getNamePrefix() {
			return "Qualify";
		}
}
