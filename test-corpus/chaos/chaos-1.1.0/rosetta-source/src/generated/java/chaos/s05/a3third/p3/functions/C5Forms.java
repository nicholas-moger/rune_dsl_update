package chaos.s05.a3third.p3.functions;

import chaos.s05.a3third.p1.C5Item;
import chaos.s05.a3third.p1.C5KindEnum;
import chaos.s05.a3third.p1.C5Sub;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperListOfLists;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C5Forms.C5FormsDefault.class)
public abstract class C5Forms implements RosettaFunction {

	/**
	* @param items 
	* @param others 
	* @param raws 
	* @return verdict 
	*/
	public String evaluate(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
		String verdict = doEvaluate(items, others, raws);
		
		return verdict;
	}

	protected abstract String doEvaluate(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperC<BigDecimal> vals(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperS<? extends C5Item> solo(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperS<? extends C5Item> soloThen(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperS<Integer> neg(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperS<BigDecimal> lastV(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperS<BigDecimal> hiV(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperS<BigDecimal> loV(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperC<String> uniqRaw(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperC<String> uniqOnes(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperS<Boolean> disWith(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperC<BigDecimal> fallback(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperS<Integer> graded(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperS<Integer> asInt(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperS<C5KindEnum> asEnum(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperC<C5KindEnum> enumsThen(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperC<String> strsThen(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperS<Boolean> anyLeft(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperS<Boolean> noneLeft(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperC<String> eqForm(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperS<Boolean> boolArm(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperC<String> lambdaThen(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperC<BigDecimal> lambdaIte(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	protected abstract MapperS<BigDecimal> soloArm(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws);

	public static class C5FormsDefault extends C5Forms {
		@Override
		protected String doEvaluate(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			if (items == null) {
				items = Collections.emptyList();
			}
			if (others == null) {
				others = Collections.emptyList();
			}
			if (raws == null) {
				raws = Collections.emptyList();
			}
			String verdict = null;
			return assignOutput(verdict, items, others, raws);
		}
		
		protected String assignOutput(String verdict, List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			final MapperC<BigDecimal> thenArg = fallback(items, others, raws);
			if (ComparisonResult.ofNullSafe(disWith(items, others, raws)).andNullSafe(ComparisonResult.ofNullSafe(anyLeft(items, others, raws))).andNullSafe(lessThan(neg(items, others, raws), MapperS.of(0), CardinalityOperator.All)).andNullSafe(exists(solo(items, others, raws))).andNullSafe(exists(soloThen(items, others, raws))).andNullSafe(exists(lastV(items, others, raws))).andNullSafe(exists(hiV(items, others, raws))).andNullSafe(exists(loV(items, others, raws))).andNullSafe(greaterThanEquals(MapperS.of(uniqOnes(items, others, raws).resultCount()), MapperS.of(eqForm(items, others, raws).resultCount()), CardinalityOperator.All)).andNullSafe(greaterThanEquals(thenArg
				.sumBigDecimal(), asInt(items, others, raws).<BigDecimal>map("Type coercion", integer -> integer == null ? null : BigDecimal.valueOf(integer)), CardinalityOperator.All)).andNullSafe(greaterThanEquals(graded(items, others, raws), MapperS.of(0), CardinalityOperator.All)).andNullSafe(exists(asEnum(items, others, raws))).andNullSafe(ComparisonResult.ofNullSafe(noneLeft(items, others, raws))).andNullSafe(areEqual(MapperS.of(enumsThen(items, others, raws).resultCount()), MapperS.of(strsThen(items, others, raws).resultCount()), CardinalityOperator.All)).andNullSafe(ComparisonResult.ofNullSafe(boolArm(items, others, raws))).andNullSafe(greaterThanEquals(MapperS.of(lambdaThen(items, others, raws).resultCount()), MapperS.of(0), CardinalityOperator.All)).andNullSafe(greaterThanEquals(lambdaIte(items, others, raws)
				.sumBigDecimal(), MapperS.of(BigDecimal.valueOf(0)), CardinalityOperator.All)).andNullSafe(exists(soloArm(items, others, raws))).getOrDefault(false)) {
				verdict = "covered";
			} else {
				verdict = "sparse";
			}
			
			return verdict;
		}
		
		@Override
		protected MapperC<BigDecimal> vals(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			return MapperC.<C5Item>of(items)
				.mapItem(item -> item.<BigDecimal>map("getOpt", c5Item -> c5Item.getOpt()));
		}
		
		@Override
		protected MapperS<? extends C5Item> solo(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			return MapperS.of(MapperC.of(items).get());
		}
		
		@Override
		protected MapperS<? extends C5Item> soloThen(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			final MapperC<C5Item> thenArg = MapperC.<C5Item>of(items);
			return MapperS.of(thenArg.get());
		}
		
		@Override
		protected MapperS<Integer> neg(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			return MapperS.of(-1);
		}
		
		@Override
		protected MapperS<BigDecimal> lastV(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			final MapperC<BigDecimal> thenArg = vals(items, others, raws);
			return thenArg
				.last();
		}
		
		@Override
		protected MapperS<BigDecimal> hiV(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			final MapperC<BigDecimal> thenArg = vals(items, others, raws);
			return thenArg
				.max();
		}
		
		@Override
		protected MapperS<BigDecimal> loV(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			final MapperC<BigDecimal> thenArg = vals(items, others, raws);
			return thenArg
				.min();
		}
		
		@Override
		protected MapperC<String> uniqRaw(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			return distinct(MapperC.<String>of(raws));
		}
		
		@Override
		protected MapperC<String> uniqOnes(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			final MapperC<String> thenArg = MapperC.<C5Item>of(items)
				.mapItem(item -> item.<String>map("getOne", c5Item -> c5Item.getOne()));
			return distinct(thenArg);
		}
		
		@Override
		protected MapperS<Boolean> disWith(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			return disjoint(uniqRaw(items, others, raws), MapperC.<C5Item>of(others)
				.mapItem(item -> item.<String>map("getOne", c5Item -> c5Item.getOne()))).asMapper();
		}
		
		@Override
		protected MapperC<BigDecimal> fallback(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			final MapperC<BigDecimal> thenArg = vals(items, others, raws);
			return (thenArg.getMulti().isEmpty() ? MapperC.<Integer>of(MapperS.of(0)).<BigDecimal>map("Type coercion", integer -> BigDecimal.valueOf(integer)) : thenArg);
		}
		
		@Override
		protected MapperS<Integer> graded(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			final MapperC<C5Item> thenArg0 = MapperC.<C5Item>of(items);
			final MapperS<C5Item> thenArg1 = MapperS.of(thenArg0.get());
			final MapperS<String> thenArg2 = thenArg1
				.mapSingleToItem(item -> item.<String>map("getOne", c5Item -> c5Item.getOne()));
			final MapperS<Integer> ifThenElseResult;
			if (thenArg2.get() == null) {
				ifThenElseResult = MapperS.<Integer>ofNull();
			} else if (areEqual(thenArg2, MapperS.of("a"), CardinalityOperator.All).get()) {
				ifThenElseResult = MapperS.of(1);
			} else {
				ifThenElseResult = MapperS.of(0);
			}
			return ifThenElseResult;
		}
		
		@Override
		protected MapperS<Integer> asInt(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			return MapperS.of("7").checkedMap("to-int", Integer::parseInt, NumberFormatException.class);
		}
		
		@Override
		protected MapperS<C5KindEnum> asEnum(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			return MapperS.of("Spot").checkedMap("to-enum", C5KindEnum::fromDisplayName, IllegalArgumentException.class);
		}
		
		@Override
		protected MapperC<C5KindEnum> enumsThen(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			final MapperC<String> thenArg = MapperC.<String>of(MapperS.of("Fwd"), MapperS.of("Spot"));
			return thenArg
				.mapItem(item -> item.checkedMap("to-enum", C5KindEnum::fromDisplayName, IllegalArgumentException.class));
		}
		
		@Override
		protected MapperC<String> strsThen(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			final MapperC<BigDecimal> thenArg = vals(items, others, raws);
			return thenArg
				.mapItem(item -> item.map("to-string", Object::toString));
		}
		
		@Override
		protected MapperS<Boolean> anyLeft(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			final MapperC<C5Item> thenArg = MapperC.<C5Item>of(items);
			return exists(thenArg).asMapper();
		}
		
		@Override
		protected MapperS<Boolean> noneLeft(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			final MapperC<C5Item> thenArg = MapperC.<C5Item>of(others);
			return notExists(thenArg).asMapper();
		}
		
		@Override
		protected MapperC<String> eqForm(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			final MapperC<String> thenArg = MapperC.<C5Item>of(items)
				.mapItem(item -> item.<String>map("getOne", c5Item -> c5Item.getOne()));
			return thenArg
				.filterItemNullSafe(item -> areEqual(item, MapperS.of("x"), CardinalityOperator.All).get());
		}
		
		@Override
		protected MapperS<Boolean> boolArm(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			if (anyLeft(items, others, raws).getOrDefault(false)) {
				return ComparisonResult.ofNullSafe(disWith(items, others, raws)).andNullSafe(ComparisonResult.ofNullSafe(noneLeft(items, others, raws))).asMapper();
			}
			return lessThan(neg(items, others, raws), MapperS.of(0), CardinalityOperator.All).asMapper();
		}
		
		@Override
		protected MapperC<String> lambdaThen(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			final MapperListOfLists<String> thenArg = MapperC.<C5Item>of(items)
				.mapItemToList(x -> {
					final MapperC<C5Sub> thenArg0 = x.<C5Sub>mapC("getSub", c5Item -> c5Item.getSub());
					final MapperC<C5Sub> thenArg1 = thenArg0
						.filterItemNullSafe(item -> exists(item.<BigDecimal>mapC("getVals", c5Sub -> c5Sub.getVals())).get());
					return thenArg1
						.mapItem(item -> item.<String>map("getName", c5Sub -> c5Sub.getName()));
				});
			return thenArg
				.flattenList();
		}
		
		@Override
		protected MapperC<BigDecimal> lambdaIte(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			return MapperC.<C5Item>of(items)
				.mapItem(x -> {
					if (exists(x.<BigDecimal>map("getOpt", c5Item -> c5Item.getOpt())).getOrDefault(false)) {
						return x.<BigDecimal>map("getOpt", c5Item -> c5Item.getOpt());
					}
					return MapperS.of(BigDecimal.valueOf(0));
				});
		}
		
		@Override
		protected MapperS<BigDecimal> soloArm(List<? extends C5Item> items, List<? extends C5Item> others, List<String> raws) {
			if (anyLeft(items, others, raws).getOrDefault(false)) {
				return MapperS.of(MapperC.of(items).get()).<BigDecimal>map("getOpt", c5Item -> c5Item.getOpt());
			}
			return MapperC.<C5Item>of(others)
				.first().<BigDecimal>map("getOpt", c5Item -> c5Item.getOpt());
		}
	}
}
