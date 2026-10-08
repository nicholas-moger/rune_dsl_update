package chaos.s06.a2alias;

import chaos.s06.a2alias.h.C6Tag;
import chaos.s06.a2alias.meta.C6EventMeta;
import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.util.ListEquals;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * The rule-from type; references the helper; carries a meta seat for the rule x meta conjunction.
 * @version 1.0.0
 */
@RosettaDataType(value="C6Event", builder=C6Event.C6EventBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C6Event", model="chaos", builder=C6Event.C6EventBuilderImpl.class, version="1.0.0")
public interface C6Event extends RosettaModelObject {

	C6EventMeta metaData = new C6EventMeta();

	/*********************** Getter Methods  ***********************/
	String getEventId();
	BigDecimal getQty();
	List<String> getLegs();
	C6Tag getMark();
	FieldWithMetaString getVenue();

	/*********************** Build Methods  ***********************/
	C6Event build();
	
	C6Event.C6EventBuilder toBuilder();
	
	static C6Event.C6EventBuilder builder() {
		return new C6Event.C6EventBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C6Event> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C6Event> getType() {
		return C6Event.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("eventId"), String.class, getEventId(), this);
		processor.processBasic(path.newSubPath("qty"), BigDecimal.class, getQty(), this);
		processor.processBasic(path.newSubPath("legs"), String.class, getLegs(), this);
		processRosetta(path.newSubPath("mark"), processor, C6Tag.class, getMark());
		processRosetta(path.newSubPath("venue"), processor, FieldWithMetaString.class, getVenue());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C6EventBuilder extends C6Event, RosettaModelObjectBuilder {
		C6Tag.C6TagBuilder getOrCreateMark();
		@Override
		C6Tag.C6TagBuilder getMark();
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateVenue();
		@Override
		FieldWithMetaString.FieldWithMetaStringBuilder getVenue();
		C6Event.C6EventBuilder setEventId(String eventId);
		C6Event.C6EventBuilder setQty(BigDecimal qty);
		C6Event.C6EventBuilder addLegs(String legs);
		C6Event.C6EventBuilder addLegs(String legs, int idx);
		C6Event.C6EventBuilder addLegs(List<String> legs);
		C6Event.C6EventBuilder setLegs(List<String> legs);
		C6Event.C6EventBuilder setMark(C6Tag mark);
		C6Event.C6EventBuilder setVenue(FieldWithMetaString venue);
		C6Event.C6EventBuilder setVenueValue(String venue);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("eventId"), String.class, getEventId(), this);
			processor.processBasic(path.newSubPath("qty"), BigDecimal.class, getQty(), this);
			processor.processBasic(path.newSubPath("legs"), String.class, getLegs(), this);
			processRosetta(path.newSubPath("mark"), processor, C6Tag.C6TagBuilder.class, getMark());
			processRosetta(path.newSubPath("venue"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getVenue());
		}
		

		C6Event.C6EventBuilder prune();
	}

	/*********************** Immutable Implementation of C6Event  ***********************/
	class C6EventImpl implements C6Event {
		private final String eventId;
		private final BigDecimal qty;
		private final List<String> legs;
		private final C6Tag mark;
		private final FieldWithMetaString venue;
		
		protected C6EventImpl(C6Event.C6EventBuilder builder) {
			this.eventId = builder.getEventId();
			this.qty = builder.getQty();
			this.legs = ofNullable(builder.getLegs()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.mark = ofNullable(builder.getMark()).map(f->f.build()).orElse(null);
			this.venue = ofNullable(builder.getVenue()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("eventId")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("eventId")
		public String getEventId() {
			return eventId;
		}
		
		@Override
		@RosettaAttribute("qty")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("qty")
		public BigDecimal getQty() {
			return qty;
		}
		
		@Override
		@RosettaAttribute("legs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("legs")
		public List<String> getLegs() {
			return legs;
		}
		
		@Override
		@RosettaAttribute("mark")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("mark")
		public C6Tag getMark() {
			return mark;
		}
		
		@Override
		@RosettaAttribute("venue")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("venue")
		public FieldWithMetaString getVenue() {
			return venue;
		}
		
		@Override
		public C6Event build() {
			return this;
		}
		
		@Override
		public C6Event.C6EventBuilder toBuilder() {
			C6Event.C6EventBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C6Event.C6EventBuilder builder) {
			ofNullable(getEventId()).ifPresent(builder::setEventId);
			ofNullable(getQty()).ifPresent(builder::setQty);
			ofNullable(getLegs()).ifPresent(builder::setLegs);
			ofNullable(getMark()).ifPresent(builder::setMark);
			ofNullable(getVenue()).ifPresent(builder::setVenue);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C6Event _that = getType().cast(o);
		
			if (!Objects.equals(eventId, _that.getEventId())) return false;
			if (!Objects.equals(qty, _that.getQty())) return false;
			if (!ListEquals.listEquals(legs, _that.getLegs())) return false;
			if (!Objects.equals(mark, _that.getMark())) return false;
			if (!Objects.equals(venue, _that.getVenue())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (eventId != null ? eventId.hashCode() : 0);
			_result = 31 * _result + (qty != null ? qty.hashCode() : 0);
			_result = 31 * _result + (legs != null ? legs.hashCode() : 0);
			_result = 31 * _result + (mark != null ? mark.hashCode() : 0);
			_result = 31 * _result + (venue != null ? venue.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C6Event {" +
				"eventId=" + this.eventId + ", " +
				"qty=" + this.qty + ", " +
				"legs=" + this.legs + ", " +
				"mark=" + this.mark + ", " +
				"venue=" + this.venue +
			'}';
		}
	}

	/*********************** Builder Implementation of C6Event  ***********************/
	class C6EventBuilderImpl implements C6Event.C6EventBuilder {
	
		protected String eventId;
		protected BigDecimal qty;
		protected List<String> legs = new ArrayList<>();
		protected C6Tag.C6TagBuilder mark;
		protected FieldWithMetaString.FieldWithMetaStringBuilder venue;
		
		@Override
		@RosettaAttribute("eventId")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("eventId")
		public String getEventId() {
			return eventId;
		}
		
		@Override
		@RosettaAttribute("qty")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("qty")
		public BigDecimal getQty() {
			return qty;
		}
		
		@Override
		@RosettaAttribute("legs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("legs")
		public List<String> getLegs() {
			return legs;
		}
		
		@Override
		@RosettaAttribute("mark")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("mark")
		public C6Tag.C6TagBuilder getMark() {
			return mark;
		}
		
		@Override
		public C6Tag.C6TagBuilder getOrCreateMark() {
			C6Tag.C6TagBuilder result;
			if (mark!=null) {
				result = mark;
			}
			else {
				result = mark = C6Tag.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("venue")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("venue")
		public FieldWithMetaString.FieldWithMetaStringBuilder getVenue() {
			return venue;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateVenue() {
			FieldWithMetaString.FieldWithMetaStringBuilder result;
			if (venue!=null) {
				result = venue;
			}
			else {
				result = venue = FieldWithMetaString.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("eventId")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("eventId")
		@Override
		public C6Event.C6EventBuilder setEventId(String _eventId) {
			this.eventId = _eventId == null ? null : _eventId;
			return this;
		}
		
		@RosettaAttribute("qty")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("qty")
		@Override
		public C6Event.C6EventBuilder setQty(BigDecimal _qty) {
			this.qty = _qty == null ? null : _qty;
			return this;
		}
		
		@RosettaAttribute("legs")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("legs")
		@Override
		public C6Event.C6EventBuilder addLegs(String _legs) {
			if (_legs != null) {
				this.legs.add(_legs);
			}
			return this;
		}
		
		@Override
		public C6Event.C6EventBuilder addLegs(String _legs, int idx) {
			getIndex(this.legs, idx, () -> _legs);
			return this;
		}
		
		@Override
		public C6Event.C6EventBuilder addLegs(List<String> legss) {
			if (legss != null) {
				for (final String toAdd : legss) {
					this.legs.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("legs")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("legs")
		@Override
		public C6Event.C6EventBuilder setLegs(List<String> legss) {
			if (legss == null) {
				this.legs = new ArrayList<>();
			} else {
				this.legs = legss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("mark")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("mark")
		@Override
		public C6Event.C6EventBuilder setMark(C6Tag _mark) {
			this.mark = _mark == null ? null : _mark.toBuilder();
			return this;
		}
		
		@RosettaAttribute("venue")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("venue")
		@Override
		public C6Event.C6EventBuilder setVenue(FieldWithMetaString _venue) {
			this.venue = _venue == null ? null : _venue.toBuilder();
			return this;
		}
		
		@Override
		public C6Event.C6EventBuilder setVenueValue(String _venue) {
			this.getOrCreateVenue().setValue(_venue);
			return this;
		}
		
		@Override
		public C6Event build() {
			return new C6Event.C6EventImpl(this);
		}
		
		@Override
		public C6Event.C6EventBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C6Event.C6EventBuilder prune() {
			if (mark!=null && !mark.prune().hasData()) mark = null;
			if (venue!=null && !venue.prune().hasData()) venue = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getEventId()!=null) return true;
			if (getQty()!=null) return true;
			if (getLegs()!=null && !getLegs().isEmpty()) return true;
			if (getMark()!=null && getMark().hasData()) return true;
			if (getVenue()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C6Event.C6EventBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C6Event.C6EventBuilder o = (C6Event.C6EventBuilder) other;
			
			merger.mergeRosetta(getMark(), o.getMark(), this::setMark);
			merger.mergeRosetta(getVenue(), o.getVenue(), this::setVenue);
			
			merger.mergeBasic(getEventId(), o.getEventId(), this::setEventId);
			merger.mergeBasic(getQty(), o.getQty(), this::setQty);
			merger.mergeBasic(getLegs(), o.getLegs(), (Consumer<String>) this::addLegs);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C6Event _that = getType().cast(o);
		
			if (!Objects.equals(eventId, _that.getEventId())) return false;
			if (!Objects.equals(qty, _that.getQty())) return false;
			if (!ListEquals.listEquals(legs, _that.getLegs())) return false;
			if (!Objects.equals(mark, _that.getMark())) return false;
			if (!Objects.equals(venue, _that.getVenue())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (eventId != null ? eventId.hashCode() : 0);
			_result = 31 * _result + (qty != null ? qty.hashCode() : 0);
			_result = 31 * _result + (legs != null ? legs.hashCode() : 0);
			_result = 31 * _result + (mark != null ? mark.hashCode() : 0);
			_result = 31 * _result + (venue != null ? venue.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C6EventBuilder {" +
				"eventId=" + this.eventId + ", " +
				"qty=" + this.qty + ", " +
				"legs=" + this.legs + ", " +
				"mark=" + this.mark + ", " +
				"venue=" + this.venue +
			'}';
		}
	}
}
