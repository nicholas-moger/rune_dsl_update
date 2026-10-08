package test.chswitchedge.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import test.chswitchedge.Bag;
import test.chswitchedge.Either;
import test.chswitchedge.OptA;
import test.chswitchedge.OptB;


@ImplementedBy(PickAvRule.PickAvRuleDefault.class)
public abstract class PickAvRule implements ReportFunction<Bag, List<String>> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public List<String> evaluate(Bag input) {
		List<String> output = doEvaluate(input);
		
		return output;
	}

	protected abstract List<String> doEvaluate(Bag input);

	public static class PickAvRuleDefault extends PickAvRule {
		@Override
		protected List<String> doEvaluate(Bag input) {
			List<String> output = new ArrayList<>();
			return assignOutput(output, input);
		}
		
		protected List<String> assignOutput(List<String> output, Bag input) {
			final MapperC<Either> thenArg = MapperS.of(input)
				.mapSingleToList(item -> item.<Either>mapC("getEths", bag -> bag.getEths()));
			output = thenArg
				.mapItem(item -> {
					if (item.get() == null) {
						return MapperS.<String>ofNull();
					}
					if (item.<OptA>map("getOptA", either -> either.getOptA()).get() != null) {
						final MapperS<OptA> optA = item.<OptA>map("getOptA", either -> either.getOptA());
						return optA.<String>map("getAv", _optA -> _optA.getAv());
					}
					if (item.<OptB>map("getOptB", either -> either.getOptB()).get() != null) {
						final MapperS<OptB> optB = item.<OptB>map("getOptB", either -> either.getOptB());
						return optB.<BigDecimal>map("getBv", _optB -> _optB.getBv()).map("to-string", Object::toString);
					}
					return MapperS.of("none");
				}).getMulti();
			
			return output;
		}
	}
}
