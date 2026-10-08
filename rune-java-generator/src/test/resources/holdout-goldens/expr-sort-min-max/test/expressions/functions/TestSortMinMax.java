package test.expressions.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import test.expressions.Item;


@ImplementedBy(TestSortMinMax.TestSortMinMaxDefault.class)
public abstract class TestSortMinMax implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param items 
	* @return result 
	*/
	public List<? extends Item> evaluate(List<? extends Item> items) {
		List<Item.ItemBuilder> resultBuilder = doEvaluate(items);
		
		final List<? extends Item> result;
		if (resultBuilder == null) {
			result = null;
		} else {
			result = resultBuilder.stream().map(Item::build).collect(Collectors.toList());
			objectValidator.validate(Item.class, result);
		}
		
		return result;
	}

	protected abstract List<Item.ItemBuilder> doEvaluate(List<? extends Item> items);

	protected abstract MapperC<? extends Item> sorted(List<? extends Item> items);

	protected abstract MapperS<? extends Item> cheapest(List<? extends Item> items);

	protected abstract MapperS<? extends Item> expensive(List<? extends Item> items);

	public static class TestSortMinMaxDefault extends TestSortMinMax {
		@Override
		protected List<Item.ItemBuilder> doEvaluate(List<? extends Item> items) {
			if (items == null) {
				items = Collections.emptyList();
			}
			List<Item.ItemBuilder> result = new ArrayList<>();
			return assignOutput(result, items);
		}
		
		protected List<Item.ItemBuilder> assignOutput(List<Item.ItemBuilder> result, List<? extends Item> items) {
			result = toBuilder(sorted(items).getMulti());
			
			return Optional.ofNullable(result)
				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
				.orElse(null);
		}
		
		@Override
		protected MapperC<? extends Item> sorted(List<? extends Item> items) {
			return MapperC.<Item>of(items)
				.sort(x -> x.<BigDecimal>map("getPrice", item -> item.getPrice()));
		}
		
		@Override
		protected MapperS<? extends Item> cheapest(List<? extends Item> items) {
			return MapperC.<Item>of(items)
				.min(x -> x.<BigDecimal>map("getPrice", item -> item.getPrice()));
		}
		
		@Override
		protected MapperS<? extends Item> expensive(List<? extends Item> items) {
			return MapperC.<Item>of(items)
				.max(x -> x.<BigDecimal>map("getPrice", item -> item.getPrice()));
		}
	}
}
