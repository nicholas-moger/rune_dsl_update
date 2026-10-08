package com.rosetta.test.model.agreement.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.test.model.agreement.Bar;
import com.rosetta.test.model.agreement.Foo;
import com.rosetta.test.model.agreement.Top;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(ExtractBar.ExtractBarDefault.class)
public abstract class ExtractBar implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param top 
	* @return topOut 
	*/
	public Top evaluate(Top top) {
		Top.TopBuilder topOutBuilder = doEvaluate(top);
		
		final Top topOut;
		if (topOutBuilder == null) {
			topOut = null;
		} else {
			topOut = topOutBuilder.build();
			objectValidator.validate(Top.class, topOut);
		}
		
		return topOut;
	}

	protected abstract Top.TopBuilder doEvaluate(Top top);

	protected abstract Foo.FooBuilder fooAlias(Top.TopBuilder topOut, Top top);

	public static class ExtractBarDefault extends ExtractBar {
		@Override
		protected Top.TopBuilder doEvaluate(Top top) {
			Top.TopBuilder topOut = Top.builder();
			return assignOutput(topOut, top);
		}
		
		protected Top.TopBuilder assignOutput(Top.TopBuilder topOut, Top top) {
			topOut.getOrCreateFoo()
				.setBar1(MapperS.of(top).<Foo>map("getFoo", _top -> _top.getFoo()).<Bar>map("getBar1", foo -> foo.getBar1()).get());
			
			topOut
				.getOrCreateFoo()
				.setBar2(MapperS.of(top).<Foo>map("getFoo", _top -> _top.getFoo()).<Bar>map("getBar2", foo -> foo.getBar2()).get());
			
			return Optional.ofNullable(topOut)
				.map(o -> o.prune())
				.orElse(null);
		}
		
		@Override
		protected Foo.FooBuilder fooAlias(Top.TopBuilder topOut, Top top) {
			return toBuilder(MapperS.of(topOut).<Foo>map("getFoo", _top -> _top.getFoo()).get());
		}
	}
}
