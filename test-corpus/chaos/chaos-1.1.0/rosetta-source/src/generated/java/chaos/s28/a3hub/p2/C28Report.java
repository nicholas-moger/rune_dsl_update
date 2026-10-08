package chaos.s28.a3hub.p2;

import chaos.s28.a3hub.p2.meta.C28ReportMeta;
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
 * Report output type.
 * @version 1.0.0
 */
@RosettaDataType(value="C28Report", builder=C28Report.C28ReportBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C28Report", model="chaos", builder=C28Report.C28ReportBuilderImpl.class, version="1.0.0")
public interface C28Report extends RosettaModelObject {

	C28ReportMeta metaData = new C28ReportMeta();

	/*********************** Getter Methods  ***********************/
	String getUtiField();
	String getAvField();
	String getVenueField();

	/*********************** Build Methods  ***********************/
	C28Report build();
	
	C28Report.C28ReportBuilder toBuilder();
	
	static C28Report.C28ReportBuilder builder() {
		return new C28Report.C28ReportBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C28Report> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C28Report> getType() {
		return C28Report.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("utiField"), String.class, getUtiField(), this);
		processor.processBasic(path.newSubPath("avField"), String.class, getAvField(), this);
		processor.processBasic(path.newSubPath("venueField"), String.class, getVenueField(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C28ReportBuilder extends C28Report, RosettaModelObjectBuilder {
		C28Report.C28ReportBuilder setUtiField(String utiField);
		C28Report.C28ReportBuilder setAvField(String avField);
		C28Report.C28ReportBuilder setVenueField(String venueField);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("utiField"), String.class, getUtiField(), this);
			processor.processBasic(path.newSubPath("avField"), String.class, getAvField(), this);
			processor.processBasic(path.newSubPath("venueField"), String.class, getVenueField(), this);
		}
		

		C28Report.C28ReportBuilder prune();
	}

	/*********************** Immutable Implementation of C28Report  ***********************/
	class C28ReportImpl implements C28Report {
		private final String utiField;
		private final String avField;
		private final String venueField;
		
		protected C28ReportImpl(C28Report.C28ReportBuilder builder) {
			this.utiField = builder.getUtiField();
			this.avField = builder.getAvField();
			this.venueField = builder.getVenueField();
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
		@RosettaAttribute("avField")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("avField")
		public String getAvField() {
			return avField;
		}
		
		@Override
		@RosettaAttribute("venueField")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("venueField")
		public String getVenueField() {
			return venueField;
		}
		
		@Override
		public C28Report build() {
			return this;
		}
		
		@Override
		public C28Report.C28ReportBuilder toBuilder() {
			C28Report.C28ReportBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C28Report.C28ReportBuilder builder) {
			ofNullable(getUtiField()).ifPresent(builder::setUtiField);
			ofNullable(getAvField()).ifPresent(builder::setAvField);
			ofNullable(getVenueField()).ifPresent(builder::setVenueField);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28Report _that = getType().cast(o);
		
			if (!Objects.equals(utiField, _that.getUtiField())) return false;
			if (!Objects.equals(avField, _that.getAvField())) return false;
			if (!Objects.equals(venueField, _that.getVenueField())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (utiField != null ? utiField.hashCode() : 0);
			_result = 31 * _result + (avField != null ? avField.hashCode() : 0);
			_result = 31 * _result + (venueField != null ? venueField.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C28Report {" +
				"utiField=" + this.utiField + ", " +
				"avField=" + this.avField + ", " +
				"venueField=" + this.venueField +
			'}';
		}
	}

	/*********************** Builder Implementation of C28Report  ***********************/
	class C28ReportBuilderImpl implements C28Report.C28ReportBuilder {
	
		protected String utiField;
		protected String avField;
		protected String venueField;
		
		@Override
		@RosettaAttribute("utiField")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("utiField")
		public String getUtiField() {
			return utiField;
		}
		
		@Override
		@RosettaAttribute("avField")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("avField")
		public String getAvField() {
			return avField;
		}
		
		@Override
		@RosettaAttribute("venueField")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("venueField")
		public String getVenueField() {
			return venueField;
		}
		
		@RosettaAttribute("utiField")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("utiField")
		@Override
		public C28Report.C28ReportBuilder setUtiField(String _utiField) {
			this.utiField = _utiField == null ? null : _utiField;
			return this;
		}
		
		@RosettaAttribute("avField")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("avField")
		@Override
		public C28Report.C28ReportBuilder setAvField(String _avField) {
			this.avField = _avField == null ? null : _avField;
			return this;
		}
		
		@RosettaAttribute("venueField")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("venueField")
		@Override
		public C28Report.C28ReportBuilder setVenueField(String _venueField) {
			this.venueField = _venueField == null ? null : _venueField;
			return this;
		}
		
		@Override
		public C28Report build() {
			return new C28Report.C28ReportImpl(this);
		}
		
		@Override
		public C28Report.C28ReportBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28Report.C28ReportBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getUtiField()!=null) return true;
			if (getAvField()!=null) return true;
			if (getVenueField()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C28Report.C28ReportBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C28Report.C28ReportBuilder o = (C28Report.C28ReportBuilder) other;
			
			
			merger.mergeBasic(getUtiField(), o.getUtiField(), this::setUtiField);
			merger.mergeBasic(getAvField(), o.getAvField(), this::setAvField);
			merger.mergeBasic(getVenueField(), o.getVenueField(), this::setVenueField);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C28Report _that = getType().cast(o);
		
			if (!Objects.equals(utiField, _that.getUtiField())) return false;
			if (!Objects.equals(avField, _that.getAvField())) return false;
			if (!Objects.equals(venueField, _that.getVenueField())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (utiField != null ? utiField.hashCode() : 0);
			_result = 31 * _result + (avField != null ? avField.hashCode() : 0);
			_result = 31 * _result + (venueField != null ? venueField.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C28ReportBuilder {" +
				"utiField=" + this.utiField + ", " +
				"avField=" + this.avField + ", " +
				"venueField=" + this.venueField +
			'}';
		}
	}
}
