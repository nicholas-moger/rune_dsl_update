package test.enumuni;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * displayName probe - the chaos a5uni shape verbatim.
 * @version 1.0.0
 */
@RosettaEnum("ProbeEnum")
public enum ProbeEnum {

	@RosettaEnumValue(value = "V", displayName = "µ–„display“") 
	V("V", "\u00B5\u2013\u201Edisplay\u201C")
;
	private static Map<String, ProbeEnum> values;
	static {
        Map<String, ProbeEnum> map = new ConcurrentHashMap<>();
		for (ProbeEnum instance : ProbeEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	ProbeEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static ProbeEnum fromDisplayName(String name) {
		ProbeEnum value = values.get(name);
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
