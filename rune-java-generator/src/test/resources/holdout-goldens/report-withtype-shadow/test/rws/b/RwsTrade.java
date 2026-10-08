package test.rws.b;

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
import test.rws.b.meta.RwsTradeMeta;

import static java.util.Optional.ofNullable;

/**
 * The report-from type.
 * @version 0.0.0
 */
@RosettaDataType(value="RwsTrade", builder=RwsTrade.RwsTradeBuilderImpl.class, version="0.0.0")
@RuneDataType(value="RwsTrade", model="test", builder=RwsTrade.RwsTradeBuilderImpl.class, version="0.0.0")
public interface RwsTrade extends RosettaModelObject {

	RwsTradeMeta metaData = new RwsTradeMeta();

	/*********************** Getter Methods  ***********************/
	String getUtid();
	BigDecimal getNotional();

	/*********************** Build Methods  ***********************/
	RwsTrade build();
	
	RwsTrade.RwsTradeBuilder toBuilder();
	
	static RwsTrade.RwsTradeBuilder builder() {
		return new RwsTrade.RwsTradeBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RwsTrade> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RwsTrade> getType() {
		return RwsTrade.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("utid"), String.class, getUtid(), this);
		processor.processBasic(path.newSubPath("notional"), BigDecimal.class, getNotional(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RwsTradeBuilder extends RwsTrade, RosettaModelObjectBuilder {
		RwsTrade.RwsTradeBuilder setUtid(String utid);
		RwsTrade.RwsTradeBuilder setNotional(BigDecimal notional);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("utid"), String.class, getUtid(), this);
			processor.processBasic(path.newSubPath("notional"), BigDecimal.class, getNotional(), this);
		}
		

		RwsTrade.RwsTradeBuilder prune();
	}

	/*********************** Immutable Implementation of RwsTrade  ***********************/
	class RwsTradeImpl implements RwsTrade {
		private final String utid;
		private final BigDecimal notional;
		
		protected RwsTradeImpl(RwsTrade.RwsTradeBuilder builder) {
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
		public RwsTrade build() {
			return this;
		}
		
		@Override
		public RwsTrade.RwsTradeBuilder toBuilder() {
			RwsTrade.RwsTradeBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RwsTrade.RwsTradeBuilder builder) {
			ofNullable(getUtid()).ifPresent(builder::setUtid);
			ofNullable(getNotional()).ifPresent(builder::setNotional);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RwsTrade _that = getType().cast(o);
		
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
			return "RwsTrade {" +
				"utid=" + this.utid + ", " +
				"notional=" + this.notional +
			'}';
		}
	}

	/*********************** Builder Implementation of RwsTrade  ***********************/
	class RwsTradeBuilderImpl implements RwsTrade.RwsTradeBuilder {
	
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
		public RwsTrade.RwsTradeBuilder setUtid(String _utid) {
			this.utid = _utid == null ? null : _utid;
			return this;
		}
		
		@RosettaAttribute("notional")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("notional")
		@Override
		public RwsTrade.RwsTradeBuilder setNotional(BigDecimal _notional) {
			this.notional = _notional == null ? null : _notional;
			return this;
		}
		
		@Override
		public RwsTrade build() {
			return new RwsTrade.RwsTradeImpl(this);
		}
		
		@Override
		public RwsTrade.RwsTradeBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RwsTrade.RwsTradeBuilder prune() {
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
		public RwsTrade.RwsTradeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RwsTrade.RwsTradeBuilder o = (RwsTrade.RwsTradeBuilder) other;
			
			
			merger.mergeBasic(getUtid(), o.getUtid(), this::setUtid);
			merger.mergeBasic(getNotional(), o.getNotional(), this::setNotional);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RwsTrade _that = getType().cast(o);
		
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
			return "RwsTradeBuilder {" +
				"utid=" + this.utid + ", " +
				"notional=" + this.notional +
			'}';
		}
	}
}
