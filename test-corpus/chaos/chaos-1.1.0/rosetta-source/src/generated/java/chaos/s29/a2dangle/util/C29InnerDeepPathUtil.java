package chaos.s29.a2dangle.util;

import chaos.s29.a2dangle.C29In1;
import chaos.s29.a2dangle.C29In2;
import chaos.s29.a2dangle.C29Inner;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

public class C29InnerDeepPathUtil {
	public String chooseDeep(C29Inner c29Inner) {
		final MapperS<C29In1> c29In1 = MapperS.of(c29Inner).<C29In1>map("getC29In1", _c29Inner -> _c29Inner.getC29In1());
		if (exists(c29In1).getOrDefault(false)) {
			return c29In1.<String>map("getDeep", _c29In1 -> _c29In1.getDeep()).get();
		}
		final MapperS<C29In2> c29In2 = MapperS.of(c29Inner).<C29In2>map("getC29In2", _c29Inner -> _c29Inner.getC29In2());
		if (exists(c29In2).getOrDefault(false)) {
			return c29In2.<String>map("getDeep", _c29In2 -> _c29In2.getDeep()).get();
		}
		return null;
	}
	
}
