package chaos.s03.a4snap;

import chaos.s03.a4snap.meta.C3StockLegMeta;
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

import static java.util.Optional.ofNullable;

/**
 * Option 2.
 * @version 1.0.0-SNAPSHOT
 */
@RosettaDataType(value="C3StockLeg", builder=C3StockLeg.C3StockLegBuilderImpl.class, version="1.0.0-SNAPSHOT")
@RuneDataType(value="C3StockLeg", model="chaos", builder=C3StockLeg.C3StockLegBuilderImpl.class, version="1.0.0-SNAPSHOT")
public interface C3StockLeg extends RosettaModelObject {

	C3StockLegMeta metaData = new C3StockLegMeta();

	/*********************** Getter Methods  ***********************/
	String getTicker();
	String getCommon();

	/*********************** Build Methods  ***********************/
	C3StockLeg build();
	
	C3StockLeg.C3StockLegBuilder toBuilder();
	
	static C3StockLeg.C3StockLegBuilder builder() {
		return new C3StockLeg.C3StockLegBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C3StockLeg> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C3StockLeg> getType() {
		return C3StockLeg.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("ticker"), String.class, getTicker(), this);
		processor.processBasic(path.newSubPath("common"), String.class, getCommon(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C3StockLegBuilder extends C3StockLeg, RosettaModelObjectBuilder {
		C3StockLeg.C3StockLegBuilder setTicker(String ticker);
		C3StockLeg.C3StockLegBuilder setCommon(String common);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("ticker"), String.class, getTicker(), this);
			processor.processBasic(path.newSubPath("common"), String.class, getCommon(), this);
		}
		

		C3StockLeg.C3StockLegBuilder prune();
	}

	/*********************** Immutable Implementation of C3StockLeg  ***********************/
	class C3StockLegImpl implements C3StockLeg {
		private final String ticker;
		private final String common;
		
		protected C3StockLegImpl(C3StockLeg.C3StockLegBuilder builder) {
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
		public C3StockLeg build() {
			return this;
		}
		
		@Override
		public C3StockLeg.C3StockLegBuilder toBuilder() {
			C3StockLeg.C3StockLegBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C3StockLeg.C3StockLegBuilder builder) {
			ofNullable(getTicker()).ifPresent(builder::setTicker);
			ofNullable(getCommon()).ifPresent(builder::setCommon);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C3StockLeg _that = getType().cast(o);
		
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
			return "C3StockLeg {" +
				"ticker=" + this.ticker + ", " +
				"common=" + this.common +
			'}';
		}
	}

	/*********************** Builder Implementation of C3StockLeg  ***********************/
	class C3StockLegBuilderImpl implements C3StockLeg.C3StockLegBuilder {
	
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
		public C3StockLeg.C3StockLegBuilder setTicker(String _ticker) {
			this.ticker = _ticker == null ? null : _ticker;
			return this;
		}
		
		@RosettaAttribute("common")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("common")
		@Override
		public C3StockLeg.C3StockLegBuilder setCommon(String _common) {
			this.common = _common == null ? null : _common;
			return this;
		}
		
		@Override
		public C3StockLeg build() {
			return new C3StockLeg.C3StockLegImpl(this);
		}
		
		@Override
		public C3StockLeg.C3StockLegBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C3StockLeg.C3StockLegBuilder prune() {
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
		public C3StockLeg.C3StockLegBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C3StockLeg.C3StockLegBuilder o = (C3StockLeg.C3StockLegBuilder) other;
			
			
			merger.mergeBasic(getTicker(), o.getTicker(), this::setTicker);
			merger.mergeBasic(getCommon(), o.getCommon(), this::setCommon);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C3StockLeg _that = getType().cast(o);
		
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
			return "C3StockLegBuilder {" +
				"ticker=" + this.ticker + ", " +
				"common=" + this.common +
			'}';
		}
	}
}
