package test.deeppath.util;

import com.rosetta.model.lib.mapper.MapperS;
import java.util.Collections;
import java.util.List;
import test.deeppath.Note;
import test.deeppath.Outer;
import test.deeppath.Wrap;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

public class OuterDeepPathUtil {
	public String chooseText(Outer outer) {
		final MapperS<Wrap> wrap = MapperS.of(outer).<Wrap>map("getWrap", _outer -> _outer.getWrap());
		if (exists(wrap).getOrDefault(false)) {
			return wrap.<String>map("getText", _wrap -> _wrap.getText()).get();
		}
		final MapperS<Note> note = MapperS.of(outer).<Note>map("getNote", _outer -> _outer.getNote());
		if (exists(note).getOrDefault(false)) {
			return note.<String>map("getText", _note -> _note.getText()).get();
		}
		return null;
	}
	
	public List<String> chooseTags(Outer outer) {
		final MapperS<Wrap> wrap = MapperS.of(outer).<Wrap>map("getWrap", _outer -> _outer.getWrap());
		if (exists(wrap).getOrDefault(false)) {
			return wrap.<String>mapC("getTags", _wrap -> _wrap.getTags()).getMulti();
		}
		final MapperS<Note> note = MapperS.of(outer).<Note>map("getNote", _outer -> _outer.getNote());
		if (exists(note).getOrDefault(false)) {
			return note.<String>mapC("getTags", _note -> _note.getTags()).getMulti();
		}
		return Collections.<String>emptyList();
	}
	
}
