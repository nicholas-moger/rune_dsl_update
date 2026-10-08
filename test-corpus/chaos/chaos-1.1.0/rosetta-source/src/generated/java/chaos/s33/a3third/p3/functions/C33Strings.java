package chaos.s33.a3third.p3.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C33Strings.C33StringsDefault.class)
public abstract class C33Strings implements RosettaFunction {

	/**
	* @param s 
	* @return r 
	*/
	public String evaluate(String s) {
		String r = doEvaluate(s);
		
		return r;
	}

	protected abstract String doEvaluate(String s);

	protected abstract MapperS<String> parts(String s);

	public static class C33StringsDefault extends C33Strings {
		@Override
		protected String doEvaluate(String s) {
			String r = null;
			return assignOutput(r, s);
		}
		
		protected String assignOutput(String r, String s) {
			final MapperS<String> switchArgument = MapperS.of(s);
			if (switchArgument.get() == null) {
				r = null;
			} else if (areEqual(switchArgument, MapperS.of("\"q\""), CardinalityOperator.All).get()) {
				r = "quote";
			} else if (areEqual(switchArgument, MapperS.of("\u00B5\u2013\u00FC"), CardinalityOperator.All).get()) {
				r = "uni";
			} else {
				r = parts(s).get();
			}
			
			return r;
		}
		
		@Override
		protected MapperS<String> parts(String s) {
			final MapperC<String> thenArg = MapperC.<String>of(MapperS.of("\u00B5"), MapperS.of("\"q\""), MapperS.of("\\"), MapperS.of("\t"));
			return thenArg.join(MapperS.of("\u2013"));
		}
	}
}
