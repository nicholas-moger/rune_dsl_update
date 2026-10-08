package route.fixture;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import route.fixture.meta.RouteChildMeta;

import static java.util.Optional.ofNullable;

/**
 * the extends witness - the inherited property surface.
 * @version 1.0.0
 */
@RosettaDataType(value="RouteChild", builder=RouteChild.RouteChildBuilderImpl.class, version="1.0.0")
@RuneDataType(value="RouteChild", model="route", builder=RouteChild.RouteChildBuilderImpl.class, version="1.0.0")
public interface RouteChild extends RouteBase {

	RouteChildMeta metaData = new RouteChildMeta();

	/*********************** Getter Methods  ***********************/
	String getExtra();

	/*********************** Build Methods  ***********************/
	RouteChild build();
	
	RouteChild.RouteChildBuilder toBuilder();
	
	static RouteChild.RouteChildBuilder builder() {
		return new RouteChild.RouteChildBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RouteChild> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RouteChild> getType() {
		return RouteChild.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
		processor.processBasic(path.newSubPath("notes"), String.class, getNotes(), this);
		processRosetta(path.newSubPath("market"), processor, FieldWithMetaString.class, getMarket());
		processor.processBasic(path.newSubPath("colour"), RouteColour.class, getColour(), this);
		processor.processBasic(path.newSubPath("extra"), String.class, getExtra(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RouteChildBuilder extends RouteChild, RouteBase.RouteBaseBuilder {
		@Override
		RouteChild.RouteChildBuilder setName(String name);
		@Override
		RouteChild.RouteChildBuilder addNotes(String notes);
		@Override
		RouteChild.RouteChildBuilder addNotes(String notes, int idx);
		@Override
		RouteChild.RouteChildBuilder addNotes(List<String> notes);
		@Override
		RouteChild.RouteChildBuilder setNotes(List<String> notes);
		@Override
		RouteChild.RouteChildBuilder setMarket(FieldWithMetaString market);
		@Override
		RouteChild.RouteChildBuilder setMarketValue(String market);
		@Override
		RouteChild.RouteChildBuilder setColour(RouteColour colour);
		RouteChild.RouteChildBuilder setExtra(String extra);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
			processor.processBasic(path.newSubPath("notes"), String.class, getNotes(), this);
			processRosetta(path.newSubPath("market"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getMarket());
			processor.processBasic(path.newSubPath("colour"), RouteColour.class, getColour(), this);
			processor.processBasic(path.newSubPath("extra"), String.class, getExtra(), this);
		}
		

		RouteChild.RouteChildBuilder prune();
	}

	/*********************** Immutable Implementation of RouteChild  ***********************/
	class RouteChildImpl extends RouteBase.RouteBaseImpl implements RouteChild {
		private final String extra;
		
		protected RouteChildImpl(RouteChild.RouteChildBuilder builder) {
			super(builder);
			this.extra = builder.getExtra();
		}
		
		@Override
		@RosettaAttribute("extra")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("extra")
		public String getExtra() {
			return extra;
		}
		
		@Override
		public RouteChild build() {
			return this;
		}
		
		@Override
		public RouteChild.RouteChildBuilder toBuilder() {
			RouteChild.RouteChildBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RouteChild.RouteChildBuilder builder) {
			super.setBuilderFields(builder);
			ofNullable(getExtra()).ifPresent(builder::setExtra);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
			RouteChild _that = getType().cast(o);
		
			if (!Objects.equals(extra, _that.getExtra())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			_result = 31 * _result + (extra != null ? extra.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RouteChild {" +
				"extra=" + this.extra +
			'}' + " " + super.toString();
		}
	}

	/*********************** Builder Implementation of RouteChild  ***********************/
	class RouteChildBuilderImpl extends RouteBase.RouteBaseBuilderImpl implements RouteChild.RouteChildBuilder {
	
		protected String extra;
		
		@Override
		@RosettaAttribute("extra")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("extra")
		public String getExtra() {
			return extra;
		}
		
		@RosettaAttribute("name")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("name")
		@Override
		public RouteChild.RouteChildBuilder setName(String _name) {
			this.name = _name == null ? null : _name;
			return this;
		}
		
		@RosettaAttribute("notes")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("notes")
		@Override
		public RouteChild.RouteChildBuilder addNotes(String _notes) {
			if (_notes != null) {
				this.notes.add(_notes);
			}
			return this;
		}
		
		@Override
		public RouteChild.RouteChildBuilder addNotes(String _notes, int idx) {
			getIndex(this.notes, idx, () -> _notes);
			return this;
		}
		
		@Override
		public RouteChild.RouteChildBuilder addNotes(List<String> notess) {
			if (notess != null) {
				for (final String toAdd : notess) {
					this.notes.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("notes")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("notes")
		@Override
		public RouteChild.RouteChildBuilder setNotes(List<String> notess) {
			if (notess == null) {
				this.notes = new ArrayList<>();
			} else {
				this.notes = notess.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("market")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("market")
		@Override
		public RouteChild.RouteChildBuilder setMarket(FieldWithMetaString _market) {
			this.market = _market == null ? null : _market.toBuilder();
			return this;
		}
		
		@Override
		public RouteChild.RouteChildBuilder setMarketValue(String _market) {
			this.getOrCreateMarket().setValue(_market);
			return this;
		}
		
		@RosettaAttribute("colour")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("colour")
		@Override
		public RouteChild.RouteChildBuilder setColour(RouteColour _colour) {
			this.colour = _colour == null ? null : _colour;
			return this;
		}
		
		@RosettaAttribute("extra")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("extra")
		@Override
		public RouteChild.RouteChildBuilder setExtra(String _extra) {
			this.extra = _extra == null ? null : _extra;
			return this;
		}
		
		@Override
		public RouteChild build() {
			return new RouteChild.RouteChildImpl(this);
		}
		
		@Override
		public RouteChild.RouteChildBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RouteChild.RouteChildBuilder prune() {
			super.prune();
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (super.hasData()) return true;
			if (getExtra()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RouteChild.RouteChildBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			super.merge(other, merger);
			RouteChild.RouteChildBuilder o = (RouteChild.RouteChildBuilder) other;
			
			
			merger.mergeBasic(getExtra(), o.getExtra(), this::setExtra);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
			RouteChild _that = getType().cast(o);
		
			if (!Objects.equals(extra, _that.getExtra())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			_result = 31 * _result + (extra != null ? extra.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RouteChildBuilder {" +
				"extra=" + this.extra +
			'}' + " " + super.toString();
		}
	}
}
