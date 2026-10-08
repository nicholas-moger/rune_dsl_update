package holdout.closureparamduplicatereads.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import holdout.closureparamduplicatereads.Item;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(Merge.MergeDefault.class)
public abstract class Merge implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param x 
	* @param y 
	* @return z 
	*/
	public Item evaluate(Item x, Item y) {
		Item.ItemBuilder zBuilder = doEvaluate(x, y);
		
		final Item z;
		if (zBuilder == null) {
			z = null;
		} else {
			z = zBuilder.build();
			objectValidator.validate(Item.class, z);
		}
		
		return z;
	}

	protected abstract Item.ItemBuilder doEvaluate(Item x, Item y);

	public static class MergeDefault extends Merge {
		@Override
		protected Item.ItemBuilder doEvaluate(Item x, Item y) {
			Item.ItemBuilder z = Item.builder();
			return assignOutput(z, x, y);
		}
		
		protected Item.ItemBuilder assignOutput(Item.ItemBuilder z, Item x, Item y) {
			z = toBuilder(x);
			
			return Optional.ofNullable(z)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
