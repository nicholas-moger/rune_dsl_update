package chaos.s07.a2wild;

import chaos.s07.a2wild.meta.C7ReportMeta;
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

import static java.util.Optional.ofNullable;

/**
 * Report output type with rule references.
 * @version 1.0.0
 */
@RosettaDataType(value="C7Report", builder=C7Report.C7ReportBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C7Report", model="chaos", builder=C7Report.C7ReportBuilderImpl.class, version="1.0.0")
public interface C7Report extends RosettaModelObject {

	C7ReportMeta metaData = new C7ReportMeta();

	/*********************** Getter Methods  ***********************/
	String getUtiField();
	BigDecimal getNotionalField();

	/*********************** Build Methods  ***********************/
	C7Report build();
	
	C7Report.C7ReportBuilder toBuilder();
	
	static C7Report.C7ReportBuilder builder() {
		return new C7Report.C7ReportBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C7Report> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C7Report> getType() {
		return C7Report.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("utiField"), String.class, getUtiField(), this);
		processor.processBasic(path.newSubPath("notionalField"), BigDecimal.class, getNotionalField(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C7ReportBuilder extends C7Report, RosettaModelObjectBuilder {
		C7Report.C7ReportBuilder setUtiField(String utiField);
		C7Report.C7ReportBuilder setNotionalField(BigDecimal notionalField);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("utiField"), String.class, getUtiField(), this);
			processor.processBasic(path.newSubPath("notionalField"), BigDecimal.class, getNotionalField(), this);
		}
		

		C7Report.C7ReportBuilder prune();
	}

	/*********************** Immutable Implementation of C7Report  ***********************/
	class C7ReportImpl implements C7Report {
		private final String utiField;
		private final BigDecimal notionalField;
		
		protected C7ReportImpl(C7Report.C7ReportBuilder builder) {
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
		public C7Report build() {
			return this;
		}
		
		@Override
		public C7Report.C7ReportBuilder toBuilder() {
			C7Report.C7ReportBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C7Report.C7ReportBuilder builder) {
			ofNullable(getUtiField()).ifPresent(builder::setUtiField);
			ofNullable(getNotionalField()).ifPresent(builder::setNotionalField);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C7Report _that = getType().cast(o);
		
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
			return "C7Report {" +
				"utiField=" + this.utiField + ", " +
				"notionalField=" + this.notionalField +
			'}';
		}
	}

	/*********************** Builder Implementation of C7Report  ***********************/
	class C7ReportBuilderImpl implements C7Report.C7ReportBuilder {
	
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
		public C7Report.C7ReportBuilder setUtiField(String _utiField) {
			this.utiField = _utiField == null ? null : _utiField;
			return this;
		}
		
		@RosettaAttribute("notionalField")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("notionalField")
		@Override
		public C7Report.C7ReportBuilder setNotionalField(BigDecimal _notionalField) {
			this.notionalField = _notionalField == null ? null : _notionalField;
			return this;
		}
		
		@Override
		public C7Report build() {
			return new C7Report.C7ReportImpl(this);
		}
		
		@Override
		public C7Report.C7ReportBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C7Report.C7ReportBuilder prune() {
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
		public C7Report.C7ReportBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C7Report.C7ReportBuilder o = (C7Report.C7ReportBuilder) other;
			
			
			merger.mergeBasic(getUtiField(), o.getUtiField(), this::setUtiField);
			merger.mergeBasic(getNotionalField(), o.getNotionalField(), this::setNotionalField);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C7Report _that = getType().cast(o);
		
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
			return "C7ReportBuilder {" +
				"utiField=" + this.utiField + ", " +
				"notionalField=" + this.notionalField +
			'}';
		}
	}
}
