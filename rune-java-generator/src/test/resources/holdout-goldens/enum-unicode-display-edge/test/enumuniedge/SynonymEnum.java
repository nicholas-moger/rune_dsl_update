package test.enumuniedge;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import com.rosetta.model.lib.annotations.RosettaSynonym;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A non-ASCII synonym value on an enum value beside a non-ASCII display name.
 * @version 1.0.0
 */
@RosettaEnum("SynonymEnum")
public enum SynonymEnum {

	@RosettaSynonym(value = "µ–syn", source = "EdgeSrc")
	@RosettaEnumValue(value = "S", displayName = "µ") 
	S("S", "\u00B5"),
	
	@RosettaSynonym(value = "plain", source = "EdgeSrc")
	@RosettaEnumValue(value = "T", displayName = "tee") 
	T("T", "tee")
;
	private static Map<String, SynonymEnum> values;
	static {
        Map<String, SynonymEnum> map = new ConcurrentHashMap<>();
		for (SynonymEnum instance : SynonymEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	SynonymEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static SynonymEnum fromDisplayName(String name) {
		SynonymEnum value = values.get(name);
		if (value == null) {
			throw new IllegalArgumentException("No enum constant with display name \"" + name + "\".");
		}
		return value;
	}

	@Override
	public String toString() {
		return toDisplayString();
	}

	public String toDisplayString() {
		return displayName != null ?  displayName : rosettaName;
	}
}
