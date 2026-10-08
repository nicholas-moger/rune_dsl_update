package chaos.s18.a2dangle.functions;

import chaos.s18.a2dangle.C18Either;
import chaos.s18.a2dangle.C18OptA;
import chaos.s18.a2dangle.C18OptB;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;


@ImplementedBy(C18Pull.C18PullDefault.class)
public abstract class C18Pull implements RosettaFunction {

	/**
	* @param eth 
	* @return out 
	*/
	public String evaluate(C18Either eth) {
		String out = doEvaluate(eth);
		
		return out;
	}

	protected abstract String doEvaluate(C18Either eth);

	public static class C18PullDefault extends C18Pull {
		@Override
		protected String doEvaluate(C18Either eth) {
			String out = null;
			return assignOutput(out, eth);
		}
		
		protected String assignOutput(String out, C18Either eth) {
			final MapperS<C18Either> switchArgument = MapperS.of(eth);
			if (switchArgument.get() == null) {
				out = null;
			} else if (switchArgument.<C18OptA>map("getC18OptA", c18Either -> c18Either.getC18OptA()).get() != null) {
				final MapperS<C18OptA> c18OptA = switchArgument.<C18OptA>map("getC18OptA", c18Either -> c18Either.getC18OptA());
				out = c18OptA.<String>map("getAv", _c18OptA -> _c18OptA.getAv()).get();
			} else if (switchArgument.<C18OptB>map("getC18OptB", c18Either -> c18Either.getC18OptB()).get() != null) {
				final MapperS<C18OptB> c18OptB = switchArgument.<C18OptB>map("getC18OptB", c18Either -> c18Either.getC18OptB());
				out = c18OptB.<BigDecimal>map("getBv", _c18OptB -> _c18OptB.getBv()).map("to-string", Object::toString).get();
			} else {
				out = "none";
			}
			
			return out;
		}
	}
}
