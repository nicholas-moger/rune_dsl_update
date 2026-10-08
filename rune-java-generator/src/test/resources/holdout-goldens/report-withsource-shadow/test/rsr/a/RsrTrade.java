package test.rsr.a;

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
import test.rsr.a.meta.RsrTradeMeta;

import static java.util.Optional.ofNullable;

/**
 * The report-from type.
 * @version 0.0.0
 */
@RosettaDataType(value="RsrTrade", builder=RsrTrade.RsrTradeBuilderImpl.class, version="0.0.0")
@RuneDataType(value="RsrTrade", model="test", builder=RsrTrade.RsrTradeBuilderImpl.class, version="0.0.0")
public interface RsrTrade extends RosettaModelObject {

	RsrTradeMeta metaData = new RsrTradeMeta();

	/*********************** Getter Methods  ***********************/
	String getUtid();
	BigDecimal getNotional();

	/*********************** Build Methods  ***********************/
	RsrTrade build();
	
	RsrTrade.RsrTradeBuilder toBuilder();
	
	static RsrTrade.RsrTradeBuilder builder() {
		return new RsrTrade.RsrTradeBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RsrTrade> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RsrTrade> getType() {
		return RsrTrade.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("utid"), String.class, getUtid(), this);
		processor.processBasic(path.newSubPath("notional"), BigDecimal.class, getNotional(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RsrTradeBuilder extends RsrTrade, RosettaModelObjectBuilder {
		RsrTrade.RsrTradeBuilder setUtid(String utid);
		RsrTrade.RsrTradeBuilder setNotional(BigDecimal notional);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("utid"), String.class, getUtid(), this);
			processor.processBasic(path.newSubPath("notional"), BigDecimal.class, getNotional(), this);
		}
		

		RsrTrade.RsrTradeBuilder prune();
	}

	/*********************** Immutable Implementation of RsrTrade  ***********************/
	class RsrTradeImpl implements RsrTrade {
		private final String utid;
		private final BigDecimal notional;
		
		protected RsrTradeImpl(RsrTrade.RsrTradeBuilder builder) {
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
		public RsrTrade build() {
			return this;
		}
		
		@Override
		public RsrTrade.RsrTradeBuilder toBuilder() {
			RsrTrade.RsrTradeBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RsrTrade.RsrTradeBuilder builder) {
			ofNullable(getUtid()).ifPresent(builder::setUtid);
			ofNullable(getNotional()).ifPresent(builder::setNotional);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RsrTrade _that = getType().cast(o);
		
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
			return "RsrTrade {" +
				"utid=" + this.utid + ", " +
				"notional=" + this.notional +
			'}';
		}
	}

	/*********************** Builder Implementation of RsrTrade  ***********************/
	class RsrTradeBuilderImpl implements RsrTrade.RsrTradeBuilder {
	
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
		public RsrTrade.RsrTradeBuilder setUtid(String _utid) {
			this.utid = _utid == null ? null : _utid;
			return this;
		}
		
		@RosettaAttribute("notional")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("notional")
		@Override
		public RsrTrade.RsrTradeBuilder setNotional(BigDecimal _notional) {
			this.notional = _notional == null ? null : _notional;
			return this;
		}
		
		@Override
		public RsrTrade build() {
			return new RsrTrade.RsrTradeImpl(this);
		}
		
		@Override
		public RsrTrade.RsrTradeBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RsrTrade.RsrTradeBuilder prune() {
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
		public RsrTrade.RsrTradeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RsrTrade.RsrTradeBuilder o = (RsrTrade.RsrTradeBuilder) other;
			
			
			merger.mergeBasic(getUtid(), o.getUtid(), this::setUtid);
			merger.mergeBasic(getNotional(), o.getNotional(), this::setNotional);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RsrTrade _that = getType().cast(o);
		
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
			return "RsrTradeBuilder {" +
				"utid=" + this.utid + ", " +
				"notional=" + this.notional +
			'}';
		}
	}
}
