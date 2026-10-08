package test.dispatchns.b;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Dispatch discriminator, namespace b.
 * @version 0.0.0
 */
@RosettaEnum("ModeB")
public enum ModeB {

	@RosettaEnumValue(value = "Fast") 
	FAST("Fast", null),
	
	@RosettaEnumValue(value = "Slow") 
	SLOW("Slow", null)
;
	private static Map<String, ModeB> values;
	static {
        Map<String, ModeB> map = new ConcurrentHashMap<>();
		for (ModeB instance : ModeB.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	ModeB(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static ModeB fromDisplayName(String name) {
		ModeB value = values.get(name);
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
