package test.reg;

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
import test.reg.meta.OrganisationReportMeta;

import static java.util.Optional.ofNullable;

/**
 * Repeated rule
 * @version test
 */
@RosettaDataType(value="OrganisationReport", builder=OrganisationReport.OrganisationReportBuilderImpl.class, version="test")
@RuneDataType(value="OrganisationReport", model="test", builder=OrganisationReport.OrganisationReportBuilderImpl.class, version="test")
public interface OrganisationReport extends RosettaModelObject {

	OrganisationReportMeta metaData = new OrganisationReportMeta();

	/*********************** Getter Methods  ***********************/
	String getName();
	Boolean getIsGovernmentAgency();
	CountryEnum getCountry();

	/*********************** Build Methods  ***********************/
	OrganisationReport build();
	
	OrganisationReport.OrganisationReportBuilder toBuilder();
	
	static OrganisationReport.OrganisationReportBuilder builder() {
		return new OrganisationReport.OrganisationReportBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends OrganisationReport> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends OrganisationReport> getType() {
		return OrganisationReport.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
		processor.processBasic(path.newSubPath("isGovernmentAgency"), Boolean.class, getIsGovernmentAgency(), this);
		processor.processBasic(path.newSubPath("country"), CountryEnum.class, getCountry(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface OrganisationReportBuilder extends OrganisationReport, RosettaModelObjectBuilder {
		OrganisationReport.OrganisationReportBuilder setName(String name);
		OrganisationReport.OrganisationReportBuilder setIsGovernmentAgency(Boolean isGovernmentAgency);
		OrganisationReport.OrganisationReportBuilder setCountry(CountryEnum country);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
			processor.processBasic(path.newSubPath("isGovernmentAgency"), Boolean.class, getIsGovernmentAgency(), this);
			processor.processBasic(path.newSubPath("country"), CountryEnum.class, getCountry(), this);
		}
		

		OrganisationReport.OrganisationReportBuilder prune();
	}

	/*********************** Immutable Implementation of OrganisationReport  ***********************/
	class OrganisationReportImpl implements OrganisationReport {
		private final String name;
		private final Boolean isGovernmentAgency;
		private final CountryEnum country;
		
		protected OrganisationReportImpl(OrganisationReport.OrganisationReportBuilder builder) {
			this.name = builder.getName();
			this.isGovernmentAgency = builder.getIsGovernmentAgency();
			this.country = builder.getCountry();
		}
		
		@Override
		@RosettaAttribute("name")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("name")
		public String getName() {
			return name;
		}
		
		@Override
		@RosettaAttribute("isGovernmentAgency")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("isGovernmentAgency")
		public Boolean getIsGovernmentAgency() {
			return isGovernmentAgency;
		}
		
		@Override
		@RosettaAttribute("country")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("country")
		public CountryEnum getCountry() {
			return country;
		}
		
		@Override
		public OrganisationReport build() {
			return this;
		}
		
		@Override
		public OrganisationReport.OrganisationReportBuilder toBuilder() {
			OrganisationReport.OrganisationReportBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(OrganisationReport.OrganisationReportBuilder builder) {
			ofNullable(getName()).ifPresent(builder::setName);
			ofNullable(getIsGovernmentAgency()).ifPresent(builder::setIsGovernmentAgency);
			ofNullable(getCountry()).ifPresent(builder::setCountry);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			OrganisationReport _that = getType().cast(o);
		
			if (!Objects.equals(name, _that.getName())) return false;
			if (!Objects.equals(isGovernmentAgency, _that.getIsGovernmentAgency())) return false;
			if (!Objects.equals(country, _that.getCountry())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			_result = 31 * _result + (isGovernmentAgency != null ? isGovernmentAgency.hashCode() : 0);
			_result = 31 * _result + (country != null ? country.getClass().getName().hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "OrganisationReport {" +
				"name=" + this.name + ", " +
				"isGovernmentAgency=" + this.isGovernmentAgency + ", " +
				"country=" + this.country +
			'}';
		}
	}

	/*********************** Builder Implementation of OrganisationReport  ***********************/
	class OrganisationReportBuilderImpl implements OrganisationReport.OrganisationReportBuilder {
	
		protected String name;
		protected Boolean isGovernmentAgency;
		protected CountryEnum country;
		
		@Override
		@RosettaAttribute("name")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("name")
		public String getName() {
			return name;
		}
		
		@Override
		@RosettaAttribute("isGovernmentAgency")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("isGovernmentAgency")
		public Boolean getIsGovernmentAgency() {
			return isGovernmentAgency;
		}
		
		@Override
		@RosettaAttribute("country")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("country")
		public CountryEnum getCountry() {
			return country;
		}
		
		@RosettaAttribute("name")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("name")
		@Override
		public OrganisationReport.OrganisationReportBuilder setName(String _name) {
			this.name = _name == null ? null : _name;
			return this;
		}
		
		@RosettaAttribute("isGovernmentAgency")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("isGovernmentAgency")
		@Override
		public OrganisationReport.OrganisationReportBuilder setIsGovernmentAgency(Boolean _isGovernmentAgency) {
			this.isGovernmentAgency = _isGovernmentAgency == null ? null : _isGovernmentAgency;
			return this;
		}
		
		@RosettaAttribute("country")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("country")
		@Override
		public OrganisationReport.OrganisationReportBuilder setCountry(CountryEnum _country) {
			this.country = _country == null ? null : _country;
			return this;
		}
		
		@Override
		public OrganisationReport build() {
			return new OrganisationReport.OrganisationReportImpl(this);
		}
		
		@Override
		public OrganisationReport.OrganisationReportBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public OrganisationReport.OrganisationReportBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getName()!=null) return true;
			if (getIsGovernmentAgency()!=null) return true;
			if (getCountry()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public OrganisationReport.OrganisationReportBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			OrganisationReport.OrganisationReportBuilder o = (OrganisationReport.OrganisationReportBuilder) other;
			
			
			merger.mergeBasic(getName(), o.getName(), this::setName);
			merger.mergeBasic(getIsGovernmentAgency(), o.getIsGovernmentAgency(), this::setIsGovernmentAgency);
			merger.mergeBasic(getCountry(), o.getCountry(), this::setCountry);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			OrganisationReport _that = getType().cast(o);
		
			if (!Objects.equals(name, _that.getName())) return false;
			if (!Objects.equals(isGovernmentAgency, _that.getIsGovernmentAgency())) return false;
			if (!Objects.equals(country, _that.getCountry())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			_result = 31 * _result + (isGovernmentAgency != null ? isGovernmentAgency.hashCode() : 0);
			_result = 31 * _result + (country != null ? country.getClass().getName().hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "OrganisationReportBuilder {" +
				"name=" + this.name + ", " +
				"isGovernmentAgency=" + this.isGovernmentAgency + ", " +
				"country=" + this.country +
			'}';
		}
	}
}
