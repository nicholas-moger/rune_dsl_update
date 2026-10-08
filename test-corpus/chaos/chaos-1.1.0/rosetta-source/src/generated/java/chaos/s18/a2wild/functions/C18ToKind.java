package chaos.s18.a2wild.functions;

import chaos.s18.a2wild.C18Either;
import chaos.s18.a2wild.C18KindEnum;
import chaos.s18.a2wild.C18OptA;
import chaos.s18.a2wild.C18OptB;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C18ToKind.C18ToKindDefault.class)
public abstract class C18ToKind implements RosettaFunction {

	/**
	* @param raw 
	* @param eths 
	* @return k 
	*/
	public C18KindEnum evaluate(String raw, List<? extends C18Either> eths) {
		C18KindEnum k = doEvaluate(raw, eths);
		
		return k;
	}

	protected abstract C18KindEnum doEvaluate(String raw, List<? extends C18Either> eths);

	protected abstract MapperC<String> switched(String raw, List<? extends C18Either> eths);

	protected abstract MapperC<String> pulled(String raw, List<? extends C18Either> eths);

	public static class C18ToKindDefault extends C18ToKind {
		@Override
		protected C18KindEnum doEvaluate(String raw, List<? extends C18Either> eths) {
			if (eths == null) {
				eths = Collections.emptyList();
			}
			C18KindEnum k = null;
			return assignOutput(k, raw, eths);
		}
		
		protected C18KindEnum assignOutput(C18KindEnum k, String raw, List<? extends C18Either> eths) {
			if (areEqual(MapperS.of(pulled(raw, eths).resultCount()), MapperS.of(switched(raw, eths).resultCount()), CardinalityOperator.All).orNullSafe(notExists(pulled(raw, eths))).getOrDefault(false)) {
				k = MapperS.of(raw).checkedMap("to-enum", C18KindEnum::fromDisplayName, IllegalArgumentException.class).get();
			} else {
				k = null;
			}
			
			return k;
		}
		
		@Override
		protected MapperC<String> switched(String raw, List<? extends C18Either> eths) {
			return MapperC.<String>of(MapperS.of(raw))
				.mapItem(item -> {
					if (item.get() == null) {
						return MapperS.<String>ofNull();
					}
					if (areEqual(item, MapperS.of("r"), CardinalityOperator.All).get()) {
						return MapperS.of("Red");
					}
					if (areEqual(item, MapperS.of("g"), CardinalityOperator.All).get()) {
						return MapperS.of("Green");
					}
					return MapperS.of("Blue");
				});
		}
		
		@Override
		protected MapperC<String> pulled(String raw, List<? extends C18Either> eths) {
			return MapperC.<C18Either>of(eths)
				.mapItem(item -> {
					if (item.get() == null) {
						return MapperS.<String>ofNull();
					}
					if (item.<C18OptA>map("getC18OptA", c18Either -> c18Either.getC18OptA()).get() != null) {
						final MapperS<C18OptA> c18OptA = item.<C18OptA>map("getC18OptA", c18Either -> c18Either.getC18OptA());
						return c18OptA.<String>map("getAv", _c18OptA -> _c18OptA.getAv());
					}
					if (item.<C18OptB>map("getC18OptB", c18Either -> c18Either.getC18OptB()).get() != null) {
						final MapperS<C18OptB> c18OptB = item.<C18OptB>map("getC18OptB", c18Either -> c18Either.getC18OptB());
						return c18OptB.<BigDecimal>map("getBv", _c18OptB -> _c18OptB.getBv()).map("to-string", Object::toString);
					}
					return MapperS.of("none");
				});
		}
	}
}
