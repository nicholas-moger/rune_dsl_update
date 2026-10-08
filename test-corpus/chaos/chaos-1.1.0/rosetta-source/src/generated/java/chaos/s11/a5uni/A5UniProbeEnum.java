package chaos.s11.a5uni;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * displayName probe.
 * @version 1.0.0
 */
@RosettaEnum("A5UniProbeEnum")
public enum A5UniProbeEnum {

	@RosettaEnumValue(value = "V", displayName = "µ–„display“") 
	V("V", "\u00B5\u2013\u201Edisplay\u201C")
;
	private static Map<String, A5UniProbeEnum> values;
	static {
        Map<String, A5UniProbeEnum> map = new ConcurrentHashMap<>();
		for (A5UniProbeEnum instance : A5UniProbeEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	A5UniProbeEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static A5UniProbeEnum fromDisplayName(String name) {
		A5UniProbeEnum value = values.get(name);
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
