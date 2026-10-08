package test.dispatchns.d;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Dispatch discriminator, namespace d.
 * @version 0.0.0
 */
@RosettaEnum("ModeD")
public enum ModeD {

	@RosettaEnumValue(value = "Fast") 
	FAST("Fast", null),
	
	@RosettaEnumValue(value = "Slow") 
	SLOW("Slow", null)
;
	private static Map<String, ModeD> values;
	static {
        Map<String, ModeD> map = new ConcurrentHashMap<>();
		for (ModeD instance : ModeD.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	ModeD(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static ModeD fromDisplayName(String name) {
		ModeD value = values.get(name);
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
