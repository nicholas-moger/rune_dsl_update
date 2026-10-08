package test.reports;

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
import java.util.Objects;
import test.reports.meta.TradeMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Trade", builder=Trade.TradeBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Trade", model="test", builder=Trade.TradeBuilderImpl.class, version="0.0.0")
public interface Trade extends RosettaModelObject {

	TradeMeta metaData = new TradeMeta();

	/*********************** Getter Methods  ***********************/
	String getTradeId();
	Boolean getIsActive();

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
		processor.processBasic(path.newSubPath("tradeId"), String.class, getTradeId(), this);
		processor.processBasic(path.newSubPath("isActive"), Boolean.class, getIsActive(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface TradeBuilder extends Trade, RosettaModelObjectBuilder {
		Trade.TradeBuilder setTradeId(String tradeId);
		Trade.TradeBuilder setIsActive(Boolean isActive);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("tradeId"), String.class, getTradeId(), this);
			processor.processBasic(path.newSubPath("isActive"), Boolean.class, getIsActive(), this);
		}
		

		Trade.TradeBuilder prune();
	}

	/*********************** Immutable Implementation of Trade  ***********************/
	class TradeImpl implements Trade {
		private final String tradeId;
		private final Boolean isActive;
		
		protected TradeImpl(Trade.TradeBuilder builder) {
			this.tradeId = builder.getTradeId();
			this.isActive = builder.getIsActive();
		}
		
		@Override
		@RosettaAttribute("tradeId")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("tradeId")
		public String getTradeId() {
			return tradeId;
		}
		
		@Override
		@RosettaAttribute("isActive")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("isActive")
		public Boolean getIsActive() {
			return isActive;
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
			ofNullable(getTradeId()).ifPresent(builder::setTradeId);
			ofNullable(getIsActive()).ifPresent(builder::setIsActive);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Trade _that = getType().cast(o);
		
			if (!Objects.equals(tradeId, _that.getTradeId())) return false;
			if (!Objects.equals(isActive, _that.getIsActive())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tradeId != null ? tradeId.hashCode() : 0);
			_result = 31 * _result + (isActive != null ? isActive.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Trade {" +
				"tradeId=" + this.tradeId + ", " +
				"isActive=" + this.isActive +
			'}';
		}
	}

	/*********************** Builder Implementation of Trade  ***********************/
	class TradeBuilderImpl implements Trade.TradeBuilder {
	
		protected String tradeId;
		protected Boolean isActive;
		
		@Override
		@RosettaAttribute("tradeId")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("tradeId")
		public String getTradeId() {
			return tradeId;
		}
		
		@Override
		@RosettaAttribute("isActive")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("isActive")
		public Boolean getIsActive() {
			return isActive;
		}
		
		@RosettaAttribute("tradeId")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("tradeId")
		@Override
		public Trade.TradeBuilder setTradeId(String _tradeId) {
			this.tradeId = _tradeId == null ? null : _tradeId;
			return this;
		}
		
		@RosettaAttribute("isActive")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("isActive")
		@Override
		public Trade.TradeBuilder setIsActive(Boolean _isActive) {
			this.isActive = _isActive == null ? null : _isActive;
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
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getTradeId()!=null) return true;
			if (getIsActive()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Trade.TradeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Trade.TradeBuilder o = (Trade.TradeBuilder) other;
			
			
			merger.mergeBasic(getTradeId(), o.getTradeId(), this::setTradeId);
			merger.mergeBasic(getIsActive(), o.getIsActive(), this::setIsActive);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Trade _that = getType().cast(o);
		
			if (!Objects.equals(tradeId, _that.getTradeId())) return false;
			if (!Objects.equals(isActive, _that.getIsActive())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tradeId != null ? tradeId.hashCode() : 0);
			_result = 31 * _result + (isActive != null ? isActive.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "TradeBuilder {" +
				"tradeId=" + this.tradeId + ", " +
				"isActive=" + this.isActive +
			'}';
		}
	}
}
