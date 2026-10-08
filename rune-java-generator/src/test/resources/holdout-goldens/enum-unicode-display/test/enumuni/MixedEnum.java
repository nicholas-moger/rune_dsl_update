package test.enumuni;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * ASCII and non-ASCII display names beside each other, and a value without one.
 * @version 1.0.0
 */
@RosettaEnum("MixedEnum")
public enum MixedEnum {

	@RosettaEnumValue(value = "Plain") 
	PLAIN("Plain", null),
	
	@RosettaEnumValue(value = "Ascii", displayName = "Ascii Display") 
	ASCII("Ascii", "Ascii Display"),
	
	@RosettaEnumValue(value = "Micro", displayName = "µ") 
	MICRO("Micro", "\u00B5"),
	
	@RosettaEnumValue(value = "Dash", displayName = "en–dash") 
	DASH("Dash", "en\u2013dash"),
	
	@RosettaEnumValue(value = "Quotes", displayName = "„low“ quotes") 
	QUOTES("Quotes", "\u201Elow\u201C quotes"),
	
	@RosettaEnumValue(value = "Cjk", displayName = "試験") 
	CJK("Cjk", "\u8A66\u9A13")
;
	private static Map<String, MixedEnum> values;
	static {
        Map<String, MixedEnum> map = new ConcurrentHashMap<>();
		for (MixedEnum instance : MixedEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	MixedEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static MixedEnum fromDisplayName(String name) {
		MixedEnum value = values.get(name);
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
