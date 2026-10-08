package chaos.s03.base;

import chaos.s03.base.meta.C3CashLegMeta;
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
 * Option 1 - shares &#39;common&#39; with its sibling.
 * @version 1.0.0
 */
@RosettaDataType(value="C3CashLeg", builder=C3CashLeg.C3CashLegBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C3CashLeg", model="chaos", builder=C3CashLeg.C3CashLegBuilderImpl.class, version="1.0.0")
public interface C3CashLeg extends RosettaModelObject {

	C3CashLegMeta metaData = new C3CashLegMeta();

	/*********************** Getter Methods  ***********************/
	String getCcy();
	String getCommon();

	/*********************** Build Methods  ***********************/
	C3CashLeg build();
	
	C3CashLeg.C3CashLegBuilder toBuilder();
	
	static C3CashLeg.C3CashLegBuilder builder() {
		return new C3CashLeg.C3CashLegBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C3CashLeg> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C3CashLeg> getType() {
		return C3CashLeg.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("ccy"), String.class, getCcy(), this);
		processor.processBasic(path.newSubPath("common"), String.class, getCommon(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C3CashLegBuilder extends C3CashLeg, RosettaModelObjectBuilder {
		C3CashLeg.C3CashLegBuilder setCcy(String ccy);
		C3CashLeg.C3CashLegBuilder setCommon(String common);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("ccy"), String.class, getCcy(), this);
			processor.processBasic(path.newSubPath("common"), String.class, getCommon(), this);
		}
		

		C3CashLeg.C3CashLegBuilder prune();
	}

	/*********************** Immutable Implementation of C3CashLeg  ***********************/
	class C3CashLegImpl implements C3CashLeg {
		private final String ccy;
		private final String common;
		
		protected C3CashLegImpl(C3CashLeg.C3CashLegBuilder builder) {
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
		public C3CashLeg build() {
			return this;
		}
		
		@Override
		public C3CashLeg.C3CashLegBuilder toBuilder() {
			C3CashLeg.C3CashLegBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C3CashLeg.C3CashLegBuilder builder) {
			ofNullable(getCcy()).ifPresent(builder::setCcy);
			ofNullable(getCommon()).ifPresent(builder::setCommon);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C3CashLeg _that = getType().cast(o);
		
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
			return "C3CashLeg {" +
				"ccy=" + this.ccy + ", " +
				"common=" + this.common +
			'}';
		}
	}

	/*********************** Builder Implementation of C3CashLeg  ***********************/
	class C3CashLegBuilderImpl implements C3CashLeg.C3CashLegBuilder {
	
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
		public C3CashLeg.C3CashLegBuilder setCcy(String _ccy) {
			this.ccy = _ccy == null ? null : _ccy;
			return this;
		}
		
		@RosettaAttribute("common")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("common")
		@Override
		public C3CashLeg.C3CashLegBuilder setCommon(String _common) {
			this.common = _common == null ? null : _common;
			return this;
		}
		
		@Override
		public C3CashLeg build() {
			return new C3CashLeg.C3CashLegImpl(this);
		}
		
		@Override
		public C3CashLeg.C3CashLegBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C3CashLeg.C3CashLegBuilder prune() {
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
		public C3CashLeg.C3CashLegBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C3CashLeg.C3CashLegBuilder o = (C3CashLeg.C3CashLegBuilder) other;
			
			
			merger.mergeBasic(getCcy(), o.getCcy(), this::setCcy);
			merger.mergeBasic(getCommon(), o.getCommon(), this::setCommon);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C3CashLeg _that = getType().cast(o);
		
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
			return "C3CashLegBuilder {" +
				"ccy=" + this.ccy + ", " +
				"common=" + this.common +
			'}';
		}
	}
}
