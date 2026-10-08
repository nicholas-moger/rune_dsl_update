package holdout.closureparamduplicatereads.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import holdout.closureparamduplicatereads.Item;
import java.math.BigDecimal;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(MergeV.MergeVDefault.class)
public abstract class MergeV implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param x 
	* @param n 
	* @return z 
	*/
	public Item evaluate(Item x, BigDecimal n) {
		Item.ItemBuilder zBuilder = doEvaluate(x, n);
		
		final Item z;
		if (zBuilder == null) {
			z = null;
		} else {
			z = zBuilder.build();
			objectValidator.validate(Item.class, z);
		}
		
		return z;
	}

	protected abstract Item.ItemBuilder doEvaluate(Item x, BigDecimal n);

	public static class MergeVDefault extends MergeV {
		@Override
		protected Item.ItemBuilder doEvaluate(Item x, BigDecimal n) {
			Item.ItemBuilder z = Item.builder();
			return assignOutput(z, x, n);
		}
		
		protected Item.ItemBuilder assignOutput(Item.ItemBuilder z, Item x, BigDecimal n) {
			z = toBuilder(x);
			
			return Optional.ofNullable(z)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
