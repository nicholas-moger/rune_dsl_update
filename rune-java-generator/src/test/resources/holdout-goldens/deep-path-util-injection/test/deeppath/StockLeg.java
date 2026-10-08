package test.deeppath;

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
import test.deeppath.meta.StockLegMeta;

import static java.util.Optional.ofNullable;

/**
 * Inner option 2 (the chaos C3StockLeg).
 * @version 0.0.0
 */
@RosettaDataType(value="StockLeg", builder=StockLeg.StockLegBuilderImpl.class, version="0.0.0")
@RuneDataType(value="StockLeg", model="test", builder=StockLeg.StockLegBuilderImpl.class, version="0.0.0")
public interface StockLeg extends RosettaModelObject {

	StockLegMeta metaData = new StockLegMeta();

	/*********************** Getter Methods  ***********************/
	String getTicker();
	String getCommon();

	/*********************** Build Methods  ***********************/
	StockLeg build();
	
	StockLeg.StockLegBuilder toBuilder();
	
	static StockLeg.StockLegBuilder builder() {
		return new StockLeg.StockLegBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends StockLeg> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends StockLeg> getType() {
		return StockLeg.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("ticker"), String.class, getTicker(), this);
		processor.processBasic(path.newSubPath("common"), String.class, getCommon(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface StockLegBuilder extends StockLeg, RosettaModelObjectBuilder {
		StockLeg.StockLegBuilder setTicker(String ticker);
		StockLeg.StockLegBuilder setCommon(String common);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("ticker"), String.class, getTicker(), this);
			processor.processBasic(path.newSubPath("common"), String.class, getCommon(), this);
		}
		

		StockLeg.StockLegBuilder prune();
	}

	/*********************** Immutable Implementation of StockLeg  ***********************/
	class StockLegImpl implements StockLeg {
		private final String ticker;
		private final String common;
		
		protected StockLegImpl(StockLeg.StockLegBuilder builder) {
			this.ticker = builder.getTicker();
			this.common = builder.getCommon();
		}
		
		@Override
		@RosettaAttribute("ticker")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("ticker")
		public String getTicker() {
			return ticker;
		}
		
		@Override
		@RosettaAttribute("common")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("common")
		public String getCommon() {
			return common;
		}
		
		@Override
		public StockLeg build() {
			return this;
		}
		
		@Override
		public StockLeg.StockLegBuilder toBuilder() {
			StockLeg.StockLegBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(StockLeg.StockLegBuilder builder) {
			ofNullable(getTicker()).ifPresent(builder::setTicker);
			ofNullable(getCommon()).ifPresent(builder::setCommon);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			StockLeg _that = getType().cast(o);
		
			if (!Objects.equals(ticker, _that.getTicker())) return false;
			if (!Objects.equals(common, _that.getCommon())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (ticker != null ? ticker.hashCode() : 0);
			_result = 31 * _result + (common != null ? common.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "StockLeg {" +
				"ticker=" + this.ticker + ", " +
				"common=" + this.common +
			'}';
		}
	}

	/*********************** Builder Implementation of StockLeg  ***********************/
	class StockLegBuilderImpl implements StockLeg.StockLegBuilder {
	
		protected String ticker;
		protected String common;
		
		@Override
		@RosettaAttribute("ticker")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("ticker")
		public String getTicker() {
			return ticker;
		}
		
		@Override
		@RosettaAttribute("common")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("common")
		public String getCommon() {
			return common;
		}
		
		@RosettaAttribute("ticker")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("ticker")
		@Override
		public StockLeg.StockLegBuilder setTicker(String _ticker) {
			this.ticker = _ticker == null ? null : _ticker;
			return this;
		}
		
		@RosettaAttribute("common")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("common")
		@Override
		public StockLeg.StockLegBuilder setCommon(String _common) {
			this.common = _common == null ? null : _common;
			return this;
		}
		
		@Override
		public StockLeg build() {
			return new StockLeg.StockLegImpl(this);
		}
		
		@Override
		public StockLeg.StockLegBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public StockLeg.StockLegBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getTicker()!=null) return true;
			if (getCommon()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public StockLeg.StockLegBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			StockLeg.StockLegBuilder o = (StockLeg.StockLegBuilder) other;
			
			
			merger.mergeBasic(getTicker(), o.getTicker(), this::setTicker);
			merger.mergeBasic(getCommon(), o.getCommon(), this::setCommon);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			StockLeg _that = getType().cast(o);
		
			if (!Objects.equals(ticker, _that.getTicker())) return false;
			if (!Objects.equals(common, _that.getCommon())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (ticker != null ? ticker.hashCode() : 0);
			_result = 31 * _result + (common != null ? common.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "StockLegBuilder {" +
				"ticker=" + this.ticker + ", " +
				"common=" + this.common +
			'}';
		}
	}
}
