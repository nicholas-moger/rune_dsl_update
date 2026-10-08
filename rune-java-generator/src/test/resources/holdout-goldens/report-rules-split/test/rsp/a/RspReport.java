package test.rsp.a;

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
import test.rsp.a.meta.RspReportMeta;

import static java.util.Optional.ofNullable;

/**
 * Report output type with rule references.
 * @version 0.0.0
 */
@RosettaDataType(value="RspReport", builder=RspReport.RspReportBuilderImpl.class, version="0.0.0")
@RuneDataType(value="RspReport", model="test", builder=RspReport.RspReportBuilderImpl.class, version="0.0.0")
public interface RspReport extends RosettaModelObject {

	RspReportMeta metaData = new RspReportMeta();

	/*********************** Getter Methods  ***********************/
	String getUtiField();
	BigDecimal getNotionalField();

	/*********************** Build Methods  ***********************/
	RspReport build();
	
	RspReport.RspReportBuilder toBuilder();
	
	static RspReport.RspReportBuilder builder() {
		return new RspReport.RspReportBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RspReport> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RspReport> getType() {
		return RspReport.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("utiField"), String.class, getUtiField(), this);
		processor.processBasic(path.newSubPath("notionalField"), BigDecimal.class, getNotionalField(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RspReportBuilder extends RspReport, RosettaModelObjectBuilder {
		RspReport.RspReportBuilder setUtiField(String utiField);
		RspReport.RspReportBuilder setNotionalField(BigDecimal notionalField);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("utiField"), String.class, getUtiField(), this);
			processor.processBasic(path.newSubPath("notionalField"), BigDecimal.class, getNotionalField(), this);
		}
		

		RspReport.RspReportBuilder prune();
	}

	/*********************** Immutable Implementation of RspReport  ***********************/
	class RspReportImpl implements RspReport {
		private final String utiField;
		private final BigDecimal notionalField;
		
		protected RspReportImpl(RspReport.RspReportBuilder builder) {
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
		public RspReport build() {
			return this;
		}
		
		@Override
		public RspReport.RspReportBuilder toBuilder() {
			RspReport.RspReportBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RspReport.RspReportBuilder builder) {
			ofNullable(getUtiField()).ifPresent(builder::setUtiField);
			ofNullable(getNotionalField()).ifPresent(builder::setNotionalField);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RspReport _that = getType().cast(o);
		
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
			return "RspReport {" +
				"utiField=" + this.utiField + ", " +
				"notionalField=" + this.notionalField +
			'}';
		}
	}

	/*********************** Builder Implementation of RspReport  ***********************/
	class RspReportBuilderImpl implements RspReport.RspReportBuilder {
	
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
		public RspReport.RspReportBuilder setUtiField(String _utiField) {
			this.utiField = _utiField == null ? null : _utiField;
			return this;
		}
		
		@RosettaAttribute("notionalField")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("notionalField")
		@Override
		public RspReport.RspReportBuilder setNotionalField(BigDecimal _notionalField) {
			this.notionalField = _notionalField == null ? null : _notionalField;
			return this;
		}
		
		@Override
		public RspReport build() {
			return new RspReport.RspReportImpl(this);
		}
		
		@Override
		public RspReport.RspReportBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RspReport.RspReportBuilder prune() {
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
		public RspReport.RspReportBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RspReport.RspReportBuilder o = (RspReport.RspReportBuilder) other;
			
			
			merger.mergeBasic(getUtiField(), o.getUtiField(), this::setUtiField);
			merger.mergeBasic(getNotionalField(), o.getNotionalField(), this::setNotionalField);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RspReport _that = getType().cast(o);
		
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
			return "RspReportBuilder {" +
				"utiField=" + this.utiField + ", " +
				"notionalField=" + this.notionalField +
			'}';
		}
	}
}
