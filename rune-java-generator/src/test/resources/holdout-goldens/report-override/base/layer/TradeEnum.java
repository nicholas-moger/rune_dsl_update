package base.layer;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @version 0.0.0
 */
@RosettaEnum("TradeEnum")
public enum TradeEnum {

	@RosettaEnumValue(value = "A") 
	A("A", null),
	
	@RosettaEnumValue(value = "B") 
	B("B", null),
	
	@RosettaEnumValue(value = "C") 
	C("C", null),
	
	@RosettaEnumValue(value = "D") 
	D("D", null)
;
	private static Map<String, TradeEnum> values;
	static {
        Map<String, TradeEnum> map = new ConcurrentHashMap<>();
		for (TradeEnum instance : TradeEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	TradeEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static TradeEnum fromDisplayName(String name) {
		TradeEnum value = values.get(name);
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
