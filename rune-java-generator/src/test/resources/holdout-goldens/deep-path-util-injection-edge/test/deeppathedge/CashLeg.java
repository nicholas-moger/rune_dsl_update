package test.deeppathedge;

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
import test.deeppathedge.meta.CashLegMeta;

import static java.util.Optional.ofNullable;

/**
 * Inner option 1 - shares &#39;common&#39; with its sibling (the chaos C3CashLeg).
 * @version 0.0.0
 */
@RosettaDataType(value="CashLeg", builder=CashLeg.CashLegBuilderImpl.class, version="0.0.0")
@RuneDataType(value="CashLeg", model="test", builder=CashLeg.CashLegBuilderImpl.class, version="0.0.0")
public interface CashLeg extends RosettaModelObject {

	CashLegMeta metaData = new CashLegMeta();

	/*********************** Getter Methods  ***********************/
	String getCcy();
	String getCommon();

	/*********************** Build Methods  ***********************/
	CashLeg build();
	
	CashLeg.CashLegBuilder toBuilder();
	
	static CashLeg.CashLegBuilder builder() {
		return new CashLeg.CashLegBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends CashLeg> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends CashLeg> getType() {
		return CashLeg.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("ccy"), String.class, getCcy(), this);
		processor.processBasic(path.newSubPath("common"), String.class, getCommon(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface CashLegBuilder extends CashLeg, RosettaModelObjectBuilder {
		CashLeg.CashLegBuilder setCcy(String ccy);
		CashLeg.CashLegBuilder setCommon(String common);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("ccy"), String.class, getCcy(), this);
			processor.processBasic(path.newSubPath("common"), String.class, getCommon(), this);
		}
		

		CashLeg.CashLegBuilder prune();
	}

	/*********************** Immutable Implementation of CashLeg  ***********************/
	class CashLegImpl implements CashLeg {
		private final String ccy;
		private final String common;
		
		protected CashLegImpl(CashLeg.CashLegBuilder builder) {
			this.ccy = builder.getCcy();
			this.common = builder.getCommon();
		}
		
		@Override
		@RosettaAttribute("ccy")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("ccy")
		public String getCcy() {
			return ccy;
		}
		
		@Override
		@RosettaAttribute("common")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("common")
		public String getCommon() {
			return common;
		}
		
		@Override
		public CashLeg build() {
			return this;
		}
		
		@Override
		public CashLeg.CashLegBuilder toBuilder() {
			CashLeg.CashLegBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(CashLeg.CashLegBuilder builder) {
			ofNullable(getCcy()).ifPresent(builder::setCcy);
			ofNullable(getCommon()).ifPresent(builder::setCommon);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			CashLeg _that = getType().cast(o);
		
			if (!Objects.equals(ccy, _that.getCcy())) return false;
			if (!Objects.equals(common, _that.getCommon())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (ccy != null ? ccy.hashCode() : 0);
			_result = 31 * _result + (common != null ? common.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "CashLeg {" +
				"ccy=" + this.ccy + ", " +
				"common=" + this.common +
			'}';
		}
	}

	/*********************** Builder Implementation of CashLeg  ***********************/
	class CashLegBuilderImpl implements CashLeg.CashLegBuilder {
	
		protected String ccy;
		protected String common;
		
		@Override
		@RosettaAttribute("ccy")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("ccy")
		public String getCcy() {
			return ccy;
		}
		
		@Override
		@RosettaAttribute("common")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("common")
		public String getCommon() {
			return common;
		}
		
		@RosettaAttribute("ccy")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("ccy")
		@Override
		public CashLeg.CashLegBuilder setCcy(String _ccy) {
			this.ccy = _ccy == null ? null : _ccy;
			return this;
		}
		
		@RosettaAttribute("common")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("common")
		@Override
		public CashLeg.CashLegBuilder setCommon(String _common) {
			this.common = _common == null ? null : _common;
			return this;
		}
		
		@Override
		public CashLeg build() {
			return new CashLeg.CashLegImpl(this);
		}
		
		@Override
		public CashLeg.CashLegBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public CashLeg.CashLegBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getCcy()!=null) return true;
			if (getCommon()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public CashLeg.CashLegBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			CashLeg.CashLegBuilder o = (CashLeg.CashLegBuilder) other;
			
			
			merger.mergeBasic(getCcy(), o.getCcy(), this::setCcy);
			merger.mergeBasic(getCommon(), o.getCommon(), this::setCommon);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			CashLeg _that = getType().cast(o);
		
			if (!Objects.equals(ccy, _that.getCcy())) return false;
			if (!Objects.equals(common, _that.getCommon())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (ccy != null ? ccy.hashCode() : 0);
			_result = 31 * _result + (common != null ? common.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "CashLegBuilder {" +
				"ccy=" + this.ccy + ", " +
				"common=" + this.common +
			'}';
		}
	}
}
