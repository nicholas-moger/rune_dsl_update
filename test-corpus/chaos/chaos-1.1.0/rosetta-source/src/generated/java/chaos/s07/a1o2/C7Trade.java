package chaos.s07.a1o2;

import chaos.s07.a1o2.meta.C7TradeMeta;
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
 * The report-from type - label annotations in both grammar forms.
 * @version 1.0.0
 */
@RosettaDataType(value="C7Trade", builder=C7Trade.C7TradeBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C7Trade", model="chaos", builder=C7Trade.C7TradeBuilderImpl.class, version="1.0.0")
public interface C7Trade extends RosettaModelObject {

	C7TradeMeta metaData = new C7TradeMeta();

	/*********************** Getter Methods  ***********************/
	String getUtid();
	BigDecimal getNotional();
	C7Extra getExtra();

	/*********************** Build Methods  ***********************/
	C7Trade build();
	
	C7Trade.C7TradeBuilder toBuilder();
	
	static C7Trade.C7TradeBuilder builder() {
		return new C7Trade.C7TradeBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C7Trade> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C7Trade> getType() {
		return C7Trade.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("utid"), String.class, getUtid(), this);
		processor.processBasic(path.newSubPath("notional"), BigDecimal.class, getNotional(), this);
		processRosetta(path.newSubPath("extra"), processor, C7Extra.class, getExtra());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C7TradeBuilder extends C7Trade, RosettaModelObjectBuilder {
		C7Extra.C7ExtraBuilder getOrCreateExtra();
		@Override
		C7Extra.C7ExtraBuilder getExtra();
		C7Trade.C7TradeBuilder setUtid(String utid);
		C7Trade.C7TradeBuilder setNotional(BigDecimal notional);
		C7Trade.C7TradeBuilder setExtra(C7Extra extra);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("utid"), String.class, getUtid(), this);
			processor.processBasic(path.newSubPath("notional"), BigDecimal.class, getNotional(), this);
			processRosetta(path.newSubPath("extra"), processor, C7Extra.C7ExtraBuilder.class, getExtra());
		}
		

		C7Trade.C7TradeBuilder prune();
	}

	/*********************** Immutable Implementation of C7Trade  ***********************/
	class C7TradeImpl implements C7Trade {
		private final String utid;
		private final BigDecimal notional;
		private final C7Extra extra;
		
		protected C7TradeImpl(C7Trade.C7TradeBuilder builder) {
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
		public C7Extra getExtra() {
			return extra;
		}
		
		@Override
		public C7Trade build() {
			return this;
		}
		
		@Override
		public C7Trade.C7TradeBuilder toBuilder() {
			C7Trade.C7TradeBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C7Trade.C7TradeBuilder builder) {
			ofNullable(getUtid()).ifPresent(builder::setUtid);
			ofNullable(getNotional()).ifPresent(builder::setNotional);
			ofNullable(getExtra()).ifPresent(builder::setExtra);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C7Trade _that = getType().cast(o);
		
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
			return "C7Trade {" +
				"utid=" + this.utid + ", " +
				"notional=" + this.notional + ", " +
				"extra=" + this.extra +
			'}';
		}
	}

	/*********************** Builder Implementation of C7Trade  ***********************/
	class C7TradeBuilderImpl implements C7Trade.C7TradeBuilder {
	
		protected String utid;
		protected BigDecimal notional;
		protected C7Extra.C7ExtraBuilder extra;
		
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
		public C7Extra.C7ExtraBuilder getExtra() {
			return extra;
		}
		
		@Override
		public C7Extra.C7ExtraBuilder getOrCreateExtra() {
			C7Extra.C7ExtraBuilder result;
			if (extra!=null) {
				result = extra;
			}
			else {
				result = extra = C7Extra.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("utid")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("utid")
		@Override
		public C7Trade.C7TradeBuilder setUtid(String _utid) {
			this.utid = _utid == null ? null : _utid;
			return this;
		}
		
		@RosettaAttribute("notional")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("notional")
		@Override
		public C7Trade.C7TradeBuilder setNotional(BigDecimal _notional) {
			this.notional = _notional == null ? null : _notional;
			return this;
		}
		
		@RosettaAttribute("extra")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("extra")
		@Override
		public C7Trade.C7TradeBuilder setExtra(C7Extra _extra) {
			this.extra = _extra == null ? null : _extra.toBuilder();
			return this;
		}
		
		@Override
		public C7Trade build() {
			return new C7Trade.C7TradeImpl(this);
		}
		
		@Override
		public C7Trade.C7TradeBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C7Trade.C7TradeBuilder prune() {
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
		public C7Trade.C7TradeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C7Trade.C7TradeBuilder o = (C7Trade.C7TradeBuilder) other;
			
			merger.mergeRosetta(getExtra(), o.getExtra(), this::setExtra);
			
			merger.mergeBasic(getUtid(), o.getUtid(), this::setUtid);
			merger.mergeBasic(getNotional(), o.getNotional(), this::setNotional);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C7Trade _that = getType().cast(o);
		
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
			return "C7TradeBuilder {" +
				"utid=" + this.utid + ", " +
				"notional=" + this.notional + ", " +
				"extra=" + this.extra +
			'}';
		}
	}
}
