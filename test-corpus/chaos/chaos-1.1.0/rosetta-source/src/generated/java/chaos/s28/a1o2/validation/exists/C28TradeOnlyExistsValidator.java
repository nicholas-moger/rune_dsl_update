package chaos.s28.a1o2.validation.exists;

import chaos.s28.a1o2.C28Extra;
import chaos.s28.a1o2.C28Trade;
import chaos.s28.a1o2.C28Which;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C28TradeOnlyExistsValidator implements ValidatorWithArg<C28Trade, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C28Trade> ValidationResult<C28Trade> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("utid", ExistenceChecker.isSet((String) o.getUtid()))
				.put("which", ExistenceChecker.isSet((C28Which) o.getWhich()))
				.put("venue", ExistenceChecker.isSet((FieldWithMetaString) o.getVenue()))
				.put("extra", ExistenceChecker.isSet((C28Extra) o.getExtra()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C28Trade", ValidationResult.ValidationType.ONLY_EXISTS, "C28Trade", path, "");
		}
		return failure("C28Trade", ValidationResult.ValidationType.ONLY_EXISTS, "C28Trade", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
