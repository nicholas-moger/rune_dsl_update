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
import test.reg.meta.AttributeReportMeta;

import static java.util.Optional.ofNullable;

/**
 * @version test
 */
@RosettaDataType(value="AttributeReport", builder=AttributeReport.AttributeReportBuilderImpl.class, version="test")
@RuneDataType(value="AttributeReport", model="test", builder=AttributeReport.AttributeReportBuilderImpl.class, version="test")
public interface AttributeReport extends RosettaModelObject {

	AttributeReportMeta metaData = new AttributeReportMeta();

	/*********************** Getter Methods  ***********************/
	/**
	 * Basic type - int
	 */
	Integer getHeroInt();
	/**
	 * Basic type - number
	 */
	BigDecimal getHeroNumber();
	/**
	 * Basic type - time
	 */
	LocalTime getHeroTime();
	/**
	 * Record type - zonedDateTime
	 */
	ZonedDateTime getHeroZonedDateTime();

	/*********************** Build Methods  ***********************/
	AttributeReport build();
	
	AttributeReport.AttributeReportBuilder toBuilder();
	
	static AttributeReport.AttributeReportBuilder builder() {
		return new AttributeReport.AttributeReportBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends AttributeReport> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends AttributeReport> getType() {
		return AttributeReport.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("heroInt"), Integer.class, getHeroInt(), this);
		processor.processBasic(path.newSubPath("heroNumber"), BigDecimal.class, getHeroNumber(), this);
		processor.processBasic(path.newSubPath("heroTime"), LocalTime.class, getHeroTime(), this);
		processor.processBasic(path.newSubPath("heroZonedDateTime"), ZonedDateTime.class, getHeroZonedDateTime(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface AttributeReportBuilder extends AttributeReport, RosettaModelObjectBuilder {
		AttributeReport.AttributeReportBuilder setHeroInt(Integer heroInt);
		AttributeReport.AttributeReportBuilder setHeroNumber(BigDecimal heroNumber);
		AttributeReport.AttributeReportBuilder setHeroTime(LocalTime heroTime);
		AttributeReport.AttributeReportBuilder setHeroZonedDateTime(ZonedDateTime heroZonedDateTime);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("heroInt"), Integer.class, getHeroInt(), this);
			processor.processBasic(path.newSubPath("heroNumber"), BigDecimal.class, getHeroNumber(), this);
			processor.processBasic(path.newSubPath("heroTime"), LocalTime.class, getHeroTime(), this);
			processor.processBasic(path.newSubPath("heroZonedDateTime"), ZonedDateTime.class, getHeroZonedDateTime(), this);
		}
		

		AttributeReport.AttributeReportBuilder prune();
	}

	/*********************** Immutable Implementation of AttributeReport  ***********************/
	class AttributeReportImpl implements AttributeReport {
		private final Integer heroInt;
		private final BigDecimal heroNumber;
		private final LocalTime heroTime;
		private final ZonedDateTime heroZonedDateTime;
		
		protected AttributeReportImpl(AttributeReport.AttributeReportBuilder builder) {
			this.heroInt = builder.getHeroInt();
			this.heroNumber = builder.getHeroNumber();
			this.heroTime = builder.getHeroTime();
			this.heroZonedDateTime = builder.getHeroZonedDateTime();
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
		@RosettaAttribute("heroTime")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("heroTime")
		public LocalTime getHeroTime() {
			return heroTime;
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
		public AttributeReport build() {
			return this;
		}
		
		@Override
		public AttributeReport.AttributeReportBuilder toBuilder() {
			AttributeReport.AttributeReportBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(AttributeReport.AttributeReportBuilder builder) {
			ofNullable(getHeroInt()).ifPresent(builder::setHeroInt);
			ofNullable(getHeroNumber()).ifPresent(builder::setHeroNumber);
			ofNullable(getHeroTime()).ifPresent(builder::setHeroTime);
			ofNullable(getHeroZonedDateTime()).ifPresent(builder::setHeroZonedDateTime);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			AttributeReport _that = getType().cast(o);
		
			if (!Objects.equals(heroInt, _that.getHeroInt())) return false;
			if (!Objects.equals(heroNumber, _that.getHeroNumber())) return false;
			if (!Objects.equals(heroTime, _that.getHeroTime())) return false;
			if (!Objects.equals(heroZonedDateTime, _that.getHeroZonedDateTime())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (heroInt != null ? heroInt.hashCode() : 0);
			_result = 31 * _result + (heroNumber != null ? heroNumber.hashCode() : 0);
			_result = 31 * _result + (heroTime != null ? heroTime.hashCode() : 0);
			_result = 31 * _result + (heroZonedDateTime != null ? heroZonedDateTime.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "AttributeReport {" +
				"heroInt=" + this.heroInt + ", " +
				"heroNumber=" + this.heroNumber + ", " +
				"heroTime=" + this.heroTime + ", " +
				"heroZonedDateTime=" + this.heroZonedDateTime +
			'}';
		}
	}

	/*********************** Builder Implementation of AttributeReport  ***********************/
	class AttributeReportBuilderImpl implements AttributeReport.AttributeReportBuilder {
	
		protected Integer heroInt;
		protected BigDecimal heroNumber;
		protected LocalTime heroTime;
		protected ZonedDateTime heroZonedDateTime;
		
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
		@RosettaAttribute("heroTime")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("heroTime")
		public LocalTime getHeroTime() {
			return heroTime;
		}
		
		@Override
		@RosettaAttribute("heroZonedDateTime")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("heroZonedDateTime")
		public ZonedDateTime getHeroZonedDateTime() {
			return heroZonedDateTime;
		}
		
		@RosettaAttribute("heroInt")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("heroInt")
		@Override
		public AttributeReport.AttributeReportBuilder setHeroInt(Integer _heroInt) {
			this.heroInt = _heroInt == null ? null : _heroInt;
			return this;
		}
		
		@RosettaAttribute("heroNumber")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("heroNumber")
		@Override
		public AttributeReport.AttributeReportBuilder setHeroNumber(BigDecimal _heroNumber) {
			this.heroNumber = _heroNumber == null ? null : _heroNumber;
			return this;
		}
		
		@RosettaAttribute("heroTime")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("heroTime")
		@Override
		public AttributeReport.AttributeReportBuilder setHeroTime(LocalTime _heroTime) {
			this.heroTime = _heroTime == null ? null : _heroTime;
			return this;
		}
		
		@RosettaAttribute("heroZonedDateTime")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("heroZonedDateTime")
		@Override
		public AttributeReport.AttributeReportBuilder setHeroZonedDateTime(ZonedDateTime _heroZonedDateTime) {
			this.heroZonedDateTime = _heroZonedDateTime == null ? null : _heroZonedDateTime;
			return this;
		}
		
		@Override
		public AttributeReport build() {
			return new AttributeReport.AttributeReportImpl(this);
		}
		
		@Override
		public AttributeReport.AttributeReportBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public AttributeReport.AttributeReportBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getHeroInt()!=null) return true;
			if (getHeroNumber()!=null) return true;
			if (getHeroTime()!=null) return true;
			if (getHeroZonedDateTime()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public AttributeReport.AttributeReportBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			AttributeReport.AttributeReportBuilder o = (AttributeReport.AttributeReportBuilder) other;
			
			
			merger.mergeBasic(getHeroInt(), o.getHeroInt(), this::setHeroInt);
			merger.mergeBasic(getHeroNumber(), o.getHeroNumber(), this::setHeroNumber);
			merger.mergeBasic(getHeroTime(), o.getHeroTime(), this::setHeroTime);
			merger.mergeBasic(getHeroZonedDateTime(), o.getHeroZonedDateTime(), this::setHeroZonedDateTime);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			AttributeReport _that = getType().cast(o);
		
			if (!Objects.equals(heroInt, _that.getHeroInt())) return false;
			if (!Objects.equals(heroNumber, _that.getHeroNumber())) return false;
			if (!Objects.equals(heroTime, _that.getHeroTime())) return false;
			if (!Objects.equals(heroZonedDateTime, _that.getHeroZonedDateTime())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (heroInt != null ? heroInt.hashCode() : 0);
			_result = 31 * _result + (heroNumber != null ? heroNumber.hashCode() : 0);
			_result = 31 * _result + (heroTime != null ? heroTime.hashCode() : 0);
			_result = 31 * _result + (heroZonedDateTime != null ? heroZonedDateTime.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "AttributeReportBuilder {" +
				"heroInt=" + this.heroInt + ", " +
				"heroNumber=" + this.heroNumber + ", " +
				"heroTime=" + this.heroTime + ", " +
				"heroZonedDateTime=" + this.heroZonedDateTime +
			'}';
		}
	}
}
