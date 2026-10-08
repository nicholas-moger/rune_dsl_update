package holdout.closureparamduplicatereads.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import holdout.closureparamduplicatereads.Item;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(ReduceDistinctNavRead.ReduceDistinctNavReadDefault.class)
public abstract class ReduceDistinctNavRead implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected MergeV mergeV;

	/**
	* @param items 
	* @return pick 
	*/
	public Item evaluate(List<? extends Item> items) {
		Item.ItemBuilder pickBuilder = doEvaluate(items);
		
		final Item pick;
		if (pickBuilder == null) {
			pick = null;
		} else {
			pick = pickBuilder.build();
			objectValidator.validate(Item.class, pick);
		}
		
		return pick;
	}

	protected abstract Item.ItemBuilder doEvaluate(List<? extends Item> items);

	public static class ReduceDistinctNavReadDefault extends ReduceDistinctNavRead {
		@Override
		protected Item.ItemBuilder doEvaluate(List<? extends Item> items) {
			if (items == null) {
				items = Collections.emptyList();
			}
			Item.ItemBuilder pick = Item.builder();
			return assignOutput(pick, items);
		}
		
		protected Item.ItemBuilder assignOutput(Item.ItemBuilder pick, List<? extends Item> items) {
			pick = toBuilder(MapperC.<Item>of(items)
				.<Item>reduce((a, b) -> MapperS.of(mergeV.evaluate(a.get(), b.<BigDecimal>map("getV", item -> item.getV()).get()))).get());
			
			return Optional.ofNullable(pick)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
