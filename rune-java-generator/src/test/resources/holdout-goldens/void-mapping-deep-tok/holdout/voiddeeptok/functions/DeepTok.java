package holdout.voiddeeptok.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaVoid;
import holdout.voiddeeptok.Holder;
import holdout.voiddeeptok.util.HolderDeepPathUtil;
import javax.inject.Inject;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(DeepTok.DeepTokDefault.class)
public abstract class DeepTok implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected HolderDeepPathUtil holderDeepPathUtil;

	/**
	* @param h 
	* @return r 
	*/
	public Boolean evaluate(Holder h) {
		Boolean r = doEvaluate(h);
		
		return r;
	}

	protected abstract Boolean doEvaluate(Holder h);

	public static class DeepTokDefault extends DeepTok {
		@Override
		protected Boolean doEvaluate(Holder h) {
			Boolean r = null;
			return assignOutput(r, h);
		}
		
		protected Boolean assignOutput(Boolean r, Holder h) {
			r = exists(MapperS.of(h).<FieldWithMetaVoid>map("chooseTok", holder -> holderDeepPathUtil.chooseTok(holder))).get();
			
			return r;
		}
	}
}
