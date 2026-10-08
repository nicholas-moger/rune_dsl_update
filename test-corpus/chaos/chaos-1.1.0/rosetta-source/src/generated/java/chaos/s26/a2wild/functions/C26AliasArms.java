package chaos.s26.a2wild.functions;

import chaos.s26.a2wild.h.C26Tag;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C26AliasArms.C26AliasArmsDefault.class)
public abstract class C26AliasArms implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected C26Upper c26Upper;

	/**
	* @param raws 
	* @param tags 
	* @return outs 
	*/
	public List<String> evaluate(List<String> raws, List<? extends C26Tag> tags) {
		List<String> outs = doEvaluate(raws, tags);
		
		return outs;
	}

	protected abstract List<String> doEvaluate(List<String> raws, List<? extends C26Tag> tags);

	protected abstract MapperC<String> bare(List<String> raws, List<? extends C26Tag> tags);

	protected abstract MapperC<String> called(List<String> raws, List<? extends C26Tag> tags);

	protected abstract MapperC<String> navved(List<String> raws, List<? extends C26Tag> tags);

	protected abstract MapperC<String> stringed(List<String> raws, List<? extends C26Tag> tags);

	protected abstract MapperC<String> paramed(List<String> raws, List<? extends C26Tag> tags);

	public static class C26AliasArmsDefault extends C26AliasArms {
		@Override
		protected List<String> doEvaluate(List<String> raws, List<? extends C26Tag> tags) {
			if (raws == null) {
				raws = Collections.emptyList();
			}
			if (tags == null) {
				tags = Collections.emptyList();
			}
			List<String> outs = new ArrayList<>();
			return assignOutput(outs, raws, tags);
		}
		
		protected List<String> assignOutput(List<String> outs, List<String> raws, List<? extends C26Tag> tags) {
			outs.addAll(bare(raws, tags).getMulti());
			
			outs.addAll(called(raws, tags).getMulti());
			
			outs.addAll(navved(raws, tags).getMulti());
			
			outs.addAll(stringed(raws, tags).getMulti());
			
			outs.addAll(paramed(raws, tags).getMulti());
			
			return outs;
		}
		
		@Override
		protected MapperC<String> bare(List<String> raws, List<? extends C26Tag> tags) {
			return MapperC.<String>of(raws)
				.mapItem(item -> {
					if (item.get() == null) {
						return MapperS.<String>ofNull();
					}
					if (areEqual(item, MapperS.of("keep"), CardinalityOperator.All).get()) {
						return item;
					}
					return MapperS.of("other");
				});
		}
		
		@Override
		protected MapperC<String> called(List<String> raws, List<? extends C26Tag> tags) {
			return MapperC.<String>of(raws)
				.mapItem(item -> {
					if (item.get() == null) {
						return MapperS.<String>ofNull();
					}
					if (areEqual(item, MapperS.of("up"), CardinalityOperator.All).get()) {
						return MapperS.of(c26Upper.evaluate(item.get()));
					}
					return item;
				});
		}
		
		@Override
		protected MapperC<String> navved(List<String> raws, List<? extends C26Tag> tags) {
			return MapperC.<C26Tag>of(tags)
				.mapItem(item -> {
					final MapperS<String> switchArgument = item.<String>map("getCode", c26Tag -> c26Tag.getCode());
					if (switchArgument.get() == null) {
						return MapperS.<String>ofNull();
					}
					if (areEqual(switchArgument, MapperS.of("x"), CardinalityOperator.All).get()) {
						return item.<String>map("getCode", c26Tag -> c26Tag.getCode());
					}
					return MapperS.of("y");
				});
		}
		
		@Override
		protected MapperC<String> stringed(List<String> raws, List<? extends C26Tag> tags) {
			return MapperC.<C26Tag>of(tags)
				.mapItem(item -> {
					final MapperS<String> switchArgument = item.<String>map("getCode", c26Tag -> c26Tag.getCode());
					if (switchArgument.get() == null) {
						return MapperS.<String>ofNull();
					}
					if (areEqual(switchArgument, MapperS.of("n"), CardinalityOperator.All).get()) {
						return MapperS.of(item.<String>map("getCode", c26Tag -> c26Tag.getCode()).resultCount()).map("to-string", Object::toString);
					}
					return MapperS.of("0");
				});
		}
		
		@Override
		protected MapperC<String> paramed(List<String> raws, List<? extends C26Tag> tags) {
			return MapperC.<String>of(raws)
				.mapItem(p -> {
					if (p.get() == null) {
						return MapperS.<String>ofNull();
					}
					if (areEqual(p, MapperS.of("r"), CardinalityOperator.All).get()) {
						return MapperS.of("red");
					}
					return MapperS.of("grey");
				});
		}
	}
}
