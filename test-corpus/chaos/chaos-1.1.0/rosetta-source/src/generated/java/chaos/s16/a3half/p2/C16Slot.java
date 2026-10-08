package chaos.s16.a3half.p2;

import chaos.s16.a3half.p1.C16Alpha;
import chaos.s16.a3half.p1.C16Held;
import chaos.s16.a3half.p2.meta.C16SlotMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Resolves C16Alpha at an attribute type seat.
 * @version 1.0.0
 */
@RosettaDataType(value="C16Slot", builder=C16Slot.C16SlotBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C16Slot", model="chaos", builder=C16Slot.C16SlotBuilderImpl.class, version="1.0.0")
public interface C16Slot extends RosettaModelObject {

	C16SlotMeta metaData = new C16SlotMeta();

	/*********************** Getter Methods  ***********************/
	C16Alpha getFirstPick();
	C16Held getHeld();

	/*********************** Build Methods  ***********************/
	C16Slot build();
	
	C16Slot.C16SlotBuilder toBuilder();
	
	static C16Slot.C16SlotBuilder builder() {
		return new C16Slot.C16SlotBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C16Slot> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C16Slot> getType() {
		return C16Slot.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("firstPick"), processor, C16Alpha.class, getFirstPick());
		processRosetta(path.newSubPath("held"), processor, C16Held.class, getHeld());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C16SlotBuilder extends C16Slot, RosettaModelObjectBuilder {
		C16Alpha.C16AlphaBuilder getOrCreateFirstPick();
		@Override
		C16Alpha.C16AlphaBuilder getFirstPick();
		C16Held.C16HeldBuilder getOrCreateHeld();
		@Override
		C16Held.C16HeldBuilder getHeld();
		C16Slot.C16SlotBuilder setFirstPick(C16Alpha firstPick);
		C16Slot.C16SlotBuilder setHeld(C16Held held);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("firstPick"), processor, C16Alpha.C16AlphaBuilder.class, getFirstPick());
			processRosetta(path.newSubPath("held"), processor, C16Held.C16HeldBuilder.class, getHeld());
		}
		

		C16Slot.C16SlotBuilder prune();
	}

	/*********************** Immutable Implementation of C16Slot  ***********************/
	class C16SlotImpl implements C16Slot {
		private final C16Alpha firstPick;
		private final C16Held held;
		
		protected C16SlotImpl(C16Slot.C16SlotBuilder builder) {
			this.firstPick = ofNullable(builder.getFirstPick()).map(f->f.build()).orElse(null);
			this.held = ofNullable(builder.getHeld()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("firstPick")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("firstPick")
		public C16Alpha getFirstPick() {
			return firstPick;
		}
		
		@Override
		@RosettaAttribute("held")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("held")
		public C16Held getHeld() {
			return held;
		}
		
		@Override
		public C16Slot build() {
			return this;
		}
		
		@Override
		public C16Slot.C16SlotBuilder toBuilder() {
			C16Slot.C16SlotBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C16Slot.C16SlotBuilder builder) {
			ofNullable(getFirstPick()).ifPresent(builder::setFirstPick);
			ofNullable(getHeld()).ifPresent(builder::setHeld);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C16Slot _that = getType().cast(o);
		
			if (!Objects.equals(firstPick, _that.getFirstPick())) return false;
			if (!Objects.equals(held, _that.getHeld())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (firstPick != null ? firstPick.hashCode() : 0);
			_result = 31 * _result + (held != null ? held.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C16Slot {" +
				"firstPick=" + this.firstPick + ", " +
				"held=" + this.held +
			'}';
		}
	}

	/*********************** Builder Implementation of C16Slot  ***********************/
	class C16SlotBuilderImpl implements C16Slot.C16SlotBuilder {
	
		protected C16Alpha.C16AlphaBuilder firstPick;
		protected C16Held.C16HeldBuilder held;
		
		@Override
		@RosettaAttribute("firstPick")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("firstPick")
		public C16Alpha.C16AlphaBuilder getFirstPick() {
			return firstPick;
		}
		
		@Override
		public C16Alpha.C16AlphaBuilder getOrCreateFirstPick() {
			C16Alpha.C16AlphaBuilder result;
			if (firstPick!=null) {
				result = firstPick;
			}
			else {
				result = firstPick = C16Alpha.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("held")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("held")
		public C16Held.C16HeldBuilder getHeld() {
			return held;
		}
		
		@Override
		public C16Held.C16HeldBuilder getOrCreateHeld() {
			C16Held.C16HeldBuilder result;
			if (held!=null) {
				result = held;
			}
			else {
				result = held = C16Held.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("firstPick")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("firstPick")
		@Override
		public C16Slot.C16SlotBuilder setFirstPick(C16Alpha _firstPick) {
			this.firstPick = _firstPick == null ? null : _firstPick.toBuilder();
			return this;
		}
		
		@RosettaAttribute("held")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("held")
		@Override
		public C16Slot.C16SlotBuilder setHeld(C16Held _held) {
			this.held = _held == null ? null : _held.toBuilder();
			return this;
		}
		
		@Override
		public C16Slot build() {
			return new C16Slot.C16SlotImpl(this);
		}
		
		@Override
		public C16Slot.C16SlotBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C16Slot.C16SlotBuilder prune() {
			if (firstPick!=null && !firstPick.prune().hasData()) firstPick = null;
			if (held!=null && !held.prune().hasData()) held = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getFirstPick()!=null && getFirstPick().hasData()) return true;
			if (getHeld()!=null && getHeld().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C16Slot.C16SlotBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C16Slot.C16SlotBuilder o = (C16Slot.C16SlotBuilder) other;
			
			merger.mergeRosetta(getFirstPick(), o.getFirstPick(), this::setFirstPick);
			merger.mergeRosetta(getHeld(), o.getHeld(), this::setHeld);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C16Slot _that = getType().cast(o);
		
			if (!Objects.equals(firstPick, _that.getFirstPick())) return false;
			if (!Objects.equals(held, _that.getHeld())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (firstPick != null ? firstPick.hashCode() : 0);
			_result = 31 * _result + (held != null ? held.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C16SlotBuilder {" +
				"firstPick=" + this.firstPick + ", " +
				"held=" + this.held +
			'}';
		}
	}
}
