package chaos.s28.a2wild;

import chaos.s28.a2wild.meta.C28ChoiceReportMeta;
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
 * The other option of the choice named as a report type.
 * @version 1.0.0
 */
@RosettaDataType(value="C28ChoiceReport", builder=C28ChoiceReport.C28ChoiceReportBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C28ChoiceReport", model="chaos", builder=C28ChoiceReport.C28ChoiceReportBuilderImpl.class, version="1.0.0")
public interface C28ChoiceReport extends RosettaModelObject {

	C28ChoiceReportMeta metaData = new C28ChoiceReportMeta();

	/*********************** Getter Methods  ***********************/
	String getUtiField();

	/*********************** Build Methods  ***********************/
	C28ChoiceReport build();
	
	C28ChoiceReport.C28ChoiceReportBuilder toBuilder();
	
	static C28ChoiceReport.C28ChoiceReportBuilder builder() {
		return new C28ChoiceReport.C28ChoiceReportBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C28ChoiceReport> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C28ChoiceReport> getType() {
		return C28ChoiceReport.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("utiField"), String.class, getUtiField(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C28ChoiceReportBuilder extends C28ChoiceReport, RosettaModelObjectBuilder {
		C28ChoiceReport.C28ChoiceReportBuilder setUtiField(String utiField);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("utiField"), String.class, getUtiField(), this);
		}
		

		C28ChoiceReport.C28ChoiceReportBuilder prune();
	}

	/*********************** Immutable Implementation of C28ChoiceReport  ***********************/
	class C28ChoiceReportImpl implements C28ChoiceReport {
		private final String utiField;
		
		protected C28ChoiceReportImpl(C28ChoiceReport.C28ChoiceReportBuilder builder) {
			this.utiField = builder.getUtiField();
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
		public C28ChoiceReport build() {
			return this;
		}
		
		@Override
		public C28ChoiceReport.C28ChoiceReportBuilder toBuilder() {
			C28ChoiceReport.C28ChoiceReportBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C28ChoiceReport.C28ChoiceReportBuilder builder) {
			ofNullable(getUtiField()).ifPresent(builder::setUtiField);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28ChoiceReport _that = getType().cast(o);
		
			if (!Objects.equals(utiField, _that.getUtiField())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (utiField != null ? utiField.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C28ChoiceReport {" +
				"utiField=" + this.utiField +
			'}';
		}
	}

	/*********************** Builder Implementation of C28ChoiceReport  ***********************/
	class C28ChoiceReportBuilderImpl implements C28ChoiceReport.C28ChoiceReportBuilder {
	
		protected String utiField;
		
		@Override
		@RosettaAttribute("utiField")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("utiField")
		public String getUtiField() {
			return utiField;
		}
		
		@RosettaAttribute("utiField")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("utiField")
		@Override
		public C28ChoiceReport.C28ChoiceReportBuilder setUtiField(String _utiField) {
			this.utiField = _utiField == null ? null : _utiField;
			return this;
		}
		
		@Override
		public C28ChoiceReport build() {
			return new C28ChoiceReport.C28ChoiceReportImpl(this);
		}
		
		@Override
		public C28ChoiceReport.C28ChoiceReportBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28ChoiceReport.C28ChoiceReportBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getUtiField()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28ChoiceReport.C28ChoiceReportBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C28ChoiceReport.C28ChoiceReportBuilder o = (C28ChoiceReport.C28ChoiceReportBuilder) other;
			
			
			merger.mergeBasic(getUtiField(), o.getUtiField(), this::setUtiField);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28ChoiceReport _that = getType().cast(o);
		
			if (!Objects.equals(utiField, _that.getUtiField())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (utiField != null ? utiField.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C28ChoiceReportBuilder {" +
				"utiField=" + this.utiField +
			'}';
		}
	}
}
