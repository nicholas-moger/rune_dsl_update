package test.rsp.a;

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
import test.rsp.a.meta.RspTradeMeta;

import static java.util.Optional.ofNullable;

/**
 * The report-from type.
 * @version 0.0.0
 */
@RosettaDataType(value="RspTrade", builder=RspTrade.RspTradeBuilderImpl.class, version="0.0.0")
@RuneDataType(value="RspTrade", model="test", builder=RspTrade.RspTradeBuilderImpl.class, version="0.0.0")
public interface RspTrade extends RosettaModelObject {

	RspTradeMeta metaData = new RspTradeMeta();

	/*********************** Getter Methods  ***********************/
	String getUtid();
	BigDecimal getNotional();

	/*********************** Build Methods  ***********************/
	RspTrade build();
	
	RspTrade.RspTradeBuilder toBuilder();
	
	static RspTrade.RspTradeBuilder builder() {
		return new RspTrade.RspTradeBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RspTrade> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RspTrade> getType() {
		return RspTrade.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("utid"), String.class, getUtid(), this);
		processor.processBasic(path.newSubPath("notional"), BigDecimal.class, getNotional(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RspTradeBuilder extends RspTrade, RosettaModelObjectBuilder {
		RspTrade.RspTradeBuilder setUtid(String utid);
		RspTrade.RspTradeBuilder setNotional(BigDecimal notional);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("utid"), String.class, getUtid(), this);
			processor.processBasic(path.newSubPath("notional"), BigDecimal.class, getNotional(), this);
		}
		

		RspTrade.RspTradeBuilder prune();
	}

	/*********************** Immutable Implementation of RspTrade  ***********************/
	class RspTradeImpl implements RspTrade {
		private final String utid;
		private final BigDecimal notional;
		
		protected RspTradeImpl(RspTrade.RspTradeBuilder builder) {
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
		public RspTrade build() {
			return this;
		}
		
		@Override
		public RspTrade.RspTradeBuilder toBuilder() {
			RspTrade.RspTradeBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RspTrade.RspTradeBuilder builder) {
			ofNullable(getUtid()).ifPresent(builder::setUtid);
			ofNullable(getNotional()).ifPresent(builder::setNotional);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RspTrade _that = getType().cast(o);
		
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
			return "RspTrade {" +
				"utid=" + this.utid + ", " +
				"notional=" + this.notional +
			'}';
		}
	}

	/*********************** Builder Implementation of RspTrade  ***********************/
	class RspTradeBuilderImpl implements RspTrade.RspTradeBuilder {
	
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
		public RspTrade.RspTradeBuilder setUtid(String _utid) {
			this.utid = _utid == null ? null : _utid;
			return this;
		}
		
		@RosettaAttribute("notional")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("notional")
		@Override
		public RspTrade.RspTradeBuilder setNotional(BigDecimal _notional) {
			this.notional = _notional == null ? null : _notional;
			return this;
		}
		
		@Override
		public RspTrade build() {
			return new RspTrade.RspTradeImpl(this);
		}
		
		@Override
		public RspTrade.RspTradeBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RspTrade.RspTradeBuilder prune() {
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
		public RspTrade.RspTradeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RspTrade.RspTradeBuilder o = (RspTrade.RspTradeBuilder) other;
			
			
			merger.mergeBasic(getUtid(), o.getUtid(), this::setUtid);
			merger.mergeBasic(getNotional(), o.getNotional(), this::setNotional);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RspTrade _that = getType().cast(o);
		
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
			return "RspTradeBuilder {" +
				"utid=" + this.utid + ", " +
				"notional=" + this.notional +
			'}';
		}
	}
}
