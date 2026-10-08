package holdout.voiddeeptok.util;

import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaVoid;
import holdout.voiddeeptok.Holder;
import holdout.voiddeeptok.HolderA;
import holdout.voiddeeptok.HolderB;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

public class HolderDeepPathUtil {
	public FieldWithMetaVoid chooseTok(Holder holder) {
		final MapperS<HolderA> holderA = MapperS.of(holder).<HolderA>map("getHolderA", _holder -> _holder.getHolderA());
		if (exists(holderA).getOrDefault(false)) {
			return holderA.<FieldWithMetaVoid>map("getTok", _holderA -> _holderA.getTok()).get();
		}
		final MapperS<HolderB> holderB = MapperS.of(holder).<HolderB>map("getHolderB", _holder -> _holder.getHolderB());
		if (exists(holderB).getOrDefault(false)) {
			return holderB.<FieldWithMetaVoid>map("getTok", _holderB -> _holderB.getTok()).get();
		}
		return FieldWithMetaVoid.builder().build();
	}
	
}
