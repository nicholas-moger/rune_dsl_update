package test.rwq.a;

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
import test.rwq.a.meta.RwqTradeMeta;

import static java.util.Optional.ofNullable;

/**
 * The report-from type.
 * @version 0.0.0
 */
@RosettaDataType(value="RwqTrade", builder=RwqTrade.RwqTradeBuilderImpl.class, version="0.0.0")
@RuneDataType(value="RwqTrade", model="test", builder=RwqTrade.RwqTradeBuilderImpl.class, version="0.0.0")
public interface RwqTrade extends RosettaModelObject {

	RwqTradeMeta metaData = new RwqTradeMeta();

	/*********************** Getter Methods  ***********************/
	String getUtid();
	BigDecimal getNotional();

	/*********************** Build Methods  ***********************/
	RwqTrade build();
	
	RwqTrade.RwqTradeBuilder toBuilder();
	
	static RwqTrade.RwqTradeBuilder builder() {
		return new RwqTrade.RwqTradeBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RwqTrade> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RwqTrade> getType() {
		return RwqTrade.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("utid"), String.class, getUtid(), this);
		processor.processBasic(path.newSubPath("notional"), BigDecimal.class, getNotional(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RwqTradeBuilder extends RwqTrade, RosettaModelObjectBuilder {
		RwqTrade.RwqTradeBuilder setUtid(String utid);
		RwqTrade.RwqTradeBuilder setNotional(BigDecimal notional);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("utid"), String.class, getUtid(), this);
			processor.processBasic(path.newSubPath("notional"), BigDecimal.class, getNotional(), this);
		}
		

		RwqTrade.RwqTradeBuilder prune();
	}

	/*********************** Immutable Implementation of RwqTrade  ***********************/
	class RwqTradeImpl implements RwqTrade {
		private final String utid;
		private final BigDecimal notional;
		
		protected RwqTradeImpl(RwqTrade.RwqTradeBuilder builder) {
			this.utid = builder.getUtid();
			this.notional = builder.getNotional();
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
		public RwqTrade build() {
			return this;
		}
		
		@Override
		public RwqTrade.RwqTradeBuilder toBuilder() {
			RwqTrade.RwqTradeBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RwqTrade.RwqTradeBuilder builder) {
			ofNullable(getUtid()).ifPresent(builder::setUtid);
			ofNullable(getNotional()).ifPresent(builder::setNotional);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RwqTrade _that = getType().cast(o);
		
			if (!Objects.equals(utid, _that.getUtid())) return false;
			if (!Objects.equals(notional, _that.getNotional())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (utid != null ? utid.hashCode() : 0);
			_result = 31 * _result + (notional != null ? notional.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RwqTrade {" +
				"utid=" + this.utid + ", " +
				"notional=" + this.notional +
			'}';
		}
	}

	/*********************** Builder Implementation of RwqTrade  ***********************/
	class RwqTradeBuilderImpl implements RwqTrade.RwqTradeBuilder {
	
		protected String utid;
		protected BigDecimal notional;
		
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
		
		@RosettaAttribute("utid")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("utid")
		@Override
		public RwqTrade.RwqTradeBuilder setUtid(String _utid) {
			this.utid = _utid == null ? null : _utid;
			return this;
		}
		
		@RosettaAttribute("notional")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("notional")
		@Override
		public RwqTrade.RwqTradeBuilder setNotional(BigDecimal _notional) {
			this.notional = _notional == null ? null : _notional;
			return this;
		}
		
		@Override
		public RwqTrade build() {
			return new RwqTrade.RwqTradeImpl(this);
		}
		
		@Override
		public RwqTrade.RwqTradeBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RwqTrade.RwqTradeBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getUtid()!=null) return true;
			if (getNotional()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RwqTrade.RwqTradeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RwqTrade.RwqTradeBuilder o = (RwqTrade.RwqTradeBuilder) other;
			
			
			merger.mergeBasic(getUtid(), o.getUtid(), this::setUtid);
			merger.mergeBasic(getNotional(), o.getNotional(), this::setNotional);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RwqTrade _that = getType().cast(o);
		
			if (!Objects.equals(utid, _that.getUtid())) return false;
			if (!Objects.equals(notional, _that.getNotional())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (utid != null ? utid.hashCode() : 0);
			_result = 31 * _result + (notional != null ? notional.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RwqTradeBuilder {" +
				"utid=" + this.utid + ", " +
				"notional=" + this.notional +
			'}';
		}
	}
}
