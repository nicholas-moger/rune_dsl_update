package chaos.s33.a2alias;

import chaos.s33.a2alias.meta.C33ReportMeta;
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
 * Report type.
 * @version 1.0.0
 */
@RosettaDataType(value="C33Report", builder=C33Report.C33ReportBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C33Report", model="chaos", builder=C33Report.C33ReportBuilderImpl.class, version="1.0.0")
public interface C33Report extends RosettaModelObject {

	C33ReportMeta metaData = new C33ReportMeta();

	/*********************** Getter Methods  ***********************/
	String getUtiField();
	String getLitField();

	/*********************** Build Methods  ***********************/
	C33Report build();
	
	C33Report.C33ReportBuilder toBuilder();
	
	static C33Report.C33ReportBuilder builder() {
		return new C33Report.C33ReportBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C33Report> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C33Report> getType() {
		return C33Report.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("utiField"), String.class, getUtiField(), this);
		processor.processBasic(path.newSubPath("litField"), String.class, getLitField(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C33ReportBuilder extends C33Report, RosettaModelObjectBuilder {
		C33Report.C33ReportBuilder setUtiField(String utiField);
		C33Report.C33ReportBuilder setLitField(String litField);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("utiField"), String.class, getUtiField(), this);
			processor.processBasic(path.newSubPath("litField"), String.class, getLitField(), this);
		}
		

		C33Report.C33ReportBuilder prune();
	}

	/*********************** Immutable Implementation of C33Report  ***********************/
	class C33ReportImpl implements C33Report {
		private final String utiField;
		private final String litField;
		
		protected C33ReportImpl(C33Report.C33ReportBuilder builder) {
			this.utiField = builder.getUtiField();
			this.litField = builder.getLitField();
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
		@RosettaAttribute("litField")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("litField")
		public String getLitField() {
			return litField;
		}
		
		@Override
		public C33Report build() {
			return this;
		}
		
		@Override
		public C33Report.C33ReportBuilder toBuilder() {
			C33Report.C33ReportBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C33Report.C33ReportBuilder builder) {
			ofNullable(getUtiField()).ifPresent(builder::setUtiField);
			ofNullable(getLitField()).ifPresent(builder::setLitField);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C33Report _that = getType().cast(o);
		
			if (!Objects.equals(utiField, _that.getUtiField())) return false;
			if (!Objects.equals(litField, _that.getLitField())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (utiField != null ? utiField.hashCode() : 0);
			_result = 31 * _result + (litField != null ? litField.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C33Report {" +
				"utiField=" + this.utiField + ", " +
				"litField=" + this.litField +
			'}';
		}
	}

	/*********************** Builder Implementation of C33Report  ***********************/
	class C33ReportBuilderImpl implements C33Report.C33ReportBuilder {
	
		protected String utiField;
		protected String litField;
		
		@Override
		@RosettaAttribute("utiField")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("utiField")
		public String getUtiField() {
			return utiField;
		}
		
		@Override
		@RosettaAttribute("litField")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("litField")
		public String getLitField() {
			return litField;
		}
		
		@RosettaAttribute("utiField")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("utiField")
		@Override
		public C33Report.C33ReportBuilder setUtiField(String _utiField) {
			this.utiField = _utiField == null ? null : _utiField;
			return this;
		}
		
		@RosettaAttribute("litField")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("litField")
		@Override
		public C33Report.C33ReportBuilder setLitField(String _litField) {
			this.litField = _litField == null ? null : _litField;
			return this;
		}
		
		@Override
		public C33Report build() {
			return new C33Report.C33ReportImpl(this);
		}
		
		@Override
		public C33Report.C33ReportBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C33Report.C33ReportBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getUtiField()!=null) return true;
			if (getLitField()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C33Report.C33ReportBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C33Report.C33ReportBuilder o = (C33Report.C33ReportBuilder) other;
			
			
			merger.mergeBasic(getUtiField(), o.getUtiField(), this::setUtiField);
			merger.mergeBasic(getLitField(), o.getLitField(), this::setLitField);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C33Report _that = getType().cast(o);
		
			if (!Objects.equals(utiField, _that.getUtiField())) return false;
			if (!Objects.equals(litField, _that.getLitField())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (utiField != null ? utiField.hashCode() : 0);
			_result = 31 * _result + (litField != null ? litField.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C33ReportBuilder {" +
				"utiField=" + this.utiField + ", " +
				"litField=" + this.litField +
			'}';
		}
	}
}
