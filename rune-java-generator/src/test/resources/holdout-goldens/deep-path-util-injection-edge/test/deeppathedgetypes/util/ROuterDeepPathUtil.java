package test.deeppathedgetypes.util;

import com.rosetta.model.lib.mapper.MapperS;
import test.deeppathedgetypes.RNote;
import test.deeppathedgetypes.ROuter;
import test.deeppathedgetypes.RWrap;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

public class ROuterDeepPathUtil {
	public String chooseText(ROuter rOuter) {
		final MapperS<RWrap> rWrap = MapperS.of(rOuter).<RWrap>map("getRWrap", _rOuter -> _rOuter.getRWrap());
		if (exists(rWrap).getOrDefault(false)) {
			return rWrap.<String>map("getText", _rWrap -> _rWrap.getText()).get();
		}
		final MapperS<RNote> rNote = MapperS.of(rOuter).<RNote>map("getRNote", _rOuter -> _rOuter.getRNote());
		if (exists(rNote).getOrDefault(false)) {
			return rNote.<String>map("getText", _rNote -> _rNote.getText()).get();
		}
		return null;
	}
	
}
