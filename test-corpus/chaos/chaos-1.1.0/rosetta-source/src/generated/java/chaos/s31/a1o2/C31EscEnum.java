package chaos.s31.a1o2;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * displayNames carrying a raw quote, a backslash and non-ASCII - the D46 RAW law (upstream&#39;s own non-compiling emission for the first two: EscapeEnum, rider 4).
 * @version 1.0.0
 */
@RosettaEnum("C31EscEnum")
public enum C31EscEnum {

	@RosettaEnumValue(value = "Quote", displayName = "say "hi"") 
	QUOTE("Quote", "say \"hi\""),
	
	@RosettaEnumValue(value = "Slash", displayName = "back\slash") 
	SLASH("Slash", "back\\slash"),
	
	@RosettaEnumValue(value = "Uni", displayName = "µ–„display“") 
	UNI("Uni", "\u00B5\u2013\u201Edisplay\u201C"),
	
	@RosettaEnumValue(value = "Plain", displayName = "plain") 
	PLAIN("Plain", "plain")
;
	private static Map<String, C31EscEnum> values;
	static {
        Map<String, C31EscEnum> map = new ConcurrentHashMap<>();
		for (C31EscEnum instance : C31EscEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C31EscEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C31EscEnum fromDisplayName(String name) {
		C31EscEnum value = values.get(name);
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
