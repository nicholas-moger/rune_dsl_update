package holdout.rulemetaoutputunwrap.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.model.metafields.FieldWithMetaString;
import holdout.rulemetaoutputunwrap.Trade;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class TradeOnlyExistsValidator implements ValidatorWithArg<Trade, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Trade> ValidationResult<Trade> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("utid", ExistenceChecker.isSet((String) o.getUtid()))
				.put("venue", ExistenceChecker.isSet((FieldWithMetaString) o.getVenue()))
				.put("venues", ExistenceChecker.isSet((List<? extends FieldWithMetaString>) o.getVenues()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Trade", ValidationResult.ValidationType.ONLY_EXISTS, "Trade", path, "");
		}
		return failure("Trade", ValidationResult.ValidationType.ONLY_EXISTS, "Trade", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
