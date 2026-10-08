package route.fixture;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import route.fixture.meta.RouteBaseMeta;

import static java.util.Optional.ofNullable;

/**
 * the DATA_RULE, TYPE_FORMAT, CARDINALITY and metafield witness in one type.
 * @version 1.0.0
 */
@RosettaDataType(value="RouteBase", builder=RouteBase.RouteBaseBuilderImpl.class, version="1.0.0")
@RuneDataType(value="RouteBase", model="route", builder=RouteBase.RouteBaseBuilderImpl.class, version="1.0.0")
public interface RouteBase extends RosettaModelObject {

	RouteBaseMeta metaData = new RouteBaseMeta();

	/*********************** Getter Methods  ***********************/
	/**
	 * a type-format-bearing attribute - the TYPE_FORMAT pass.
	 */
	String getName();
	/**
	 * a multi attribute - the CARDINALITY pass.
	 */
	List<String> getNotes();
	/**
	 * a scheme-annotated attribute - the METAFIELD pass.
	 */
	FieldWithMetaString getMarket();
	RouteColour getColour();

	/*********************** Build Methods  ***********************/
	RouteBase build();
	
	RouteBase.RouteBaseBuilder toBuilder();
	
	static RouteBase.RouteBaseBuilder builder() {
		return new RouteBase.RouteBaseBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RouteBase> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RouteBase> getType() {
		return RouteBase.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
		processor.processBasic(path.newSubPath("notes"), String.class, getNotes(), this);
		processRosetta(path.newSubPath("market"), processor, FieldWithMetaString.class, getMarket());
		processor.processBasic(path.newSubPath("colour"), RouteColour.class, getColour(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RouteBaseBuilder extends RouteBase, RosettaModelObjectBuilder {
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateMarket();
		@Override
		FieldWithMetaString.FieldWithMetaStringBuilder getMarket();
		RouteBase.RouteBaseBuilder setName(String name);
		RouteBase.RouteBaseBuilder addNotes(String notes);
		RouteBase.RouteBaseBuilder addNotes(String notes, int idx);
		RouteBase.RouteBaseBuilder addNotes(List<String> notes);
		RouteBase.RouteBaseBuilder setNotes(List<String> notes);
		RouteBase.RouteBaseBuilder setMarket(FieldWithMetaString market);
		RouteBase.RouteBaseBuilder setMarketValue(String market);
		RouteBase.RouteBaseBuilder setColour(RouteColour colour);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
			processor.processBasic(path.newSubPath("notes"), String.class, getNotes(), this);
			processRosetta(path.newSubPath("market"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getMarket());
			processor.processBasic(path.newSubPath("colour"), RouteColour.class, getColour(), this);
		}
		

		RouteBase.RouteBaseBuilder prune();
	}

	/*********************** Immutable Implementation of RouteBase  ***********************/
	class RouteBaseImpl implements RouteBase {
		private final String name;
		private final List<String> notes;
		private final FieldWithMetaString market;
		private final RouteColour colour;
		
		protected RouteBaseImpl(RouteBase.RouteBaseBuilder builder) {
			this.name = builder.getName();
			this.notes = ofNullable(builder.getNotes()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.market = ofNullable(builder.getMarket()).map(f->f.build()).orElse(null);
			this.colour = builder.getColour();
		}
		
		@Override
		@RosettaAttribute("name")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("name")
		public String getName() {
			return name;
		}
		
		@Override
		@RosettaAttribute("notes")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("notes")
		public List<String> getNotes() {
			return notes;
		}
		
		@Override
		@RosettaAttribute("market")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("market")
		public FieldWithMetaString getMarket() {
			return market;
		}
		
		@Override
		@RosettaAttribute("colour")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("colour")
		public RouteColour getColour() {
			return colour;
		}
		
		@Override
		public RouteBase build() {
			return this;
		}
		
		@Override
		public RouteBase.RouteBaseBuilder toBuilder() {
			RouteBase.RouteBaseBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RouteBase.RouteBaseBuilder builder) {
			ofNullable(getName()).ifPresent(builder::setName);
			ofNullable(getNotes()).ifPresent(builder::setNotes);
			ofNullable(getMarket()).ifPresent(builder::setMarket);
			ofNullable(getColour()).ifPresent(builder::setColour);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RouteBase _that = getType().cast(o);
		
			if (!Objects.equals(name, _that.getName())) return false;
			if (!ListEquals.listEquals(notes, _that.getNotes())) return false;
			if (!Objects.equals(market, _that.getMarket())) return false;
			if (!Objects.equals(colour, _that.getColour())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			_result = 31 * _result + (notes != null ? notes.hashCode() : 0);
			_result = 31 * _result + (market != null ? market.hashCode() : 0);
			_result = 31 * _result + (colour != null ? colour.getClass().getName().hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RouteBase {" +
				"name=" + this.name + ", " +
				"notes=" + this.notes + ", " +
				"market=" + this.market + ", " +
				"colour=" + this.colour +
			'}';
		}
	}

	/*********************** Builder Implementation of RouteBase  ***********************/
	class RouteBaseBuilderImpl implements RouteBase.RouteBaseBuilder {
	
		protected String name;
		protected List<String> notes = new ArrayList<>();
		protected FieldWithMetaString.FieldWithMetaStringBuilder market;
		protected RouteColour colour;
		
		@Override
		@RosettaAttribute("name")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("name")
		public String getName() {
			return name;
		}
		
		@Override
		@RosettaAttribute("notes")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("notes")
		public List<String> getNotes() {
			return notes;
		}
		
		@Override
		@RosettaAttribute("market")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("market")
		public FieldWithMetaString.FieldWithMetaStringBuilder getMarket() {
			return market;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateMarket() {
			FieldWithMetaString.FieldWithMetaStringBuilder result;
			if (market!=null) {
				result = market;
			}
			else {
				result = market = FieldWithMetaString.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("colour")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("colour")
		public RouteColour getColour() {
			return colour;
		}
		
		@RosettaAttribute("name")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("name")
		@Override
		public RouteBase.RouteBaseBuilder setName(String _name) {
			this.name = _name == null ? null : _name;
			return this;
		}
		
		@RosettaAttribute("notes")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("notes")
		@Override
		public RouteBase.RouteBaseBuilder addNotes(String _notes) {
			if (_notes != null) {
				this.notes.add(_notes);
			}
			return this;
		}
		
		@Override
		public RouteBase.RouteBaseBuilder addNotes(String _notes, int idx) {
			getIndex(this.notes, idx, () -> _notes);
			return this;
		}
		
		@Override
		public RouteBase.RouteBaseBuilder addNotes(List<String> notess) {
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
		public RouteBase.RouteBaseBuilder setNotes(List<String> notess) {
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
		public RouteBase.RouteBaseBuilder setMarket(FieldWithMetaString _market) {
			this.market = _market == null ? null : _market.toBuilder();
			return this;
		}
		
		@Override
		public RouteBase.RouteBaseBuilder setMarketValue(String _market) {
			this.getOrCreateMarket().setValue(_market);
			return this;
		}
		
		@RosettaAttribute("colour")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("colour")
		@Override
		public RouteBase.RouteBaseBuilder setColour(RouteColour _colour) {
			this.colour = _colour == null ? null : _colour;
			return this;
		}
		
		@Override
		public RouteBase build() {
			return new RouteBase.RouteBaseImpl(this);
		}
		
		@Override
		public RouteBase.RouteBaseBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RouteBase.RouteBaseBuilder prune() {
			if (market!=null && !market.prune().hasData()) market = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getName()!=null) return true;
			if (getNotes()!=null && !getNotes().isEmpty()) return true;
			if (getMarket()!=null) return true;
			if (getColour()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RouteBase.RouteBaseBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RouteBase.RouteBaseBuilder o = (RouteBase.RouteBaseBuilder) other;
			
			merger.mergeRosetta(getMarket(), o.getMarket(), this::setMarket);
			
			merger.mergeBasic(getName(), o.getName(), this::setName);
			merger.mergeBasic(getNotes(), o.getNotes(), (Consumer<String>) this::addNotes);
			merger.mergeBasic(getColour(), o.getColour(), this::setColour);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RouteBase _that = getType().cast(o);
		
			if (!Objects.equals(name, _that.getName())) return false;
			if (!ListEquals.listEquals(notes, _that.getNotes())) return false;
			if (!Objects.equals(market, _that.getMarket())) return false;
			if (!Objects.equals(colour, _that.getColour())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			_result = 31 * _result + (notes != null ? notes.hashCode() : 0);
			_result = 31 * _result + (market != null ? market.hashCode() : 0);
			_result = 31 * _result + (colour != null ? colour.getClass().getName().hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RouteBaseBuilder {" +
				"name=" + this.name + ", " +
				"notes=" + this.notes + ", " +
				"market=" + this.market + ", " +
				"colour=" + this.colour +
			'}';
		}
	}
}
