package com.rosetta.test.model.agreement.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.test.model.agreement.Bar;
import com.rosetta.test.model.agreement.Foo;
import com.rosetta.test.model.agreement.Top;
import java.math.BigDecimal;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(UpdateBarId.UpdateBarIdDefault.class)
public abstract class UpdateBarId implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param top 
	* @param newId 
	* @return topOut 
	*/
	public Top evaluate(Top top, BigDecimal newId) {
		Top.TopBuilder topOutBuilder = doEvaluate(top, newId);
		
		final Top topOut;
		if (topOutBuilder == null) {
			topOut = null;
		} else {
			topOut = topOutBuilder.build();
			objectValidator.validate(Top.class, topOut);
		}
		
		return topOut;
	}

	protected abstract Top.TopBuilder doEvaluate(Top top, BigDecimal newId);

	protected abstract Bar.BarBuilder barAlias(Top.TopBuilder topOut, Top top, BigDecimal newId);

	public static class UpdateBarIdDefault extends UpdateBarId {
		@Override
		protected Top.TopBuilder doEvaluate(Top top, BigDecimal newId) {
			Top.TopBuilder topOut = Top.builder();
			return assignOutput(topOut, top, newId);
		}
		
		protected Top.TopBuilder assignOutput(Top.TopBuilder topOut, Top top, BigDecimal newId) {
			topOut.getOrCreateFoo().getOrCreateBar()
				.setId(newId);
			
			return Optional.ofNullable(topOut)
				.map(o -> o.prune())
				.orElse(null);
		}
		
		@Override
		protected Bar.BarBuilder barAlias(Top.TopBuilder topOut, Top top, BigDecimal newId) {
			return toBuilder(MapperS.of(topOut).<Foo>map("getFoo", _top -> _top.getFoo()).<Bar>map("getBar", foo -> foo.getBar()).get());
		}
	}
}
