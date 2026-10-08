package chaos.s07.a1o2.reports;

import chaos.s07.a1o2.C7Trade;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;


@ImplementedBy(C7NotionalRule.C7NotionalRuleDefault.class)
public abstract class C7NotionalRule implements ReportFunction<C7Trade, BigDecimal> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public BigDecimal evaluate(C7Trade input) {
		BigDecimal output = doEvaluate(input);
		
		return output;
	}

	protected abstract BigDecimal doEvaluate(C7Trade input);

	public static class C7NotionalRuleDefault extends C7NotionalRule {
		@Override
		protected BigDecimal doEvaluate(C7Trade input) {
			BigDecimal output = null;
			return assignOutput(output, input);
		}
		
		protected BigDecimal assignOutput(BigDecimal output, C7Trade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> MapperS.of(item.<BigDecimal>map("getNotional", c7Trade -> c7Trade.getNotional()).getOrDefault(BigDecimal.valueOf(0)))).get();
			
			return output;
		}
	}
}
