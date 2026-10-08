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
import test.reg.meta.OrganisationMeta;

import static java.util.Optional.ofNullable;

/**
 * @version test
 */
@RosettaDataType(value="Organisation", builder=Organisation.OrganisationBuilderImpl.class, version="test")
@RuneDataType(value="Organisation", model="test", builder=Organisation.OrganisationBuilderImpl.class, version="test")
public interface Organisation extends RosettaModelObject {

	OrganisationMeta metaData = new OrganisationMeta();

	/*********************** Getter Methods  ***********************/
	String getName();
	Boolean getIsGovernmentAgency();
	CountryEnum getCountry();

	/*********************** Build Methods  ***********************/
	Organisation build();
	
	Organisation.OrganisationBuilder toBuilder();
	
	static Organisation.OrganisationBuilder builder() {
		return new Organisation.OrganisationBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Organisation> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Organisation> getType() {
		return Organisation.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
		processor.processBasic(path.newSubPath("isGovernmentAgency"), Boolean.class, getIsGovernmentAgency(), this);
		processor.processBasic(path.newSubPath("country"), CountryEnum.class, getCountry(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface OrganisationBuilder extends Organisation, RosettaModelObjectBuilder {
		Organisation.OrganisationBuilder setName(String name);
		Organisation.OrganisationBuilder setIsGovernmentAgency(Boolean isGovernmentAgency);
		Organisation.OrganisationBuilder setCountry(CountryEnum country);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
			processor.processBasic(path.newSubPath("isGovernmentAgency"), Boolean.class, getIsGovernmentAgency(), this);
			processor.processBasic(path.newSubPath("country"), CountryEnum.class, getCountry(), this);
		}
		

		Organisation.OrganisationBuilder prune();
	}

	/*********************** Immutable Implementation of Organisation  ***********************/
	class OrganisationImpl implements Organisation {
		private final String name;
		private final Boolean isGovernmentAgency;
		private final CountryEnum country;
		
		protected OrganisationImpl(Organisation.OrganisationBuilder builder) {
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
		public Organisation build() {
			return this;
		}
		
		@Override
		public Organisation.OrganisationBuilder toBuilder() {
			Organisation.OrganisationBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Organisation.OrganisationBuilder builder) {
			ofNullable(getName()).ifPresent(builder::setName);
			ofNullable(getIsGovernmentAgency()).ifPresent(builder::setIsGovernmentAgency);
			ofNullable(getCountry()).ifPresent(builder::setCountry);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Organisation _that = getType().cast(o);
		
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
			return "Organisation {" +
				"name=" + this.name + ", " +
				"isGovernmentAgency=" + this.isGovernmentAgency + ", " +
				"country=" + this.country +
			'}';
		}
	}

	/*********************** Builder Implementation of Organisation  ***********************/
	class OrganisationBuilderImpl implements Organisation.OrganisationBuilder {
	
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
		public Organisation.OrganisationBuilder setName(String _name) {
			this.name = _name == null ? null : _name;
			return this;
		}
		
		@RosettaAttribute("isGovernmentAgency")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("isGovernmentAgency")
		@Override
		public Organisation.OrganisationBuilder setIsGovernmentAgency(Boolean _isGovernmentAgency) {
			this.isGovernmentAgency = _isGovernmentAgency == null ? null : _isGovernmentAgency;
			return this;
		}
		
		@RosettaAttribute("country")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("country")
		@Override
		public Organisation.OrganisationBuilder setCountry(CountryEnum _country) {
			this.country = _country == null ? null : _country;
			return this;
		}
		
		@Override
		public Organisation build() {
			return new Organisation.OrganisationImpl(this);
		}
		
		@Override
		public Organisation.OrganisationBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Organisation.OrganisationBuilder prune() {
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
		public Organisation.OrganisationBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Organisation.OrganisationBuilder o = (Organisation.OrganisationBuilder) other;
			
			
			merger.mergeBasic(getName(), o.getName(), this::setName);
			merger.mergeBasic(getIsGovernmentAgency(), o.getIsGovernmentAgency(), this::setIsGovernmentAgency);
			merger.mergeBasic(getCountry(), o.getCountry(), this::setCountry);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Organisation _that = getType().cast(o);
		
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
			return "OrganisationBuilder {" +
				"name=" + this.name + ", " +
				"isGovernmentAgency=" + this.isGovernmentAgency + ", " +
				"country=" + this.country +
			'}';
		}
	}
}
