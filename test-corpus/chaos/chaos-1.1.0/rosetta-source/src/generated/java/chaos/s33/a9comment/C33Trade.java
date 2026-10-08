package chaos.s33.a9comment;

import chaos.s33.a9comment.meta.C33TradeMeta;
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
import java.math.BigDecimal;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Labels, synonyms and docReferences carrying non-ASCII, a quote and a backslash each (D46&#39;s REACH beyond the enum seats).
 * @version 1.0.0
 *
 * Body C33AUTH
 * Corpus Regulation C33Reg C33 “Chaos” Règlement &quot;quoted&quot; \ back "Display string with non-ASCII, a quote and a backslash." 
 * c33article "3"
 *
 * Provision Provision with µ–ü, a "quote" and a back\slash.
 *
 */
@RosettaDataType(value="C33Trade", builder=C33Trade.C33TradeBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C33Trade", model="chaos", builder=C33Trade.C33TradeBuilderImpl.class, version="1.0.0")
public interface C33Trade extends RosettaModelObject {

	C33TradeMeta metaData = new C33TradeMeta();

	/*********************** Getter Methods  ***********************/
	String getUtid();
	BigDecimal getNotional();
	C33Extra getExtra();

	/*********************** Build Methods  ***********************/
	C33Trade build();
	
	C33Trade.C33TradeBuilder toBuilder();
	
	static C33Trade.C33TradeBuilder builder() {
		return new C33Trade.C33TradeBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C33Trade> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C33Trade> getType() {
		return C33Trade.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("utid"), String.class, getUtid(), this);
		processor.processBasic(path.newSubPath("notional"), BigDecimal.class, getNotional(), this);
		processRosetta(path.newSubPath("extra"), processor, C33Extra.class, getExtra());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C33TradeBuilder extends C33Trade, RosettaModelObjectBuilder {
		C33Extra.C33ExtraBuilder getOrCreateExtra();
		@Override
		C33Extra.C33ExtraBuilder getExtra();
		C33Trade.C33TradeBuilder setUtid(String utid);
		C33Trade.C33TradeBuilder setNotional(BigDecimal notional);
		C33Trade.C33TradeBuilder setExtra(C33Extra extra);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("utid"), String.class, getUtid(), this);
			processor.processBasic(path.newSubPath("notional"), BigDecimal.class, getNotional(), this);
			processRosetta(path.newSubPath("extra"), processor, C33Extra.C33ExtraBuilder.class, getExtra());
		}
		

		C33Trade.C33TradeBuilder prune();
	}

	/*********************** Immutable Implementation of C33Trade  ***********************/
	class C33TradeImpl implements C33Trade {
		private final String utid;
		private final BigDecimal notional;
		private final C33Extra extra;
		
		protected C33TradeImpl(C33Trade.C33TradeBuilder builder) {
			this.utid = builder.getUtid();
			this.notional = builder.getNotional();
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
		@RosettaAttribute("notional")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("notional")
		public BigDecimal getNotional() {
			return notional;
		}
		
		@Override
		@RosettaAttribute("extra")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("extra")
		public C33Extra getExtra() {
			return extra;
		}
		
		@Override
		public C33Trade build() {
			return this;
		}
		
		@Override
		public C33Trade.C33TradeBuilder toBuilder() {
			C33Trade.C33TradeBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C33Trade.C33TradeBuilder builder) {
			ofNullable(getUtid()).ifPresent(builder::setUtid);
			ofNullable(getNotional()).ifPresent(builder::setNotional);
			ofNullable(getExtra()).ifPresent(builder::setExtra);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C33Trade _that = getType().cast(o);
		
			if (!Objects.equals(utid, _that.getUtid())) return false;
			if (!Objects.equals(notional, _that.getNotional())) return false;
			if (!Objects.equals(extra, _that.getExtra())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (utid != null ? utid.hashCode() : 0);
			_result = 31 * _result + (notional != null ? notional.hashCode() : 0);
			_result = 31 * _result + (extra != null ? extra.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C33Trade {" +
				"utid=" + this.utid + ", " +
				"notional=" + this.notional + ", " +
				"extra=" + this.extra +
			'}';
		}
	}

	/*********************** Builder Implementation of C33Trade  ***********************/
	class C33TradeBuilderImpl implements C33Trade.C33TradeBuilder {
	
		protected String utid;
		protected BigDecimal notional;
		protected C33Extra.C33ExtraBuilder extra;
		
		@Override
		@RosettaAttribute("utid")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("utid")
		public String getUtid() {
			return utid;
		}
		
		@Override
		@RosettaAttribute("notional")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("notional")
		public BigDecimal getNotional() {
			return notional;
		}
		
		@Override
		@RosettaAttribute("extra")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("extra")
		public C33Extra.C33ExtraBuilder getExtra() {
			return extra;
		}
		
		@Override
		public C33Extra.C33ExtraBuilder getOrCreateExtra() {
			C33Extra.C33ExtraBuilder result;
			if (extra!=null) {
				result = extra;
			}
			else {
				result = extra = C33Extra.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("utid")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("utid")
		@Override
		public C33Trade.C33TradeBuilder setUtid(String _utid) {
			this.utid = _utid == null ? null : _utid;
			return this;
		}
		
		@RosettaAttribute("notional")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("notional")
		@Override
		public C33Trade.C33TradeBuilder setNotional(BigDecimal _notional) {
			this.notional = _notional == null ? null : _notional;
			return this;
		}
		
		@RosettaAttribute("extra")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("extra")
		@Override
		public C33Trade.C33TradeBuilder setExtra(C33Extra _extra) {
			this.extra = _extra == null ? null : _extra.toBuilder();
			return this;
		}
		
		@Override
		public C33Trade build() {
			return new C33Trade.C33TradeImpl(this);
		}
		
		@Override
		public C33Trade.C33TradeBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C33Trade.C33TradeBuilder prune() {
			if (extra!=null && !extra.prune().hasData()) extra = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getUtid()!=null) return true;
			if (getNotional()!=null) return true;
			if (getExtra()!=null && getExtra().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C33Trade.C33TradeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C33Trade.C33TradeBuilder o = (C33Trade.C33TradeBuilder) other;
			
			merger.mergeRosetta(getExtra(), o.getExtra(), this::setExtra);
			
			merger.mergeBasic(getUtid(), o.getUtid(), this::setUtid);
			merger.mergeBasic(getNotional(), o.getNotional(), this::setNotional);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C33Trade _that = getType().cast(o);
		
			if (!Objects.equals(utid, _that.getUtid())) return false;
			if (!Objects.equals(notional, _that.getNotional())) return false;
			if (!Objects.equals(extra, _that.getExtra())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (utid != null ? utid.hashCode() : 0);
			_result = 31 * _result + (notional != null ? notional.hashCode() : 0);
			_result = 31 * _result + (extra != null ? extra.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C33TradeBuilder {" +
				"utid=" + this.utid + ", " +
				"notional=" + this.notional + ", " +
				"extra=" + this.extra +
			'}';
		}
	}
}
