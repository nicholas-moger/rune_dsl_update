package holdout.closureparamduplicatereads.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import holdout.closureparamduplicatereads.Item;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(ReduceDistinctCallRead.ReduceDistinctCallReadDefault.class)
public abstract class ReduceDistinctCallRead implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected Merge merge;

	/**
	* @param items 
	* @return merged 
	*/
	public Item evaluate(List<? extends Item> items) {
		Item.ItemBuilder mergedBuilder = doEvaluate(items);
		
		final Item merged;
		if (mergedBuilder == null) {
			merged = null;
		} else {
			merged = mergedBuilder.build();
			objectValidator.validate(Item.class, merged);
		}
		
		return merged;
	}

	protected abstract Item.ItemBuilder doEvaluate(List<? extends Item> items);

	public static class ReduceDistinctCallReadDefault extends ReduceDistinctCallRead {
		@Override
		protected Item.ItemBuilder doEvaluate(List<? extends Item> items) {
			if (items == null) {
				items = Collections.emptyList();
			}
			Item.ItemBuilder merged = Item.builder();
			return assignOutput(merged, items);
		}
		
		protected Item.ItemBuilder assignOutput(Item.ItemBuilder merged, List<? extends Item> items) {
			merged = toBuilder(MapperC.<Item>of(items)
				.<Item>reduce((a, b) -> MapperS.of(merge.evaluate(a.get(), b.get()))).get());
			
			return Optional.ofNullable(merged)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
