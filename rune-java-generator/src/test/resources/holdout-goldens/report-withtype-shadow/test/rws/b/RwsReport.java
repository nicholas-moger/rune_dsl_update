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
import test.rws.b.meta.RwsReportMeta;

import static java.util.Optional.ofNullable;

/**
 * Report output type with rule references - the OWN namespace&#39;s, shadowing the imported one.
 * @version 0.0.0
 */
@RosettaDataType(value="RwsReport", builder=RwsReport.RwsReportBuilderImpl.class, version="0.0.0")
@RuneDataType(value="RwsReport", model="test", builder=RwsReport.RwsReportBuilderImpl.class, version="0.0.0")
public interface RwsReport extends RosettaModelObject {

	RwsReportMeta metaData = new RwsReportMeta();

	/*********************** Getter Methods  ***********************/
	String getUtiField();
	BigDecimal getNotionalField();

	/*********************** Build Methods  ***********************/
	RwsReport build();
	
	RwsReport.RwsReportBuilder toBuilder();
	
	static RwsReport.RwsReportBuilder builder() {
		return new RwsReport.RwsReportBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RwsReport> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RwsReport> getType() {
		return RwsReport.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("utiField"), String.class, getUtiField(), this);
		processor.processBasic(path.newSubPath("notionalField"), BigDecimal.class, getNotionalField(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RwsReportBuilder extends RwsReport, RosettaModelObjectBuilder {
		RwsReport.RwsReportBuilder setUtiField(String utiField);
		RwsReport.RwsReportBuilder setNotionalField(BigDecimal notionalField);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("utiField"), String.class, getUtiField(), this);
			processor.processBasic(path.newSubPath("notionalField"), BigDecimal.class, getNotionalField(), this);
		}
		

		RwsReport.RwsReportBuilder prune();
	}

	/*********************** Immutable Implementation of RwsReport  ***********************/
	class RwsReportImpl implements RwsReport {
		private final String utiField;
		private final BigDecimal notionalField;
		
		protected RwsReportImpl(RwsReport.RwsReportBuilder builder) {
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
		public RwsReport build() {
			return this;
		}
		
		@Override
		public RwsReport.RwsReportBuilder toBuilder() {
			RwsReport.RwsReportBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RwsReport.RwsReportBuilder builder) {
			ofNullable(getUtiField()).ifPresent(builder::setUtiField);
			ofNullable(getNotionalField()).ifPresent(builder::setNotionalField);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RwsReport _that = getType().cast(o);
		
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
			return "RwsReport {" +
				"utiField=" + this.utiField + ", " +
				"notionalField=" + this.notionalField +
			'}';
		}
	}

	/*********************** Builder Implementation of RwsReport  ***********************/
	class RwsReportBuilderImpl implements RwsReport.RwsReportBuilder {
	
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
		public RwsReport.RwsReportBuilder setUtiField(String _utiField) {
			this.utiField = _utiField == null ? null : _utiField;
			return this;
		}
		
		@RosettaAttribute("notionalField")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("notionalField")
		@Override
		public RwsReport.RwsReportBuilder setNotionalField(BigDecimal _notionalField) {
			this.notionalField = _notionalField == null ? null : _notionalField;
			return this;
		}
		
		@Override
		public RwsReport build() {
			return new RwsReport.RwsReportImpl(this);
		}
		
		@Override
		public RwsReport.RwsReportBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RwsReport.RwsReportBuilder prune() {
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
		public RwsReport.RwsReportBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RwsReport.RwsReportBuilder o = (RwsReport.RwsReportBuilder) other;
			
			
			merger.mergeBasic(getUtiField(), o.getUtiField(), this::setUtiField);
			merger.mergeBasic(getNotionalField(), o.getNotionalField(), this::setNotionalField);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RwsReport _that = getType().cast(o);
		
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
			return "RwsReportBuilder {" +
				"utiField=" + this.utiField + ", " +
				"notionalField=" + this.notionalField +
			'}';
		}
	}
}
