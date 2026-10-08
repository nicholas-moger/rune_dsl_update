package test.reg;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
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
import com.rosetta.model.lib.records.Date;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import test.reg.meta.PersonMeta;

import static java.util.Optional.ofNullable;

/**
 * @version test
 */
@RosettaDataType(value="Person", builder=Person.PersonBuilderImpl.class, version="test")
@RuneDataType(value="Person", model="test", builder=Person.PersonBuilderImpl.class, version="test")
public interface Person extends RosettaModelObject {

	PersonMeta metaData = new PersonMeta();

	/*********************** Getter Methods  ***********************/
	String getName();
	Date getDateOfBirth();
	CountryEnum getNationality();
	Boolean getHasSpecialAbilities();
	List<PowerEnum> getPowers();
	Attribute getAttribute();
	List<? extends Organisation> getOrganisations();

	/*********************** Build Methods  ***********************/
	Person build();
	
	Person.PersonBuilder toBuilder();
	
	static Person.PersonBuilder builder() {
		return new Person.PersonBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Person> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Person> getType() {
		return Person.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
		processor.processBasic(path.newSubPath("dateOfBirth"), Date.class, getDateOfBirth(), this);
		processor.processBasic(path.newSubPath("nationality"), CountryEnum.class, getNationality(), this);
		processor.processBasic(path.newSubPath("hasSpecialAbilities"), Boolean.class, getHasSpecialAbilities(), this);
		processor.processBasic(path.newSubPath("powers"), PowerEnum.class, getPowers(), this);
		processRosetta(path.newSubPath("attribute"), processor, Attribute.class, getAttribute());
		processRosetta(path.newSubPath("organisations"), processor, Organisation.class, getOrganisations());
	}
	

	/*********************** Builder Interface  ***********************/
	interface PersonBuilder extends Person, RosettaModelObjectBuilder {
		Attribute.AttributeBuilder getOrCreateAttribute();
		@Override
		Attribute.AttributeBuilder getAttribute();
		Organisation.OrganisationBuilder getOrCreateOrganisations(int index);
		@Override
		List<? extends Organisation.OrganisationBuilder> getOrganisations();
		Person.PersonBuilder setName(String name);
		Person.PersonBuilder setDateOfBirth(Date dateOfBirth);
		Person.PersonBuilder setNationality(CountryEnum nationality);
		Person.PersonBuilder setHasSpecialAbilities(Boolean hasSpecialAbilities);
		Person.PersonBuilder addPowers(PowerEnum powers);
		Person.PersonBuilder addPowers(PowerEnum powers, int idx);
		Person.PersonBuilder addPowers(List<PowerEnum> powers);
		Person.PersonBuilder setPowers(List<PowerEnum> powers);
		Person.PersonBuilder setAttribute(Attribute attribute);
		Person.PersonBuilder addOrganisations(Organisation organisations);
		Person.PersonBuilder addOrganisations(Organisation organisations, int idx);
		Person.PersonBuilder addOrganisations(List<? extends Organisation> organisations);
		Person.PersonBuilder setOrganisations(List<? extends Organisation> organisations);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("name"), String.class, getName(), this);
			processor.processBasic(path.newSubPath("dateOfBirth"), Date.class, getDateOfBirth(), this);
			processor.processBasic(path.newSubPath("nationality"), CountryEnum.class, getNationality(), this);
			processor.processBasic(path.newSubPath("hasSpecialAbilities"), Boolean.class, getHasSpecialAbilities(), this);
			processor.processBasic(path.newSubPath("powers"), PowerEnum.class, getPowers(), this);
			processRosetta(path.newSubPath("attribute"), processor, Attribute.AttributeBuilder.class, getAttribute());
			processRosetta(path.newSubPath("organisations"), processor, Organisation.OrganisationBuilder.class, getOrganisations());
		}
		

		Person.PersonBuilder prune();
	}

	/*********************** Immutable Implementation of Person  ***********************/
	class PersonImpl implements Person {
		private final String name;
		private final Date dateOfBirth;
		private final CountryEnum nationality;
		private final Boolean hasSpecialAbilities;
		private final List<PowerEnum> powers;
		private final Attribute attribute;
		private final List<? extends Organisation> organisations;
		
		protected PersonImpl(Person.PersonBuilder builder) {
			this.name = builder.getName();
			this.dateOfBirth = builder.getDateOfBirth();
			this.nationality = builder.getNationality();
			this.hasSpecialAbilities = builder.getHasSpecialAbilities();
			this.powers = ofNullable(builder.getPowers()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.attribute = ofNullable(builder.getAttribute()).map(f->f.build()).orElse(null);
			this.organisations = ofNullable(builder.getOrganisations()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
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
		@RosettaAttribute("dateOfBirth")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("dateOfBirth")
		public Date getDateOfBirth() {
			return dateOfBirth;
		}
		
		@Override
		@RosettaAttribute("nationality")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("nationality")
		public CountryEnum getNationality() {
			return nationality;
		}
		
		@Override
		@RosettaAttribute("hasSpecialAbilities")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("hasSpecialAbilities")
		public Boolean getHasSpecialAbilities() {
			return hasSpecialAbilities;
		}
		
		@Override
		@RosettaAttribute("powers")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("powers")
		public List<PowerEnum> getPowers() {
			return powers;
		}
		
		@Override
		@RosettaAttribute("attribute")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("attribute")
		public Attribute getAttribute() {
			return attribute;
		}
		
		@Override
		@RosettaAttribute("organisations")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("organisations")
		public List<? extends Organisation> getOrganisations() {
			return organisations;
		}
		
		@Override
		public Person build() {
			return this;
		}
		
		@Override
		public Person.PersonBuilder toBuilder() {
			Person.PersonBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Person.PersonBuilder builder) {
			ofNullable(getName()).ifPresent(builder::setName);
			ofNullable(getDateOfBirth()).ifPresent(builder::setDateOfBirth);
			ofNullable(getNationality()).ifPresent(builder::setNationality);
			ofNullable(getHasSpecialAbilities()).ifPresent(builder::setHasSpecialAbilities);
			ofNullable(getPowers()).ifPresent(builder::setPowers);
			ofNullable(getAttribute()).ifPresent(builder::setAttribute);
			ofNullable(getOrganisations()).ifPresent(builder::setOrganisations);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Person _that = getType().cast(o);
		
			if (!Objects.equals(name, _that.getName())) return false;
			if (!Objects.equals(dateOfBirth, _that.getDateOfBirth())) return false;
			if (!Objects.equals(nationality, _that.getNationality())) return false;
			if (!Objects.equals(hasSpecialAbilities, _that.getHasSpecialAbilities())) return false;
			if (!ListEquals.listEquals(powers, _that.getPowers())) return false;
			if (!Objects.equals(attribute, _that.getAttribute())) return false;
			if (!ListEquals.listEquals(organisations, _that.getOrganisations())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			_result = 31 * _result + (dateOfBirth != null ? dateOfBirth.hashCode() : 0);
			_result = 31 * _result + (nationality != null ? nationality.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (hasSpecialAbilities != null ? hasSpecialAbilities.hashCode() : 0);
			_result = 31 * _result + (powers != null ? powers.stream().map(Object::getClass).map(Class::getName).mapToInt(String::hashCode).sum() : 0);
			_result = 31 * _result + (attribute != null ? attribute.hashCode() : 0);
			_result = 31 * _result + (organisations != null ? organisations.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Person {" +
				"name=" + this.name + ", " +
				"dateOfBirth=" + this.dateOfBirth + ", " +
				"nationality=" + this.nationality + ", " +
				"hasSpecialAbilities=" + this.hasSpecialAbilities + ", " +
				"powers=" + this.powers + ", " +
				"attribute=" + this.attribute + ", " +
				"organisations=" + this.organisations +
			'}';
		}
	}

	/*********************** Builder Implementation of Person  ***********************/
	class PersonBuilderImpl implements Person.PersonBuilder {
	
		protected String name;
		protected Date dateOfBirth;
		protected CountryEnum nationality;
		protected Boolean hasSpecialAbilities;
		protected List<PowerEnum> powers = new ArrayList<>();
		protected Attribute.AttributeBuilder attribute;
		protected List<Organisation.OrganisationBuilder> organisations = new ArrayList<>();
		
		@Override
		@RosettaAttribute("name")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("name")
		public String getName() {
			return name;
		}
		
		@Override
		@RosettaAttribute("dateOfBirth")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("dateOfBirth")
		public Date getDateOfBirth() {
			return dateOfBirth;
		}
		
		@Override
		@RosettaAttribute("nationality")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("nationality")
		public CountryEnum getNationality() {
			return nationality;
		}
		
		@Override
		@RosettaAttribute("hasSpecialAbilities")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("hasSpecialAbilities")
		public Boolean getHasSpecialAbilities() {
			return hasSpecialAbilities;
		}
		
		@Override
		@RosettaAttribute("powers")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("powers")
		public List<PowerEnum> getPowers() {
			return powers;
		}
		
		@Override
		@RosettaAttribute("attribute")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("attribute")
		public Attribute.AttributeBuilder getAttribute() {
			return attribute;
		}
		
		@Override
		public Attribute.AttributeBuilder getOrCreateAttribute() {
			Attribute.AttributeBuilder result;
			if (attribute!=null) {
				result = attribute;
			}
			else {
				result = attribute = Attribute.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("organisations")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("organisations")
		public List<? extends Organisation.OrganisationBuilder> getOrganisations() {
			return organisations;
		}
		
		@Override
		public Organisation.OrganisationBuilder getOrCreateOrganisations(int index) {
			if (organisations==null) {
				this.organisations = new ArrayList<>();
			}
			return getIndex(organisations, index, () -> {
						Organisation.OrganisationBuilder newOrganisations = Organisation.builder();
						return newOrganisations;
					});
		}
		
		@RosettaAttribute("name")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("name")
		@Override
		public Person.PersonBuilder setName(String _name) {
			this.name = _name == null ? null : _name;
			return this;
		}
		
		@RosettaAttribute("dateOfBirth")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("dateOfBirth")
		@Override
		public Person.PersonBuilder setDateOfBirth(Date _dateOfBirth) {
			this.dateOfBirth = _dateOfBirth == null ? null : _dateOfBirth;
			return this;
		}
		
		@RosettaAttribute("nationality")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("nationality")
		@Override
		public Person.PersonBuilder setNationality(CountryEnum _nationality) {
			this.nationality = _nationality == null ? null : _nationality;
			return this;
		}
		
		@RosettaAttribute("hasSpecialAbilities")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("hasSpecialAbilities")
		@Override
		public Person.PersonBuilder setHasSpecialAbilities(Boolean _hasSpecialAbilities) {
			this.hasSpecialAbilities = _hasSpecialAbilities == null ? null : _hasSpecialAbilities;
			return this;
		}
		
		@RosettaAttribute("powers")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("powers")
		@Override
		public Person.PersonBuilder addPowers(PowerEnum _powers) {
			if (_powers != null) {
				this.powers.add(_powers);
			}
			return this;
		}
		
		@Override
		public Person.PersonBuilder addPowers(PowerEnum _powers, int idx) {
			getIndex(this.powers, idx, () -> _powers);
			return this;
		}
		
		@Override
		public Person.PersonBuilder addPowers(List<PowerEnum> powerss) {
			if (powerss != null) {
				for (final PowerEnum toAdd : powerss) {
					this.powers.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("powers")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("powers")
		@Override
		public Person.PersonBuilder setPowers(List<PowerEnum> powerss) {
			if (powerss == null) {
				this.powers = new ArrayList<>();
			} else {
				this.powers = powerss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("attribute")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("attribute")
		@Override
		public Person.PersonBuilder setAttribute(Attribute _attribute) {
			this.attribute = _attribute == null ? null : _attribute.toBuilder();
			return this;
		}
		
		@RosettaAttribute("organisations")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("organisations")
		@Override
		public Person.PersonBuilder addOrganisations(Organisation _organisations) {
			if (_organisations != null) {
				this.organisations.add(_organisations.toBuilder());
			}
			return this;
		}
		
		@Override
		public Person.PersonBuilder addOrganisations(Organisation _organisations, int idx) {
			getIndex(this.organisations, idx, () -> _organisations.toBuilder());
			return this;
		}
		
		@Override
		public Person.PersonBuilder addOrganisations(List<? extends Organisation> organisationss) {
			if (organisationss != null) {
				for (final Organisation toAdd : organisationss) {
					this.organisations.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@RosettaAttribute("organisations")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("organisations")
		@Override
		public Person.PersonBuilder setOrganisations(List<? extends Organisation> organisationss) {
			if (organisationss == null) {
				this.organisations = new ArrayList<>();
			} else {
				this.organisations = organisationss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Person build() {
			return new Person.PersonImpl(this);
		}
		
		@Override
		public Person.PersonBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Person.PersonBuilder prune() {
			if (attribute!=null && !attribute.prune().hasData()) attribute = null;
			organisations = organisations.stream().filter(b->b!=null).<Organisation.OrganisationBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getName()!=null) return true;
			if (getDateOfBirth()!=null) return true;
			if (getNationality()!=null) return true;
			if (getHasSpecialAbilities()!=null) return true;
			if (getPowers()!=null && !getPowers().isEmpty()) return true;
			if (getAttribute()!=null && getAttribute().hasData()) return true;
			if (getOrganisations()!=null && getOrganisations().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Person.PersonBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Person.PersonBuilder o = (Person.PersonBuilder) other;
			
			merger.mergeRosetta(getAttribute(), o.getAttribute(), this::setAttribute);
			merger.mergeRosetta(getOrganisations(), o.getOrganisations(), this::getOrCreateOrganisations);
			
			merger.mergeBasic(getName(), o.getName(), this::setName);
			merger.mergeBasic(getDateOfBirth(), o.getDateOfBirth(), this::setDateOfBirth);
			merger.mergeBasic(getNationality(), o.getNationality(), this::setNationality);
			merger.mergeBasic(getHasSpecialAbilities(), o.getHasSpecialAbilities(), this::setHasSpecialAbilities);
			merger.mergeBasic(getPowers(), o.getPowers(), (Consumer<PowerEnum>) this::addPowers);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Person _that = getType().cast(o);
		
			if (!Objects.equals(name, _that.getName())) return false;
			if (!Objects.equals(dateOfBirth, _that.getDateOfBirth())) return false;
			if (!Objects.equals(nationality, _that.getNationality())) return false;
			if (!Objects.equals(hasSpecialAbilities, _that.getHasSpecialAbilities())) return false;
			if (!ListEquals.listEquals(powers, _that.getPowers())) return false;
			if (!Objects.equals(attribute, _that.getAttribute())) return false;
			if (!ListEquals.listEquals(organisations, _that.getOrganisations())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (name != null ? name.hashCode() : 0);
			_result = 31 * _result + (dateOfBirth != null ? dateOfBirth.hashCode() : 0);
			_result = 31 * _result + (nationality != null ? nationality.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (hasSpecialAbilities != null ? hasSpecialAbilities.hashCode() : 0);
			_result = 31 * _result + (powers != null ? powers.stream().map(Object::getClass).map(Class::getName).mapToInt(String::hashCode).sum() : 0);
			_result = 31 * _result + (attribute != null ? attribute.hashCode() : 0);
			_result = 31 * _result + (organisations != null ? organisations.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "PersonBuilder {" +
				"name=" + this.name + ", " +
				"dateOfBirth=" + this.dateOfBirth + ", " +
				"nationality=" + this.nationality + ", " +
				"hasSpecialAbilities=" + this.hasSpecialAbilities + ", " +
				"powers=" + this.powers + ", " +
				"attribute=" + this.attribute + ", " +
				"organisations=" + this.organisations +
			'}';
		}
	}
}
