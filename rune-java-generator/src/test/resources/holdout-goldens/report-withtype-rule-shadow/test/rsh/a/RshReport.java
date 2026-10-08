package test.rsh.a;

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
import test.rsh.a.meta.RshReportMeta;

import static java.util.Optional.ofNullable;

/**
 * Report output type whose name a reporting rule in the same namespace shares.
 * @version 0.0.0
 */
@RosettaDataType(value="RshReport", builder=RshReport.RshReportBuilderImpl.class, version="0.0.0")
@RuneDataType(value="RshReport", model="test", builder=RshReport.RshReportBuilderImpl.class, version="0.0.0")
public interface RshReport extends RosettaModelObject {

	RshReportMeta metaData = new RshReportMeta();

	/*********************** Getter Methods  ***********************/
	String getUtiField();
	BigDecimal getNotionalField();

	/*********************** Build Methods  ***********************/
	RshReport build();
	
	RshReport.RshReportBuilder toBuilder();
	
	static RshReport.RshReportBuilder builder() {
		return new RshReport.RshReportBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends RshReport> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends RshReport> getType() {
		return RshReport.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("utiField"), String.class, getUtiField(), this);
		processor.processBasic(path.newSubPath("notionalField"), BigDecimal.class, getNotionalField(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RshReportBuilder extends RshReport, RosettaModelObjectBuilder {
		RshReport.RshReportBuilder setUtiField(String utiField);
		RshReport.RshReportBuilder setNotionalField(BigDecimal notionalField);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("utiField"), String.class, getUtiField(), this);
			processor.processBasic(path.newSubPath("notionalField"), BigDecimal.class, getNotionalField(), this);
		}
		

		RshReport.RshReportBuilder prune();
	}

	/*********************** Immutable Implementation of RshReport  ***********************/
	class RshReportImpl implements RshReport {
		private final String utiField;
		private final BigDecimal notionalField;
		
		protected RshReportImpl(RshReport.RshReportBuilder builder) {
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
		public RshReport build() {
			return this;
		}
		
		@Override
		public RshReport.RshReportBuilder toBuilder() {
			RshReport.RshReportBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RshReport.RshReportBuilder builder) {
			ofNullable(getUtiField()).ifPresent(builder::setUtiField);
			ofNullable(getNotionalField()).ifPresent(builder::setNotionalField);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RshReport _that = getType().cast(o);
		
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
			return "RshReport {" +
				"utiField=" + this.utiField + ", " +
				"notionalField=" + this.notionalField +
			'}';
		}
	}

	/*********************** Builder Implementation of RshReport  ***********************/
	class RshReportBuilderImpl implements RshReport.RshReportBuilder {
	
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
		public RshReport.RshReportBuilder setUtiField(String _utiField) {
			this.utiField = _utiField == null ? null : _utiField;
			return this;
		}
		
		@RosettaAttribute("notionalField")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("notionalField")
		@Override
		public RshReport.RshReportBuilder setNotionalField(BigDecimal _notionalField) {
			this.notionalField = _notionalField == null ? null : _notionalField;
			return this;
		}
		
		@Override
		public RshReport build() {
			return new RshReport.RshReportImpl(this);
		}
		
		@Override
		public RshReport.RshReportBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RshReport.RshReportBuilder prune() {
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
		public RshReport.RshReportBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RshReport.RshReportBuilder o = (RshReport.RshReportBuilder) other;
			
			
			merger.mergeBasic(getUtiField(), o.getUtiField(), this::setUtiField);
			merger.mergeBasic(getNotionalField(), o.getNotionalField(), this::setNotionalField);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RshReport _that = getType().cast(o);
		
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
			return "RshReportBuilder {" +
				"utiField=" + this.utiField + ", " +
				"notionalField=" + this.notionalField +
			'}';
		}
	}
}
