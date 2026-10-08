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
import test.reg.meta.SokoviaAccordsReportMeta;

import static java.util.Optional.ofNullable;

/**
 * @version test
 */
@RosettaDataType(value="SokoviaAccordsReport", builder=SokoviaAccordsReport.SokoviaAccordsReportBuilderImpl.class, version="test")
@RuneDataType(value="SokoviaAccordsReport", model="test", builder=SokoviaAccordsReport.SokoviaAccordsReportBuilderImpl.class, version="test")
public interface SokoviaAccordsReport extends RosettaModelObject {

	SokoviaAccordsReportMeta metaData = new SokoviaAccordsReportMeta();

	/*********************** Getter Methods  ***********************/
	/**
	 * Basic type - string
	 */
	String getHeroName();
	/**
	 * Basic type - date
	 */
	Date getDateOfBirth();
	/**
	 * Enum type
	 */
	CountryEnum getNationality();
	/**
	 * Basic type - boolean
	 */
	Boolean getHasSpecialAbilities();
	/**
	 * Enum type - multi cardinality
	 */
	List<PowerEnum> getPowers();
	/**
	 * Nested report
	 */
	AttributeReport getAttribute();
	/**
	 * Repeatable rule
	 */
	List<? extends OrganisationReport> getOrganisations();
	/**
	 * Not modelled
	 */
	String getNotModelled();

	/*********************** Build Methods  ***********************/
	SokoviaAccordsReport build();
	
	SokoviaAccordsReport.SokoviaAccordsReportBuilder toBuilder();
	
	static SokoviaAccordsReport.SokoviaAccordsReportBuilder builder() {
		return new SokoviaAccordsReport.SokoviaAccordsReportBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends SokoviaAccordsReport> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends SokoviaAccordsReport> getType() {
		return SokoviaAccordsReport.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("heroName"), String.class, getHeroName(), this);
		processor.processBasic(path.newSubPath("dateOfBirth"), Date.class, getDateOfBirth(), this);
		processor.processBasic(path.newSubPath("nationality"), CountryEnum.class, getNationality(), this);
		processor.processBasic(path.newSubPath("hasSpecialAbilities"), Boolean.class, getHasSpecialAbilities(), this);
		processor.processBasic(path.newSubPath("powers"), PowerEnum.class, getPowers(), this);
		processRosetta(path.newSubPath("attribute"), processor, AttributeReport.class, getAttribute());
		processRosetta(path.newSubPath("organisations"), processor, OrganisationReport.class, getOrganisations());
		processor.processBasic(path.newSubPath("notModelled"), String.class, getNotModelled(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface SokoviaAccordsReportBuilder extends SokoviaAccordsReport, RosettaModelObjectBuilder {
		AttributeReport.AttributeReportBuilder getOrCreateAttribute();
		@Override
		AttributeReport.AttributeReportBuilder getAttribute();
		OrganisationReport.OrganisationReportBuilder getOrCreateOrganisations(int index);
		@Override
		List<? extends OrganisationReport.OrganisationReportBuilder> getOrganisations();
		SokoviaAccordsReport.SokoviaAccordsReportBuilder setHeroName(String heroName);
		SokoviaAccordsReport.SokoviaAccordsReportBuilder setDateOfBirth(Date dateOfBirth);
		SokoviaAccordsReport.SokoviaAccordsReportBuilder setNationality(CountryEnum nationality);
		SokoviaAccordsReport.SokoviaAccordsReportBuilder setHasSpecialAbilities(Boolean hasSpecialAbilities);
		SokoviaAccordsReport.SokoviaAccordsReportBuilder addPowers(PowerEnum powers);
		SokoviaAccordsReport.SokoviaAccordsReportBuilder addPowers(PowerEnum powers, int idx);
		SokoviaAccordsReport.SokoviaAccordsReportBuilder addPowers(List<PowerEnum> powers);
		SokoviaAccordsReport.SokoviaAccordsReportBuilder setPowers(List<PowerEnum> powers);
		SokoviaAccordsReport.SokoviaAccordsReportBuilder setAttribute(AttributeReport attribute);
		SokoviaAccordsReport.SokoviaAccordsReportBuilder addOrganisations(OrganisationReport organisations);
		SokoviaAccordsReport.SokoviaAccordsReportBuilder addOrganisations(OrganisationReport organisations, int idx);
		SokoviaAccordsReport.SokoviaAccordsReportBuilder addOrganisations(List<? extends OrganisationReport> organisations);
		SokoviaAccordsReport.SokoviaAccordsReportBuilder setOrganisations(List<? extends OrganisationReport> organisations);
		SokoviaAccordsReport.SokoviaAccordsReportBuilder setNotModelled(String notModelled);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("heroName"), String.class, getHeroName(), this);
			processor.processBasic(path.newSubPath("dateOfBirth"), Date.class, getDateOfBirth(), this);
			processor.processBasic(path.newSubPath("nationality"), CountryEnum.class, getNationality(), this);
			processor.processBasic(path.newSubPath("hasSpecialAbilities"), Boolean.class, getHasSpecialAbilities(), this);
			processor.processBasic(path.newSubPath("powers"), PowerEnum.class, getPowers(), this);
			processRosetta(path.newSubPath("attribute"), processor, AttributeReport.AttributeReportBuilder.class, getAttribute());
			processRosetta(path.newSubPath("organisations"), processor, OrganisationReport.OrganisationReportBuilder.class, getOrganisations());
			processor.processBasic(path.newSubPath("notModelled"), String.class, getNotModelled(), this);
		}
		

		SokoviaAccordsReport.SokoviaAccordsReportBuilder prune();
	}

	/*********************** Immutable Implementation of SokoviaAccordsReport  ***********************/
	class SokoviaAccordsReportImpl implements SokoviaAccordsReport {
		private final String heroName;
		private final Date dateOfBirth;
		private final CountryEnum nationality;
		private final Boolean hasSpecialAbilities;
		private final List<PowerEnum> powers;
		private final AttributeReport attribute;
		private final List<? extends OrganisationReport> organisations;
		private final String notModelled;
		
		protected SokoviaAccordsReportImpl(SokoviaAccordsReport.SokoviaAccordsReportBuilder builder) {
			this.heroName = builder.getHeroName();
			this.dateOfBirth = builder.getDateOfBirth();
			this.nationality = builder.getNationality();
			this.hasSpecialAbilities = builder.getHasSpecialAbilities();
			this.powers = ofNullable(builder.getPowers()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.attribute = ofNullable(builder.getAttribute()).map(f->f.build()).orElse(null);
			this.organisations = ofNullable(builder.getOrganisations()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
			this.notModelled = builder.getNotModelled();
		}
		
		@Override
		@RosettaAttribute("heroName")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("heroName")
		public String getHeroName() {
			return heroName;
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
		public AttributeReport getAttribute() {
			return attribute;
		}
		
		@Override
		@RosettaAttribute("organisations")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("organisations")
		public List<? extends OrganisationReport> getOrganisations() {
			return organisations;
		}
		
		@Override
		@RosettaAttribute("notModelled")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("notModelled")
		public String getNotModelled() {
			return notModelled;
		}
		
		@Override
		public SokoviaAccordsReport build() {
			return this;
		}
		
		@Override
		public SokoviaAccordsReport.SokoviaAccordsReportBuilder toBuilder() {
			SokoviaAccordsReport.SokoviaAccordsReportBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(SokoviaAccordsReport.SokoviaAccordsReportBuilder builder) {
			ofNullable(getHeroName()).ifPresent(builder::setHeroName);
			ofNullable(getDateOfBirth()).ifPresent(builder::setDateOfBirth);
			ofNullable(getNationality()).ifPresent(builder::setNationality);
			ofNullable(getHasSpecialAbilities()).ifPresent(builder::setHasSpecialAbilities);
			ofNullable(getPowers()).ifPresent(builder::setPowers);
			ofNullable(getAttribute()).ifPresent(builder::setAttribute);
			ofNullable(getOrganisations()).ifPresent(builder::setOrganisations);
			ofNullable(getNotModelled()).ifPresent(builder::setNotModelled);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			SokoviaAccordsReport _that = getType().cast(o);
		
			if (!Objects.equals(heroName, _that.getHeroName())) return false;
			if (!Objects.equals(dateOfBirth, _that.getDateOfBirth())) return false;
			if (!Objects.equals(nationality, _that.getNationality())) return false;
			if (!Objects.equals(hasSpecialAbilities, _that.getHasSpecialAbilities())) return false;
			if (!ListEquals.listEquals(powers, _that.getPowers())) return false;
			if (!Objects.equals(attribute, _that.getAttribute())) return false;
			if (!ListEquals.listEquals(organisations, _that.getOrganisations())) return false;
			if (!Objects.equals(notModelled, _that.getNotModelled())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (heroName != null ? heroName.hashCode() : 0);
			_result = 31 * _result + (dateOfBirth != null ? dateOfBirth.hashCode() : 0);
			_result = 31 * _result + (nationality != null ? nationality.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (hasSpecialAbilities != null ? hasSpecialAbilities.hashCode() : 0);
			_result = 31 * _result + (powers != null ? powers.stream().map(Object::getClass).map(Class::getName).mapToInt(String::hashCode).sum() : 0);
			_result = 31 * _result + (attribute != null ? attribute.hashCode() : 0);
			_result = 31 * _result + (organisations != null ? organisations.hashCode() : 0);
			_result = 31 * _result + (notModelled != null ? notModelled.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "SokoviaAccordsReport {" +
				"heroName=" + this.heroName + ", " +
				"dateOfBirth=" + this.dateOfBirth + ", " +
				"nationality=" + this.nationality + ", " +
				"hasSpecialAbilities=" + this.hasSpecialAbilities + ", " +
				"powers=" + this.powers + ", " +
				"attribute=" + this.attribute + ", " +
				"organisations=" + this.organisations + ", " +
				"notModelled=" + this.notModelled +
			'}';
		}
	}

	/*********************** Builder Implementation of SokoviaAccordsReport  ***********************/
	class SokoviaAccordsReportBuilderImpl implements SokoviaAccordsReport.SokoviaAccordsReportBuilder {
	
		protected String heroName;
		protected Date dateOfBirth;
		protected CountryEnum nationality;
		protected Boolean hasSpecialAbilities;
		protected List<PowerEnum> powers = new ArrayList<>();
		protected AttributeReport.AttributeReportBuilder attribute;
		protected List<OrganisationReport.OrganisationReportBuilder> organisations = new ArrayList<>();
		protected String notModelled;
		
		@Override
		@RosettaAttribute("heroName")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("heroName")
		public String getHeroName() {
			return heroName;
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
		public AttributeReport.AttributeReportBuilder getAttribute() {
			return attribute;
		}
		
		@Override
		public AttributeReport.AttributeReportBuilder getOrCreateAttribute() {
			AttributeReport.AttributeReportBuilder result;
			if (attribute!=null) {
				result = attribute;
			}
			else {
				result = attribute = AttributeReport.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("organisations")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("organisations")
		public List<? extends OrganisationReport.OrganisationReportBuilder> getOrganisations() {
			return organisations;
		}
		
		@Override
		public OrganisationReport.OrganisationReportBuilder getOrCreateOrganisations(int index) {
			if (organisations==null) {
				this.organisations = new ArrayList<>();
			}
			return getIndex(organisations, index, () -> {
						OrganisationReport.OrganisationReportBuilder newOrganisations = OrganisationReport.builder();
						return newOrganisations;
					});
		}
		
		@Override
		@RosettaAttribute("notModelled")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("notModelled")
		public String getNotModelled() {
			return notModelled;
		}
		
		@RosettaAttribute("heroName")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("heroName")
		@Override
		public SokoviaAccordsReport.SokoviaAccordsReportBuilder setHeroName(String _heroName) {
			this.heroName = _heroName == null ? null : _heroName;
			return this;
		}
		
		@RosettaAttribute("dateOfBirth")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("dateOfBirth")
		@Override
		public SokoviaAccordsReport.SokoviaAccordsReportBuilder setDateOfBirth(Date _dateOfBirth) {
			this.dateOfBirth = _dateOfBirth == null ? null : _dateOfBirth;
			return this;
		}
		
		@RosettaAttribute("nationality")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("nationality")
		@Override
		public SokoviaAccordsReport.SokoviaAccordsReportBuilder setNationality(CountryEnum _nationality) {
			this.nationality = _nationality == null ? null : _nationality;
			return this;
		}
		
		@RosettaAttribute("hasSpecialAbilities")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("hasSpecialAbilities")
		@Override
		public SokoviaAccordsReport.SokoviaAccordsReportBuilder setHasSpecialAbilities(Boolean _hasSpecialAbilities) {
			this.hasSpecialAbilities = _hasSpecialAbilities == null ? null : _hasSpecialAbilities;
			return this;
		}
		
		@RosettaAttribute("powers")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("powers")
		@Override
		public SokoviaAccordsReport.SokoviaAccordsReportBuilder addPowers(PowerEnum _powers) {
			if (_powers != null) {
				this.powers.add(_powers);
			}
			return this;
		}
		
		@Override
		public SokoviaAccordsReport.SokoviaAccordsReportBuilder addPowers(PowerEnum _powers, int idx) {
			getIndex(this.powers, idx, () -> _powers);
			return this;
		}
		
		@Override
		public SokoviaAccordsReport.SokoviaAccordsReportBuilder addPowers(List<PowerEnum> powerss) {
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
		public SokoviaAccordsReport.SokoviaAccordsReportBuilder setPowers(List<PowerEnum> powerss) {
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
		public SokoviaAccordsReport.SokoviaAccordsReportBuilder setAttribute(AttributeReport _attribute) {
			this.attribute = _attribute == null ? null : _attribute.toBuilder();
			return this;
		}
		
		@RosettaAttribute("organisations")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("organisations")
		@Override
		public SokoviaAccordsReport.SokoviaAccordsReportBuilder addOrganisations(OrganisationReport _organisations) {
			if (_organisations != null) {
				this.organisations.add(_organisations.toBuilder());
			}
			return this;
		}
		
		@Override
		public SokoviaAccordsReport.SokoviaAccordsReportBuilder addOrganisations(OrganisationReport _organisations, int idx) {
			getIndex(this.organisations, idx, () -> _organisations.toBuilder());
			return this;
		}
		
		@Override
		public SokoviaAccordsReport.SokoviaAccordsReportBuilder addOrganisations(List<? extends OrganisationReport> organisationss) {
			if (organisationss != null) {
				for (final OrganisationReport toAdd : organisationss) {
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
		public SokoviaAccordsReport.SokoviaAccordsReportBuilder setOrganisations(List<? extends OrganisationReport> organisationss) {
			if (organisationss == null) {
				this.organisations = new ArrayList<>();
			} else {
				this.organisations = organisationss.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("notModelled")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("notModelled")
		@Override
		public SokoviaAccordsReport.SokoviaAccordsReportBuilder setNotModelled(String _notModelled) {
			this.notModelled = _notModelled == null ? null : _notModelled;
			return this;
		}
		
		@Override
		public SokoviaAccordsReport build() {
			return new SokoviaAccordsReport.SokoviaAccordsReportImpl(this);
		}
		
		@Override
		public SokoviaAccordsReport.SokoviaAccordsReportBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public SokoviaAccordsReport.SokoviaAccordsReportBuilder prune() {
			if (attribute!=null && !attribute.prune().hasData()) attribute = null;
			organisations = organisations.stream().filter(b->b!=null).<OrganisationReport.OrganisationReportBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getHeroName()!=null) return true;
			if (getDateOfBirth()!=null) return true;
			if (getNationality()!=null) return true;
			if (getHasSpecialAbilities()!=null) return true;
			if (getPowers()!=null && !getPowers().isEmpty()) return true;
			if (getAttribute()!=null && getAttribute().hasData()) return true;
			if (getOrganisations()!=null && getOrganisations().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			if (getNotModelled()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public SokoviaAccordsReport.SokoviaAccordsReportBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			SokoviaAccordsReport.SokoviaAccordsReportBuilder o = (SokoviaAccordsReport.SokoviaAccordsReportBuilder) other;
			
			merger.mergeRosetta(getAttribute(), o.getAttribute(), this::setAttribute);
			merger.mergeRosetta(getOrganisations(), o.getOrganisations(), this::getOrCreateOrganisations);
			
			merger.mergeBasic(getHeroName(), o.getHeroName(), this::setHeroName);
			merger.mergeBasic(getDateOfBirth(), o.getDateOfBirth(), this::setDateOfBirth);
			merger.mergeBasic(getNationality(), o.getNationality(), this::setNationality);
			merger.mergeBasic(getHasSpecialAbilities(), o.getHasSpecialAbilities(), this::setHasSpecialAbilities);
			merger.mergeBasic(getPowers(), o.getPowers(), (Consumer<PowerEnum>) this::addPowers);
			merger.mergeBasic(getNotModelled(), o.getNotModelled(), this::setNotModelled);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			SokoviaAccordsReport _that = getType().cast(o);
		
			if (!Objects.equals(heroName, _that.getHeroName())) return false;
			if (!Objects.equals(dateOfBirth, _that.getDateOfBirth())) return false;
			if (!Objects.equals(nationality, _that.getNationality())) return false;
			if (!Objects.equals(hasSpecialAbilities, _that.getHasSpecialAbilities())) return false;
			if (!ListEquals.listEquals(powers, _that.getPowers())) return false;
			if (!Objects.equals(attribute, _that.getAttribute())) return false;
			if (!ListEquals.listEquals(organisations, _that.getOrganisations())) return false;
			if (!Objects.equals(notModelled, _that.getNotModelled())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (heroName != null ? heroName.hashCode() : 0);
			_result = 31 * _result + (dateOfBirth != null ? dateOfBirth.hashCode() : 0);
			_result = 31 * _result + (nationality != null ? nationality.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (hasSpecialAbilities != null ? hasSpecialAbilities.hashCode() : 0);
			_result = 31 * _result + (powers != null ? powers.stream().map(Object::getClass).map(Class::getName).mapToInt(String::hashCode).sum() : 0);
			_result = 31 * _result + (attribute != null ? attribute.hashCode() : 0);
			_result = 31 * _result + (organisations != null ? organisations.hashCode() : 0);
			_result = 31 * _result + (notModelled != null ? notModelled.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "SokoviaAccordsReportBuilder {" +
				"heroName=" + this.heroName + ", " +
				"dateOfBirth=" + this.dateOfBirth + ", " +
				"nationality=" + this.nationality + ", " +
				"hasSpecialAbilities=" + this.hasSpecialAbilities + ", " +
				"powers=" + this.powers + ", " +
				"attribute=" + this.attribute + ", " +
				"organisations=" + this.organisations + ", " +
				"notModelled=" + this.notModelled +
			'}';
		}
	}
}
