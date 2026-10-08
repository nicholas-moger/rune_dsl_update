package chaos.s26.a1o3.functions;

import chaos.s26.a1o3.C26KindEnum;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C26Nested.C26NestedDefault.class)
public abstract class C26Nested implements RosettaFunction {

	/**
	* @param k 
	* @param g 
	* @return r 
	*/
	public String evaluate(C26KindEnum k, String g) {
		String r = doEvaluate(k, g);
		
		return r;
	}

	protected abstract String doEvaluate(C26KindEnum k, String g);

	protected abstract MapperS<String> inner(C26KindEnum k, String g);

	protected abstract MapperS<Boolean> cmp(C26KindEnum k, String g);

	public static class C26NestedDefault extends C26Nested {
		@Override
		protected String doEvaluate(C26KindEnum k, String g) {
			String r = null;
			return assignOutput(r, k, g);
		}
		
		protected String assignOutput(String r, C26KindEnum k, String g) {
			if (exists(MapperS.of(k)).andNullSafe(ComparisonResult.ofNullSafe(cmp(k, g))).getOrDefault(false)) {
				r = inner(k, g).get();
			} else if (exists(MapperS.of(k)).getOrDefault(false)) {
				if (k == null) {
					r = null;
				} else if (k == C26KindEnum.BLUE) {
					r = "blue";
				} else {
					r = "none";
				}
			} else {
				r = "absent";
			}
			
			return r;
		}
		
		@Override
		protected MapperS<String> inner(C26KindEnum k, String g) {
			final MapperS<String> switchArgument = MapperS.of(g);
			if (switchArgument.get() == null) {
				return MapperS.<String>ofNull();
			}
			if (areEqual(switchArgument, MapperS.of("a"), CardinalityOperator.All).get()) {
				if (k == null) {
					return MapperS.<String>ofNull();
				}
				if (k == C26KindEnum.RED) {
					return MapperS.of("ar");
				}
				return MapperS.of("ax");
			}
			if (areEqual(switchArgument, MapperS.of("b"), CardinalityOperator.All).get()) {
				if (k == null) {
					return MapperS.<String>ofNull();
				}
				if (k == C26KindEnum.GREEN) {
					return MapperS.of("bg");
				}
				return MapperS.of("bx");
			}
			return MapperS.of("z");
		}
		
		@Override
		protected MapperS<Boolean> cmp(C26KindEnum k, String g) {
			final MapperS<String> switchArgument = MapperS.of(g);
			final MapperS<Integer> ifThenElseResult;
			if (switchArgument.get() == null) {
				ifThenElseResult = MapperS.<Integer>ofNull();
			} else if (areEqual(switchArgument, MapperS.of("a"), CardinalityOperator.All).get()) {
				ifThenElseResult = MapperS.of(1);
			} else {
				ifThenElseResult = MapperS.of(0);
			}
			return areEqual(ifThenElseResult, MapperS.of(1), CardinalityOperator.All).asMapper();
		}
	}
}
