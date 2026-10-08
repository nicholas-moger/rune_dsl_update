package chaos.s28.a1o4;

import chaos.s28.a1o4.meta.C28TradeMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
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
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * The report-from type: a choice-typed attribute navigated by bare option at a rule seat (F15&#39; leg d) and a scheme-carrying scalar.
 * @version 1.0.0
 */
@RosettaDataType(value="C28Trade", builder=C28Trade.C28TradeBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C28Trade", model="chaos", builder=C28Trade.C28TradeBuilderImpl.class, version="1.0.0")
public interface C28Trade extends RosettaModelObject {

	C28TradeMeta metaData = new C28TradeMeta();

	/*********************** Getter Methods  ***********************/
	String getUtid();
	C28Which getWhich();
	FieldWithMetaString getVenue();
	C28Extra getExtra();

	/*********************** Build Methods  ***********************/
	C28Trade build();
	
	C28Trade.C28TradeBuilder toBuilder();
	
	static C28Trade.C28TradeBuilder builder() {
		return new C28Trade.C28TradeBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C28Trade> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C28Trade> getType() {
		return C28Trade.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("utid"), String.class, getUtid(), this);
		processRosetta(path.newSubPath("which"), processor, C28Which.class, getWhich());
		processRosetta(path.newSubPath("venue"), processor, FieldWithMetaString.class, getVenue());
		processRosetta(path.newSubPath("extra"), processor, C28Extra.class, getExtra());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C28TradeBuilder extends C28Trade, RosettaModelObjectBuilder {
		C28Which.C28WhichBuilder getOrCreateWhich();
		@Override
		C28Which.C28WhichBuilder getWhich();
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateVenue();
		@Override
		FieldWithMetaString.FieldWithMetaStringBuilder getVenue();
		C28Extra.C28ExtraBuilder getOrCreateExtra();
		@Override
		C28Extra.C28ExtraBuilder getExtra();
		C28Trade.C28TradeBuilder setUtid(String utid);
		C28Trade.C28TradeBuilder setWhich(C28Which which);
		C28Trade.C28TradeBuilder setVenue(FieldWithMetaString venue);
		C28Trade.C28TradeBuilder setVenueValue(String venue);
		C28Trade.C28TradeBuilder setExtra(C28Extra extra);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("utid"), String.class, getUtid(), this);
			processRosetta(path.newSubPath("which"), processor, C28Which.C28WhichBuilder.class, getWhich());
			processRosetta(path.newSubPath("venue"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getVenue());
			processRosetta(path.newSubPath("extra"), processor, C28Extra.C28ExtraBuilder.class, getExtra());
		}
		

		C28Trade.C28TradeBuilder prune();
	}

	/*********************** Immutable Implementation of C28Trade  ***********************/
	class C28TradeImpl implements C28Trade {
		private final String utid;
		private final C28Which which;
		private final FieldWithMetaString venue;
		private final C28Extra extra;
		
		protected C28TradeImpl(C28Trade.C28TradeBuilder builder) {
			this.utid = builder.getUtid();
			this.which = ofNullable(builder.getWhich()).map(f->f.build()).orElse(null);
			this.venue = ofNullable(builder.getVenue()).map(f->f.build()).orElse(null);
			this.extra = ofNullable(builder.getExtra()).map(f->f.build()).orElse(null);
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
		@RosettaAttribute("which")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("which")
		public C28Which getWhich() {
			return which;
		}
		
		@Override
		@RosettaAttribute("venue")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("venue")
		public FieldWithMetaString getVenue() {
			return venue;
		}
		
		@Override
		@RosettaAttribute("extra")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("extra")
		public C28Extra getExtra() {
			return extra;
		}
		
		@Override
		public C28Trade build() {
			return this;
		}
		
		@Override
		public C28Trade.C28TradeBuilder toBuilder() {
			C28Trade.C28TradeBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C28Trade.C28TradeBuilder builder) {
			ofNullable(getUtid()).ifPresent(builder::setUtid);
			ofNullable(getWhich()).ifPresent(builder::setWhich);
			ofNullable(getVenue()).ifPresent(builder::setVenue);
			ofNullable(getExtra()).ifPresent(builder::setExtra);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28Trade _that = getType().cast(o);
		
			if (!Objects.equals(utid, _that.getUtid())) return false;
			if (!Objects.equals(which, _that.getWhich())) return false;
			if (!Objects.equals(venue, _that.getVenue())) return false;
			if (!Objects.equals(extra, _that.getExtra())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (utid != null ? utid.hashCode() : 0);
			_result = 31 * _result + (which != null ? which.hashCode() : 0);
			_result = 31 * _result + (venue != null ? venue.hashCode() : 0);
			_result = 31 * _result + (extra != null ? extra.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C28Trade {" +
				"utid=" + this.utid + ", " +
				"which=" + this.which + ", " +
				"venue=" + this.venue + ", " +
				"extra=" + this.extra +
			'}';
		}
	}

	/*********************** Builder Implementation of C28Trade  ***********************/
	class C28TradeBuilderImpl implements C28Trade.C28TradeBuilder {
	
		protected String utid;
		protected C28Which.C28WhichBuilder which;
		protected FieldWithMetaString.FieldWithMetaStringBuilder venue;
		protected C28Extra.C28ExtraBuilder extra;
		
		@Override
		@RosettaAttribute("utid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("utid")
		public String getUtid() {
			return utid;
		}
		
		@Override
		@RosettaAttribute("which")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("which")
		public C28Which.C28WhichBuilder getWhich() {
			return which;
		}
		
		@Override
		public C28Which.C28WhichBuilder getOrCreateWhich() {
			C28Which.C28WhichBuilder result;
			if (which!=null) {
				result = which;
			}
			else {
				result = which = C28Which.builder();
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
		
		@Override
		@RosettaAttribute("extra")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("extra")
		public C28Extra.C28ExtraBuilder getExtra() {
			return extra;
		}
		
		@Override
		public C28Extra.C28ExtraBuilder getOrCreateExtra() {
			C28Extra.C28ExtraBuilder result;
			if (extra!=null) {
				result = extra;
			}
			else {
				result = extra = C28Extra.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("utid")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("utid")
		@Override
		public C28Trade.C28TradeBuilder setUtid(String _utid) {
			this.utid = _utid == null ? null : _utid;
			return this;
		}
		
		@RosettaAttribute("which")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("which")
		@Override
		public C28Trade.C28TradeBuilder setWhich(C28Which _which) {
			this.which = _which == null ? null : _which.toBuilder();
			return this;
		}
		
		@RosettaAttribute("venue")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("venue")
		@Override
		public C28Trade.C28TradeBuilder setVenue(FieldWithMetaString _venue) {
			this.venue = _venue == null ? null : _venue.toBuilder();
			return this;
		}
		
		@Override
		public C28Trade.C28TradeBuilder setVenueValue(String _venue) {
			this.getOrCreateVenue().setValue(_venue);
			return this;
		}
		
		@RosettaAttribute("extra")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("extra")
		@Override
		public C28Trade.C28TradeBuilder setExtra(C28Extra _extra) {
			this.extra = _extra == null ? null : _extra.toBuilder();
			return this;
		}
		
		@Override
		public C28Trade build() {
			return new C28Trade.C28TradeImpl(this);
		}
		
		@Override
		public C28Trade.C28TradeBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28Trade.C28TradeBuilder prune() {
			if (which!=null && !which.prune().hasData()) which = null;
			if (venue!=null && !venue.prune().hasData()) venue = null;
			if (extra!=null && !extra.prune().hasData()) extra = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getUtid()!=null) return true;
			if (getWhich()!=null && getWhich().hasData()) return true;
			if (getVenue()!=null) return true;
			if (getExtra()!=null && getExtra().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28Trade.C28TradeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C28Trade.C28TradeBuilder o = (C28Trade.C28TradeBuilder) other;
			
			merger.mergeRosetta(getWhich(), o.getWhich(), this::setWhich);
			merger.mergeRosetta(getVenue(), o.getVenue(), this::setVenue);
			merger.mergeRosetta(getExtra(), o.getExtra(), this::setExtra);
			
			merger.mergeBasic(getUtid(), o.getUtid(), this::setUtid);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28Trade _that = getType().cast(o);
		
			if (!Objects.equals(utid, _that.getUtid())) return false;
			if (!Objects.equals(which, _that.getWhich())) return false;
			if (!Objects.equals(venue, _that.getVenue())) return false;
			if (!Objects.equals(extra, _that.getExtra())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (utid != null ? utid.hashCode() : 0);
			_result = 31 * _result + (which != null ? which.hashCode() : 0);
			_result = 31 * _result + (venue != null ? venue.hashCode() : 0);
			_result = 31 * _result + (extra != null ? extra.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C28TradeBuilder {" +
				"utid=" + this.utid + ", " +
				"which=" + this.which + ", " +
				"venue=" + this.venue + ", " +
				"extra=" + this.extra +
			'}';
		}
	}
}
