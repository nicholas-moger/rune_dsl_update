package holdout.rulemetaoutputunwrap;

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
import holdout.rulemetaoutputunwrap.meta.TradeMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * The M8a carrier&#39;s root (v3.2 seat 12, D52 H2): a scheme-carrying scalar beside a plain one.
 * @version 0.0.0
 */
@RosettaDataType(value="Trade", builder=Trade.TradeBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Trade", model="holdout", builder=Trade.TradeBuilderImpl.class, version="0.0.0")
public interface Trade extends RosettaModelObject {

	TradeMeta metaData = new TradeMeta();

	/*********************** Getter Methods  ***********************/
	String getUtid();
	FieldWithMetaString getVenue();
	List<? extends FieldWithMetaString> getVenues();

	/*********************** Build Methods  ***********************/
	Trade build();
	
	Trade.TradeBuilder toBuilder();
	
	static Trade.TradeBuilder builder() {
		return new Trade.TradeBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Trade> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Trade> getType() {
		return Trade.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("utid"), String.class, getUtid(), this);
		processRosetta(path.newSubPath("venue"), processor, FieldWithMetaString.class, getVenue());
		processRosetta(path.newSubPath("venues"), processor, FieldWithMetaString.class, getVenues());
	}
	

	/*********************** Builder Interface  ***********************/
	interface TradeBuilder extends Trade, RosettaModelObjectBuilder {
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateVenue();
		@Override
		FieldWithMetaString.FieldWithMetaStringBuilder getVenue();
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateVenues(int index);
		@Override
		List<? extends FieldWithMetaString.FieldWithMetaStringBuilder> getVenues();
		Trade.TradeBuilder setUtid(String utid);
		Trade.TradeBuilder setVenue(FieldWithMetaString venue);
		Trade.TradeBuilder setVenueValue(String venue);
		Trade.TradeBuilder addVenues(FieldWithMetaString venues);
		Trade.TradeBuilder addVenues(FieldWithMetaString venues, int idx);
		Trade.TradeBuilder addVenuesValue(String venues);
		Trade.TradeBuilder addVenuesValue(String venues, int idx);
		Trade.TradeBuilder addVenues(List<? extends FieldWithMetaString> venues);
		Trade.TradeBuilder setVenues(List<? extends FieldWithMetaString> venues);
		Trade.TradeBuilder addVenuesValue(List<? extends String> venues);
		Trade.TradeBuilder setVenuesValue(List<? extends String> venues);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("utid"), String.class, getUtid(), this);
			processRosetta(path.newSubPath("venue"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getVenue());
			processRosetta(path.newSubPath("venues"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getVenues());
		}
		

		Trade.TradeBuilder prune();
	}

	/*********************** Immutable Implementation of Trade  ***********************/
	class TradeImpl implements Trade {
		private final String utid;
		private final FieldWithMetaString venue;
		private final List<? extends FieldWithMetaString> venues;
		
		protected TradeImpl(Trade.TradeBuilder builder) {
			this.utid = builder.getUtid();
			this.venue = ofNullable(builder.getVenue()).map(f->f.build()).orElse(null);
			this.venues = ofNullable(builder.getVenues()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("utid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("utid")
		public String getUtid() {
			return utid;
		}
		
		@Override
		@RosettaAttribute("venue")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("venue")
		public FieldWithMetaString getVenue() {
			return venue;
		}
		
		@Override
		@RosettaAttribute("venues")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("venues")
		public List<? extends FieldWithMetaString> getVenues() {
			return venues;
		}
		
		@Override
		public Trade build() {
			return this;
		}
		
		@Override
		public Trade.TradeBuilder toBuilder() {
			Trade.TradeBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Trade.TradeBuilder builder) {
			ofNullable(getUtid()).ifPresent(builder::setUtid);
			ofNullable(getVenue()).ifPresent(builder::setVenue);
			ofNullable(getVenues()).ifPresent(builder::setVenues);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Trade _that = getType().cast(o);
		
			if (!Objects.equals(utid, _that.getUtid())) return false;
			if (!Objects.equals(venue, _that.getVenue())) return false;
			if (!ListEquals.listEquals(venues, _that.getVenues())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (utid != null ? utid.hashCode() : 0);
			_result = 31 * _result + (venue != null ? venue.hashCode() : 0);
			_result = 31 * _result + (venues != null ? venues.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Trade {" +
				"utid=" + this.utid + ", " +
				"venue=" + this.venue + ", " +
				"venues=" + this.venues +
			'}';
		}
	}

	/*********************** Builder Implementation of Trade  ***********************/
	class TradeBuilderImpl implements Trade.TradeBuilder {
	
		protected String utid;
		protected FieldWithMetaString.FieldWithMetaStringBuilder venue;
		protected List<FieldWithMetaString.FieldWithMetaStringBuilder> venues = new ArrayList<>();
		
		@Override
		@RosettaAttribute("utid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("utid")
		public String getUtid() {
			return utid;
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
		
		@Override
		@RosettaAttribute("venues")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("venues")
		public List<? extends FieldWithMetaString.FieldWithMetaStringBuilder> getVenues() {
			return venues;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateVenues(int index) {
			if (venues==null) {
				this.venues = new ArrayList<>();
			}
			return getIndex(venues, index, () -> {
						FieldWithMetaString.FieldWithMetaStringBuilder newVenues = FieldWithMetaString.builder();
						return newVenues;
					});
		}
		
		@RosettaAttribute("utid")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("utid")
		@Override
		public Trade.TradeBuilder setUtid(String _utid) {
			this.utid = _utid == null ? null : _utid;
			return this;
		}
		
		@RosettaAttribute("venue")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("venue")
		@Override
		public Trade.TradeBuilder setVenue(FieldWithMetaString _venue) {
			this.venue = _venue == null ? null : _venue.toBuilder();
			return this;
		}
		
		@Override
		public Trade.TradeBuilder setVenueValue(String _venue) {
			this.getOrCreateVenue().setValue(_venue);
			return this;
		}
		
		@RosettaAttribute("venues")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("venues")
		@Override
		public Trade.TradeBuilder addVenues(FieldWithMetaString _venues) {
			if (_venues != null) {
				this.venues.add(_venues.toBuilder());
			}
			return this;
		}
		
		@Override
		public Trade.TradeBuilder addVenues(FieldWithMetaString _venues, int idx) {
			getIndex(this.venues, idx, () -> _venues.toBuilder());
			return this;
		}
		
		@Override
		public Trade.TradeBuilder addVenuesValue(String _venues) {
			this.getOrCreateVenues(-1).setValue(_venues);
			return this;
		}
		
		@Override
		public Trade.TradeBuilder addVenuesValue(String _venues, int idx) {
			this.getOrCreateVenues(idx).setValue(_venues);
			return this;
		}
		
		@Override
		public Trade.TradeBuilder addVenues(List<? extends FieldWithMetaString> venuess) {
			if (venuess != null) {
				for (final FieldWithMetaString toAdd : venuess) {
					this.venues.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("venues")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("venues")
		@Override
		public Trade.TradeBuilder setVenues(List<? extends FieldWithMetaString> venuess) {
			if (venuess == null) {
				this.venues = new ArrayList<>();
			} else {
				this.venues = venuess.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Trade.TradeBuilder addVenuesValue(List<? extends String> venuess) {
			if (venuess != null) {
				for (final String toAdd : venuess) {
					this.addVenuesValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public Trade.TradeBuilder setVenuesValue(List<? extends String> venuess) {
			this.venues.clear();
			if (venuess != null) {
				venuess.forEach(this::addVenuesValue);
			}
			return this;
		}
		
		@Override
		public Trade build() {
			return new Trade.TradeImpl(this);
		}
		
		@Override
		public Trade.TradeBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Trade.TradeBuilder prune() {
			if (venue!=null && !venue.prune().hasData()) venue = null;
			venues = venues.stream().filter(b->b!=null).<FieldWithMetaString.FieldWithMetaStringBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getUtid()!=null) return true;
			if (getVenue()!=null) return true;
			if (getVenues()!=null && !getVenues().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Trade.TradeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Trade.TradeBuilder o = (Trade.TradeBuilder) other;
			
			merger.mergeRosetta(getVenue(), o.getVenue(), this::setVenue);
			merger.mergeRosetta(getVenues(), o.getVenues(), this::getOrCreateVenues);
			
			merger.mergeBasic(getUtid(), o.getUtid(), this::setUtid);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Trade _that = getType().cast(o);
		
			if (!Objects.equals(utid, _that.getUtid())) return false;
			if (!Objects.equals(venue, _that.getVenue())) return false;
			if (!ListEquals.listEquals(venues, _that.getVenues())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (utid != null ? utid.hashCode() : 0);
			_result = 31 * _result + (venue != null ? venue.hashCode() : 0);
			_result = 31 * _result + (venues != null ? venues.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "TradeBuilder {" +
				"utid=" + this.utid + ", " +
				"venue=" + this.venue + ", " +
				"venues=" + this.venues +
			'}';
		}
	}
}
