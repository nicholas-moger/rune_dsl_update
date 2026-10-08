package chaos.s16.a6choice.rival.util;

import chaos.s16.a6choice.rival.C16Pick;
import chaos.s16.a6choice.rival.C16RivalOpt;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

public class C16PickDeepPathUtil {
	public String chooseR(C16Pick c16Pick) {
		final MapperS<C16RivalOpt> c16RivalOpt = MapperS.of(c16Pick).<C16RivalOpt>map("getC16RivalOpt", _c16Pick -> _c16Pick.getC16RivalOpt());
		if (exists(c16RivalOpt).getOrDefault(false)) {
			return c16RivalOpt.<String>map("getR", _c16RivalOpt -> _c16RivalOpt.getR()).get();
		}
		return null;
	}
	
	public C16RivalOpt chooseC16RivalOpt(C16Pick c16Pick) {
		final MapperS<C16RivalOpt> c16RivalOpt = MapperS.of(c16Pick).<C16RivalOpt>map("getC16RivalOpt", _c16Pick -> _c16Pick.getC16RivalOpt());
		if (exists(c16RivalOpt).getOrDefault(false)) {
			return c16RivalOpt.get();
		}
		return null;
	}
	
}
