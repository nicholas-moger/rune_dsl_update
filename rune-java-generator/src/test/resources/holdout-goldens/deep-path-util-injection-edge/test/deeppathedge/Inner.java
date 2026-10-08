package test.deeppathedge;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneChoiceType;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;
import test.deeppathedge.meta.InnerMeta;

import static java.util.Optional.ofNullable;

/**
 * The inner choice (the chaos C3Inner).
 * @version 0.0.0
 */
@RosettaDataType(value="Inner", builder=Inner.InnerBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Inner", model="test", builder=Inner.InnerBuilderImpl.class, version="0.0.0")
@RuneChoiceType
public interface Inner extends RosettaModelObject {

	InnerMeta metaData = new InnerMeta();

	/*********************** Getter Methods  ***********************/
	CashLeg getCashLeg();
	StockLeg getStockLeg();

	/*********************** Build Methods  ***********************/
	Inner build();
	
	Inner.InnerBuilder toBuilder();
	
	static Inner.InnerBuilder builder() {
		return new Inner.InnerBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Inner> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Inner> getType() {
		return Inner.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("CashLeg"), processor, CashLeg.class, getCashLeg());
		processRosetta(path.newSubPath("StockLeg"), processor, StockLeg.class, getStockLeg());
	}
	

	/*********************** Builder Interface  ***********************/
	interface InnerBuilder extends Inner, RosettaModelObjectBuilder {
		CashLeg.CashLegBuilder getOrCreateCashLeg();
		@Override
		CashLeg.CashLegBuilder getCashLeg();
		StockLeg.StockLegBuilder getOrCreateStockLeg();
		@Override
		StockLeg.StockLegBuilder getStockLeg();
		Inner.InnerBuilder setCashLeg(CashLeg _CashLeg);
		Inner.InnerBuilder setStockLeg(StockLeg _StockLeg);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("CashLeg"), processor, CashLeg.CashLegBuilder.class, getCashLeg());
			processRosetta(path.newSubPath("StockLeg"), processor, StockLeg.StockLegBuilder.class, getStockLeg());
		}
		

		Inner.InnerBuilder prune();
	}

	/*********************** Immutable Implementation of Inner  ***********************/
	class InnerImpl implements Inner {
		private final CashLeg cashLeg;
		private final StockLeg stockLeg;
		
		protected InnerImpl(Inner.InnerBuilder builder) {
			this.cashLeg = ofNullable(builder.getCashLeg()).map(f->f.build()).orElse(null);
			this.stockLeg = ofNullable(builder.getStockLeg()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("CashLeg")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("CashLeg")
		public CashLeg getCashLeg() {
			return cashLeg;
		}
		
		@Override
		@RosettaAttribute("StockLeg")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("StockLeg")
		public StockLeg getStockLeg() {
			return stockLeg;
		}
		
		@Override
		public Inner build() {
			return this;
		}
		
		@Override
		public Inner.InnerBuilder toBuilder() {
			Inner.InnerBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Inner.InnerBuilder builder) {
			ofNullable(getCashLeg()).ifPresent(builder::setCashLeg);
			ofNullable(getStockLeg()).ifPresent(builder::setStockLeg);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Inner _that = getType().cast(o);
		
			if (!Objects.equals(cashLeg, _that.getCashLeg())) return false;
			if (!Objects.equals(stockLeg, _that.getStockLeg())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (cashLeg != null ? cashLeg.hashCode() : 0);
			_result = 31 * _result + (stockLeg != null ? stockLeg.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Inner {" +
				"CashLeg=" + this.cashLeg + ", " +
				"StockLeg=" + this.stockLeg +
			'}';
		}
	}

	/*********************** Builder Implementation of Inner  ***********************/
	class InnerBuilderImpl implements Inner.InnerBuilder {
	
		protected CashLeg.CashLegBuilder cashLeg;
		protected StockLeg.StockLegBuilder stockLeg;
		
		@Override
		@RosettaAttribute("CashLeg")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("CashLeg")
		public CashLeg.CashLegBuilder getCashLeg() {
			return cashLeg;
		}
		
		@Override
		public CashLeg.CashLegBuilder getOrCreateCashLeg() {
			CashLeg.CashLegBuilder result;
			if (cashLeg!=null) {
				result = cashLeg;
			}
			else {
				result = cashLeg = CashLeg.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("StockLeg")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("StockLeg")
		public StockLeg.StockLegBuilder getStockLeg() {
			return stockLeg;
		}
		
		@Override
		public StockLeg.StockLegBuilder getOrCreateStockLeg() {
			StockLeg.StockLegBuilder result;
			if (stockLeg!=null) {
				result = stockLeg;
			}
			else {
				result = stockLeg = StockLeg.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("CashLeg")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("CashLeg")
		@Override
		public Inner.InnerBuilder setCashLeg(CashLeg _cashLeg) {
			this.cashLeg = _cashLeg == null ? null : _cashLeg.toBuilder();
			return this;
		}
		
		@RosettaAttribute("StockLeg")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("StockLeg")
		@Override
		public Inner.InnerBuilder setStockLeg(StockLeg _stockLeg) {
			this.stockLeg = _stockLeg == null ? null : _stockLeg.toBuilder();
			return this;
		}
		
		@Override
		public Inner build() {
			return new Inner.InnerImpl(this);
		}
		
		@Override
		public Inner.InnerBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Inner.InnerBuilder prune() {
			if (cashLeg!=null && !cashLeg.prune().hasData()) cashLeg = null;
			if (stockLeg!=null && !stockLeg.prune().hasData()) stockLeg = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getCashLeg()!=null && getCashLeg().hasData()) return true;
			if (getStockLeg()!=null && getStockLeg().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Inner.InnerBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Inner.InnerBuilder o = (Inner.InnerBuilder) other;
			
			merger.mergeRosetta(getCashLeg(), o.getCashLeg(), this::setCashLeg);
			merger.mergeRosetta(getStockLeg(), o.getStockLeg(), this::setStockLeg);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Inner _that = getType().cast(o);
		
			if (!Objects.equals(cashLeg, _that.getCashLeg())) return false;
			if (!Objects.equals(stockLeg, _that.getStockLeg())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (cashLeg != null ? cashLeg.hashCode() : 0);
			_result = 31 * _result + (stockLeg != null ? stockLeg.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "InnerBuilder {" +
				"CashLeg=" + this.cashLeg + ", " +
				"StockLeg=" + this.stockLeg +
			'}';
		}
	}
}
