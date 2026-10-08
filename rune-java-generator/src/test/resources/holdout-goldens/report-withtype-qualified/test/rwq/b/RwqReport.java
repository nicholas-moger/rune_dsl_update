package test.rwq.b;

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
import test.rwq.b.meta.RwqReportMeta;

import static java.util.Optional.ofNullable;

/**
 * A same-named DECOY report type in the report&#39;s own namespace.
 * @version 0.0.0
 */
@RosettaDataType(value="RwqReport", builder=RwqReport.RwqReportBuilderImpl.class, version="0.0.0")
@RuneDataType(value="RwqReport", model="test", builder=RwqReport.RwqReportBuilderImpl.class, version="0.0.0")
public interface RwqReport extends RosettaModelObject {

	RwqReportMeta metaData = new RwqReportMeta();

	/*********************** Getter Methods  ***********************/
	String getUtiField();

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
	}
	

	/*********************** Builder Interface  ***********************/
	interface RwqReportBuilder extends RwqReport, RosettaModelObjectBuilder {
		RwqReport.RwqReportBuilder setUtiField(String utiField);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("utiField"), String.class, getUtiField(), this);
		}
		

		RwqReport.RwqReportBuilder prune();
	}

	/*********************** Immutable Implementation of RwqReport  ***********************/
	class RwqReportImpl implements RwqReport {
		private final String utiField;
		
		protected RwqReportImpl(RwqReport.RwqReportBuilder builder) {
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
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RwqReport _that = getType().cast(o);
		
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
			return "RwqReport {" +
				"utiField=" + this.utiField +
			'}';
		}
	}

	/*********************** Builder Implementation of RwqReport  ***********************/
	class RwqReportBuilderImpl implements RwqReport.RwqReportBuilder {
	
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
		public RwqReport.RwqReportBuilder setUtiField(String _utiField) {
			this.utiField = _utiField == null ? null : _utiField;
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
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RwqReport.RwqReportBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RwqReport.RwqReportBuilder o = (RwqReport.RwqReportBuilder) other;
			
			
			merger.mergeBasic(getUtiField(), o.getUtiField(), this::setUtiField);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			RwqReport _that = getType().cast(o);
		
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
			return "RwqReportBuilder {" +
				"utiField=" + this.utiField +
			'}';
		}
	}
}
