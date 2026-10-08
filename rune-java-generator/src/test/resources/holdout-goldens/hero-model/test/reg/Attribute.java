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
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Objects;
import test.reg.meta.AttributeMeta;

import static java.util.Optional.ofNullable;

/**
 * @version test
 */
@RosettaDataType(value="Attribute", builder=Attribute.AttributeBuilderImpl.class, version="test")
@RuneDataType(value="Attribute", model="test", builder=Attribute.AttributeBuilderImpl.class, version="test")
public interface Attribute extends RosettaModelObject {

	AttributeMeta metaData = new AttributeMeta();

	/*********************** Getter Methods  ***********************/
	Integer getHeroInt();
	BigDecimal getHeroNumber();
	ZonedDateTime getHeroZonedDateTime();
	LocalTime getHeroTime();

	/*********************** Build Methods  ***********************/
	Attribute build();
	
	Attribute.AttributeBuilder toBuilder();
	
	static Attribute.AttributeBuilder builder() {
		return new Attribute.AttributeBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Attribute> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Attribute> getType() {
		return Attribute.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("heroInt"), Integer.class, getHeroInt(), this);
		processor.processBasic(path.newSubPath("heroNumber"), BigDecimal.class, getHeroNumber(), this);
		processor.processBasic(path.newSubPath("heroZonedDateTime"), ZonedDateTime.class, getHeroZonedDateTime(), this);
		processor.processBasic(path.newSubPath("heroTime"), LocalTime.class, getHeroTime(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface AttributeBuilder extends Attribute, RosettaModelObjectBuilder {
		Attribute.AttributeBuilder setHeroInt(Integer heroInt);
		Attribute.AttributeBuilder setHeroNumber(BigDecimal heroNumber);
		Attribute.AttributeBuilder setHeroZonedDateTime(ZonedDateTime heroZonedDateTime);
		Attribute.AttributeBuilder setHeroTime(LocalTime heroTime);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("heroInt"), Integer.class, getHeroInt(), this);
			processor.processBasic(path.newSubPath("heroNumber"), BigDecimal.class, getHeroNumber(), this);
			processor.processBasic(path.newSubPath("heroZonedDateTime"), ZonedDateTime.class, getHeroZonedDateTime(), this);
			processor.processBasic(path.newSubPath("heroTime"), LocalTime.class, getHeroTime(), this);
		}
		

		Attribute.AttributeBuilder prune();
	}

	/*********************** Immutable Implementation of Attribute  ***********************/
	class AttributeImpl implements Attribute {
		private final Integer heroInt;
		private final BigDecimal heroNumber;
		private final ZonedDateTime heroZonedDateTime;
		private final LocalTime heroTime;
		
		protected AttributeImpl(Attribute.AttributeBuilder builder) {
			this.heroInt = builder.getHeroInt();
			this.heroNumber = builder.getHeroNumber();
			this.heroZonedDateTime = builder.getHeroZonedDateTime();
			this.heroTime = builder.getHeroTime();
		}
		
		@Override
		@RosettaAttribute("heroInt")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("heroInt")
		public Integer getHeroInt() {
			return heroInt;
		}
		
		@Override
		@RosettaAttribute("heroNumber")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("heroNumber")
		public BigDecimal getHeroNumber() {
			return heroNumber;
		}
		
		@Override
		@RosettaAttribute("heroZonedDateTime")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("heroZonedDateTime")
		public ZonedDateTime getHeroZonedDateTime() {
			return heroZonedDateTime;
		}
		
		@Override
		@RosettaAttribute("heroTime")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("heroTime")
		public LocalTime getHeroTime() {
			return heroTime;
		}
		
		@Override
		public Attribute build() {
			return this;
		}
		
		@Override
		public Attribute.AttributeBuilder toBuilder() {
			Attribute.AttributeBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Attribute.AttributeBuilder builder) {
			ofNullable(getHeroInt()).ifPresent(builder::setHeroInt);
			ofNullable(getHeroNumber()).ifPresent(builder::setHeroNumber);
			ofNullable(getHeroZonedDateTime()).ifPresent(builder::setHeroZonedDateTime);
			ofNullable(getHeroTime()).ifPresent(builder::setHeroTime);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Attribute _that = getType().cast(o);
		
			if (!Objects.equals(heroInt, _that.getHeroInt())) return false;
			if (!Objects.equals(heroNumber, _that.getHeroNumber())) return false;
			if (!Objects.equals(heroZonedDateTime, _that.getHeroZonedDateTime())) return false;
			if (!Objects.equals(heroTime, _that.getHeroTime())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (heroInt != null ? heroInt.hashCode() : 0);
			_result = 31 * _result + (heroNumber != null ? heroNumber.hashCode() : 0);
			_result = 31 * _result + (heroZonedDateTime != null ? heroZonedDateTime.hashCode() : 0);
			_result = 31 * _result + (heroTime != null ? heroTime.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Attribute {" +
				"heroInt=" + this.heroInt + ", " +
				"heroNumber=" + this.heroNumber + ", " +
				"heroZonedDateTime=" + this.heroZonedDateTime + ", " +
				"heroTime=" + this.heroTime +
			'}';
		}
	}

	/*********************** Builder Implementation of Attribute  ***********************/
	class AttributeBuilderImpl implements Attribute.AttributeBuilder {
	
		protected Integer heroInt;
		protected BigDecimal heroNumber;
		protected ZonedDateTime heroZonedDateTime;
		protected LocalTime heroTime;
		
		@Override
		@RosettaAttribute("heroInt")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("heroInt")
		public Integer getHeroInt() {
			return heroInt;
		}
		
		@Override
		@RosettaAttribute("heroNumber")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("heroNumber")
		public BigDecimal getHeroNumber() {
			return heroNumber;
		}
		
		@Override
		@RosettaAttribute("heroZonedDateTime")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("heroZonedDateTime")
		public ZonedDateTime getHeroZonedDateTime() {
			return heroZonedDateTime;
		}
		
		@Override
		@RosettaAttribute("heroTime")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("heroTime")
		public LocalTime getHeroTime() {
			return heroTime;
		}
		
		@RosettaAttribute("heroInt")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("heroInt")
		@Override
		public Attribute.AttributeBuilder setHeroInt(Integer _heroInt) {
			this.heroInt = _heroInt == null ? null : _heroInt;
			return this;
		}
		
		@RosettaAttribute("heroNumber")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("heroNumber")
		@Override
		public Attribute.AttributeBuilder setHeroNumber(BigDecimal _heroNumber) {
			this.heroNumber = _heroNumber == null ? null : _heroNumber;
			return this;
		}
		
		@RosettaAttribute("heroZonedDateTime")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("heroZonedDateTime")
		@Override
		public Attribute.AttributeBuilder setHeroZonedDateTime(ZonedDateTime _heroZonedDateTime) {
			this.heroZonedDateTime = _heroZonedDateTime == null ? null : _heroZonedDateTime;
			return this;
		}
		
		@RosettaAttribute("heroTime")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("heroTime")
		@Override
		public Attribute.AttributeBuilder setHeroTime(LocalTime _heroTime) {
			this.heroTime = _heroTime == null ? null : _heroTime;
			return this;
		}
		
		@Override
		public Attribute build() {
			return new Attribute.AttributeImpl(this);
		}
		
		@Override
		public Attribute.AttributeBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Attribute.AttributeBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getHeroInt()!=null) return true;
			if (getHeroNumber()!=null) return true;
			if (getHeroZonedDateTime()!=null) return true;
			if (getHeroTime()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Attribute.AttributeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Attribute.AttributeBuilder o = (Attribute.AttributeBuilder) other;
			
			
			merger.mergeBasic(getHeroInt(), o.getHeroInt(), this::setHeroInt);
			merger.mergeBasic(getHeroNumber(), o.getHeroNumber(), this::setHeroNumber);
			merger.mergeBasic(getHeroZonedDateTime(), o.getHeroZonedDateTime(), this::setHeroZonedDateTime);
			merger.mergeBasic(getHeroTime(), o.getHeroTime(), this::setHeroTime);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Attribute _that = getType().cast(o);
		
			if (!Objects.equals(heroInt, _that.getHeroInt())) return false;
			if (!Objects.equals(heroNumber, _that.getHeroNumber())) return false;
			if (!Objects.equals(heroZonedDateTime, _that.getHeroZonedDateTime())) return false;
			if (!Objects.equals(heroTime, _that.getHeroTime())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (heroInt != null ? heroInt.hashCode() : 0);
			_result = 31 * _result + (heroNumber != null ? heroNumber.hashCode() : 0);
			_result = 31 * _result + (heroZonedDateTime != null ? heroZonedDateTime.hashCode() : 0);
			_result = 31 * _result + (heroTime != null ? heroTime.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "AttributeBuilder {" +
				"heroInt=" + this.heroInt + ", " +
				"heroNumber=" + this.heroNumber + ", " +
				"heroZonedDateTime=" + this.heroZonedDateTime + ", " +
				"heroTime=" + this.heroTime +
			'}';
		}
	}
}
