package chaos.s26.a3half.p2.reports;

import chaos.s26.a3half.p2.C26Bag;
import chaos.s26.a3half.p2.C26Either;
import chaos.s26.a3half.p2.C26OptA;
import chaos.s26.a3half.p2.C26OptB;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@ImplementedBy(C26PickAvRule.C26PickAvRuleDefault.class)
public abstract class C26PickAvRule implements ReportFunction<C26Bag, List<String>> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public List<String> evaluate(C26Bag input) {
		List<String> output = doEvaluate(input);
		
		return output;
	}

	protected abstract List<String> doEvaluate(C26Bag input);

	public static class C26PickAvRuleDefault extends C26PickAvRule {
		@Override
		protected List<String> doEvaluate(C26Bag input) {
			List<String> output = new ArrayList<>();
			return assignOutput(output, input);
		}
		
		protected List<String> assignOutput(List<String> output, C26Bag input) {
			final MapperC<C26Either> thenArg = MapperS.of(input)
				.mapSingleToList(item -> item.<C26Either>mapC("getEths", c26Bag -> c26Bag.getEths()));
			output = thenArg
				.mapItem(item -> {
					if (item.get() == null) {
						return MapperS.<String>ofNull();
					}
					if (item.<C26OptA>map("getC26OptA", c26Either -> c26Either.getC26OptA()).get() != null) {
						final MapperS<C26OptA> c26OptA = item.<C26OptA>map("getC26OptA", c26Either -> c26Either.getC26OptA());
						return c26OptA.<String>map("getAv", _c26OptA -> _c26OptA.getAv());
					}
					if (item.<C26OptB>map("getC26OptB", c26Either -> c26Either.getC26OptB()).get() != null) {
						final MapperS<C26OptB> c26OptB = item.<C26OptB>map("getC26OptB", c26Either -> c26Either.getC26OptB());
						return c26OptB.<BigDecimal>map("getBv", _c26OptB -> _c26OptB.getBv()).map("to-string", Object::toString);
					}
					return MapperS.of("none");
				}).getMulti();
			
			return output;
		}
	}
}
