package chaos.s29.a9tabs.validation.exists;

import chaos.s29.a9tabs.C29Inner;
import chaos.s29.a9tabs.C29Leaf;
import chaos.s29.a9tabs.C29Note;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C29NoteOnlyExistsValidator implements ValidatorWithArg<C29Note, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C29Note> ValidationResult<C29Note> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("text", ExistenceChecker.isSet((String) o.getText()))
				.put("leaves", ExistenceChecker.isSet((List<? extends C29Leaf>) o.getLeaves()))
				.put("code", ExistenceChecker.isSet((FieldWithMetaString) o.getCode()))
				.put("codes", ExistenceChecker.isSet((List<? extends FieldWithMetaString>) o.getCodes()))
				.put("inners", ExistenceChecker.isSet((List<? extends C29Inner>) o.getInners()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C29Note", ValidationResult.ValidationType.ONLY_EXISTS, "C29Note", path, "");
		}
		return failure("C29Note", ValidationResult.ValidationType.ONLY_EXISTS, "C29Note", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
