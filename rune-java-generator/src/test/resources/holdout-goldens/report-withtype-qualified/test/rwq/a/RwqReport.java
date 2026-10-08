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
import test.rwq.a.meta.RwqReportMeta;

import static java.util.Optional.ofNullable;

/**
 * Report output type with rule references.
 * @version 0.0.0
 */
@RosettaDataType(value="RwqReport", builder=RwqReport.RwqReportBuilderImpl.class, version="0.0.0")
@RuneDataType(value="RwqReport", model="test", builder=RwqReport.RwqReportBuilderImpl.class, version="0.0.0")
public interface RwqReport extends RosettaModelObject {

	RwqReportMeta metaData = new RwqReportMeta();

	/*********************** Getter Methods  ***********************/
	String getUtiField();
	BigDecimal getNotionalField();

	/*********************** Build Methods  ***********************/
	RwqReport build();
	
	RwqReport.RwqReportBuilder toBuilder();
	
	static RwqReport.RwqReportBuilder builder() {
		return new RwqReport.RwqReportBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RwqReport> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RwqReport> getType() {
		return RwqReport.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("utiField"), String.class, getUtiField(), this);
		processor.processBasic(path.newSubPath("notionalField"), BigDecimal.class, getNotionalField(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RwqReportBuilder extends RwqReport, RosettaModelObjectBuilder {
		RwqReport.RwqReportBuilder setUtiField(String utiField);
		RwqReport.RwqReportBuilder setNotionalField(BigDecimal notionalField);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("utiField"), String.class, getUtiField(), this);
			processor.processBasic(path.newSubPath("notionalField"), BigDecimal.class, getNotionalField(), this);
		}
		

		RwqReport.RwqReportBuilder prune();
	}

	/*********************** Immutable Implementation of RwqReport  ***********************/
	class RwqReportImpl implements RwqReport {
		private final String utiField;
		private final BigDecimal notionalField;
		
		protected RwqReportImpl(RwqReport.RwqReportBuilder builder) {
			this.utiField = builder.getUtiField();
			this.notionalField = builder.getNotionalField();
		}
		
		@Override
		@RosettaAttribute("utiField")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("utiField")
		public String getUtiField() {
			return utiField;
		}
		
		@Override
		@RosettaAttribute("notionalField")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("notionalField")
		public BigDecimal getNotionalField() {
			return notionalField;
		}
		
		@Override
		public RwqReport build() {
			return this;
		}
		
		@Override
		public RwqReport.RwqReportBuilder toBuilder() {
			RwqReport.RwqReportBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RwqReport.RwqReportBuilder builder) {
			ofNullable(getUtiField()).ifPresent(builder::setUtiField);
			ofNullable(getNotionalField()).ifPresent(builder::setNotionalField);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RwqReport _that = getType().cast(o);
		
			if (!Objects.equals(utiField, _that.getUtiField())) return false;
			if (!Objects.equals(notionalField, _that.getNotionalField())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (utiField != null ? utiField.hashCode() : 0);
			_result = 31 * _result + (notionalField != null ? notionalField.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RwqReport {" +
				"utiField=" + this.utiField + ", " +
				"notionalField=" + this.notionalField +
			'}';
		}
	}

	/*********************** Builder Implementation of RwqReport  ***********************/
	class RwqReportBuilderImpl implements RwqReport.RwqReportBuilder {
	
		protected String utiField;
		protected BigDecimal notionalField;
		
		@Override
		@RosettaAttribute("utiField")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("utiField")
		public String getUtiField() {
			return utiField;
		}
		
		@Override
		@RosettaAttribute("notionalField")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("notionalField")
		public BigDecimal getNotionalField() {
			return notionalField;
		}
		
		@RosettaAttribute("utiField")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("utiField")
		@Override
		public RwqReport.RwqReportBuilder setUtiField(String _utiField) {
			this.utiField = _utiField == null ? null : _utiField;
			return this;
		}
		
		@RosettaAttribute("notionalField")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("notionalField")
		@Override
		public RwqReport.RwqReportBuilder setNotionalField(BigDecimal _notionalField) {
			this.notionalField = _notionalField == null ? null : _notionalField;
			return this;
		}
		
		@Override
		public RwqReport build() {
			return new RwqReport.RwqReportImpl(this);
		}
		
		@Override
		public RwqReport.RwqReportBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RwqReport.RwqReportBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getUtiField()!=null) return true;
			if (getNotionalField()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RwqReport.RwqReportBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RwqReport.RwqReportBuilder o = (RwqReport.RwqReportBuilder) other;
			
			
			merger.mergeBasic(getUtiField(), o.getUtiField(), this::setUtiField);
			merger.mergeBasic(getNotionalField(), o.getNotionalField(), this::setNotionalField);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RwqReport _that = getType().cast(o);
		
			if (!Objects.equals(utiField, _that.getUtiField())) return false;
			if (!Objects.equals(notionalField, _that.getNotionalField())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (utiField != null ? utiField.hashCode() : 0);
			_result = 31 * _result + (notionalField != null ? notionalField.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RwqReportBuilder {" +
				"utiField=" + this.utiField + ", " +
				"notionalField=" + this.notionalField +
			'}';
		}
	}
}
