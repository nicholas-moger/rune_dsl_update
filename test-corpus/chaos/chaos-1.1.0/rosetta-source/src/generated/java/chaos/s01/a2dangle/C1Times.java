package chaos.s01.a2dangle;

import chaos.s01.a2dangle.meta.C1TimesMeta;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Every temporal record type at every cardinality form.
 * @version 1.0.0
 */
@RosettaDataType(value="C1Times", builder=C1Times.C1TimesBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C1Times", model="chaos", builder=C1Times.C1TimesBuilderImpl.class, version="1.0.0")
public interface C1Times extends RosettaModelObject {

	C1TimesMeta metaData = new C1TimesMeta();

	/*********************** Getter Methods  ***********************/
	Date getD();
	LocalTime getT();
	LocalDateTime getDt();
	ZonedDateTime getZ();
	List<Date> getDs();
	LocalDateTime getStamp();

	/*********************** Build Methods  ***********************/
	C1Times build();
	
	C1Times.C1TimesBuilder toBuilder();
	
	static C1Times.C1TimesBuilder builder() {
		return new C1Times.C1TimesBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C1Times> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C1Times> getType() {
		return C1Times.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("d"), Date.class, getD(), this);
		processor.processBasic(path.newSubPath("t"), LocalTime.class, getT(), this);
		processor.processBasic(path.newSubPath("dt"), LocalDateTime.class, getDt(), this);
		processor.processBasic(path.newSubPath("z"), ZonedDateTime.class, getZ(), this);
		processor.processBasic(path.newSubPath("ds"), Date.class, getDs(), this);
		processor.processBasic(path.newSubPath("stamp"), LocalDateTime.class, getStamp(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C1TimesBuilder extends C1Times, RosettaModelObjectBuilder {
		C1Times.C1TimesBuilder setD(Date d);
		C1Times.C1TimesBuilder setT(LocalTime t);
		C1Times.C1TimesBuilder setDt(LocalDateTime dt);
		C1Times.C1TimesBuilder setZ(ZonedDateTime z);
		C1Times.C1TimesBuilder addDs(Date ds);
		C1Times.C1TimesBuilder addDs(Date ds, int idx);
		C1Times.C1TimesBuilder addDs(List<Date> ds);
		C1Times.C1TimesBuilder setDs(List<Date> ds);
		C1Times.C1TimesBuilder setStamp(LocalDateTime stamp);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("d"), Date.class, getD(), this);
			processor.processBasic(path.newSubPath("t"), LocalTime.class, getT(), this);
			processor.processBasic(path.newSubPath("dt"), LocalDateTime.class, getDt(), this);
			processor.processBasic(path.newSubPath("z"), ZonedDateTime.class, getZ(), this);
			processor.processBasic(path.newSubPath("ds"), Date.class, getDs(), this);
			processor.processBasic(path.newSubPath("stamp"), LocalDateTime.class, getStamp(), this);
		}
		

		C1Times.C1TimesBuilder prune();
	}

	/*********************** Immutable Implementation of C1Times  ***********************/
	class C1TimesImpl implements C1Times {
		private final Date d;
		private final LocalTime t;
		private final LocalDateTime dt;
		private final ZonedDateTime z;
		private final List<Date> ds;
		private final LocalDateTime stamp;
		
		protected C1TimesImpl(C1Times.C1TimesBuilder builder) {
			this.d = builder.getD();
			this.t = builder.getT();
			this.dt = builder.getDt();
			this.z = builder.getZ();
			this.ds = ofNullable(builder.getDs()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.stamp = builder.getStamp();
		}
		
		@Override
		@RosettaAttribute("d")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("d")
		public Date getD() {
			return d;
		}
		
		@Override
		@RosettaAttribute("t")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("t")
		public LocalTime getT() {
			return t;
		}
		
		@Override
		@RosettaAttribute("dt")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("dt")
		public LocalDateTime getDt() {
			return dt;
		}
		
		@Override
		@RosettaAttribute("z")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("z")
		public ZonedDateTime getZ() {
			return z;
		}
		
		@Override
		@RosettaAttribute("ds")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("ds")
		public List<Date> getDs() {
			return ds;
		}
		
		@Override
		@RosettaAttribute("stamp")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("stamp")
		public LocalDateTime getStamp() {
			return stamp;
		}
		
		@Override
		public C1Times build() {
			return this;
		}
		
		@Override
		public C1Times.C1TimesBuilder toBuilder() {
			C1Times.C1TimesBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C1Times.C1TimesBuilder builder) {
			ofNullable(getD()).ifPresent(builder::setD);
			ofNullable(getT()).ifPresent(builder::setT);
			ofNullable(getDt()).ifPresent(builder::setDt);
			ofNullable(getZ()).ifPresent(builder::setZ);
			ofNullable(getDs()).ifPresent(builder::setDs);
			ofNullable(getStamp()).ifPresent(builder::setStamp);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C1Times _that = getType().cast(o);
		
			if (!Objects.equals(d, _that.getD())) return false;
			if (!Objects.equals(t, _that.getT())) return false;
			if (!Objects.equals(dt, _that.getDt())) return false;
			if (!Objects.equals(z, _that.getZ())) return false;
			if (!ListEquals.listEquals(ds, _that.getDs())) return false;
			if (!Objects.equals(stamp, _that.getStamp())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (d != null ? d.hashCode() : 0);
			_result = 31 * _result + (t != null ? t.hashCode() : 0);
			_result = 31 * _result + (dt != null ? dt.hashCode() : 0);
			_result = 31 * _result + (z != null ? z.hashCode() : 0);
			_result = 31 * _result + (ds != null ? ds.hashCode() : 0);
			_result = 31 * _result + (stamp != null ? stamp.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C1Times {" +
				"d=" + this.d + ", " +
				"t=" + this.t + ", " +
				"dt=" + this.dt + ", " +
				"z=" + this.z + ", " +
				"ds=" + this.ds + ", " +
				"stamp=" + this.stamp +
			'}';
		}
	}

	/*********************** Builder Implementation of C1Times  ***********************/
	class C1TimesBuilderImpl implements C1Times.C1TimesBuilder {
	
		protected Date d;
		protected LocalTime t;
		protected LocalDateTime dt;
		protected ZonedDateTime z;
		protected List<Date> ds = new ArrayList<>();
		protected LocalDateTime stamp;
		
		@Override
		@RosettaAttribute("d")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("d")
		public Date getD() {
			return d;
		}
		
		@Override
		@RosettaAttribute("t")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("t")
		public LocalTime getT() {
			return t;
		}
		
		@Override
		@RosettaAttribute("dt")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("dt")
		public LocalDateTime getDt() {
			return dt;
		}
		
		@Override
		@RosettaAttribute("z")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("z")
		public ZonedDateTime getZ() {
			return z;
		}
		
		@Override
		@RosettaAttribute("ds")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("ds")
		public List<Date> getDs() {
			return ds;
		}
		
		@Override
		@RosettaAttribute("stamp")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("stamp")
		public LocalDateTime getStamp() {
			return stamp;
		}
		
		@RosettaAttribute("d")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("d")
		@Override
		public C1Times.C1TimesBuilder setD(Date _d) {
			this.d = _d == null ? null : _d;
			return this;
		}
		
		@RosettaAttribute("t")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("t")
		@Override
		public C1Times.C1TimesBuilder setT(LocalTime _t) {
			this.t = _t == null ? null : _t;
			return this;
		}
		
		@RosettaAttribute("dt")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("dt")
		@Override
		public C1Times.C1TimesBuilder setDt(LocalDateTime _dt) {
			this.dt = _dt == null ? null : _dt;
			return this;
		}
		
		@RosettaAttribute("z")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("z")
		@Override
		public C1Times.C1TimesBuilder setZ(ZonedDateTime _z) {
			this.z = _z == null ? null : _z;
			return this;
		}
		
		@RosettaAttribute("ds")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("ds")
		@Override
		public C1Times.C1TimesBuilder addDs(Date _ds) {
			if (_ds != null) {
				this.ds.add(_ds);
			}
			return this;
		}
		
		@Override
		public C1Times.C1TimesBuilder addDs(Date _ds, int idx) {
			getIndex(this.ds, idx, () -> _ds);
			return this;
		}
		
		@Override
		public C1Times.C1TimesBuilder addDs(List<Date> dss) {
			if (dss != null) {
				for (final Date toAdd : dss) {
					this.ds.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("ds")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("ds")
		@Override
		public C1Times.C1TimesBuilder setDs(List<Date> dss) {
			if (dss == null) {
				this.ds = new ArrayList<>();
			} else {
				this.ds = dss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("stamp")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("stamp")
		@Override
		public C1Times.C1TimesBuilder setStamp(LocalDateTime _stamp) {
			this.stamp = _stamp == null ? null : _stamp;
			return this;
		}
		
		@Override
		public C1Times build() {
			return new C1Times.C1TimesImpl(this);
		}
		
		@Override
		public C1Times.C1TimesBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C1Times.C1TimesBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getD()!=null) return true;
			if (getT()!=null) return true;
			if (getDt()!=null) return true;
			if (getZ()!=null) return true;
			if (getDs()!=null && !getDs().isEmpty()) return true;
			if (getStamp()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C1Times.C1TimesBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C1Times.C1TimesBuilder o = (C1Times.C1TimesBuilder) other;
			
			
			merger.mergeBasic(getD(), o.getD(), this::setD);
			merger.mergeBasic(getT(), o.getT(), this::setT);
			merger.mergeBasic(getDt(), o.getDt(), this::setDt);
			merger.mergeBasic(getZ(), o.getZ(), this::setZ);
			merger.mergeBasic(getDs(), o.getDs(), (Consumer<Date>) this::addDs);
			merger.mergeBasic(getStamp(), o.getStamp(), this::setStamp);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C1Times _that = getType().cast(o);
		
			if (!Objects.equals(d, _that.getD())) return false;
			if (!Objects.equals(t, _that.getT())) return false;
			if (!Objects.equals(dt, _that.getDt())) return false;
			if (!Objects.equals(z, _that.getZ())) return false;
			if (!ListEquals.listEquals(ds, _that.getDs())) return false;
			if (!Objects.equals(stamp, _that.getStamp())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (d != null ? d.hashCode() : 0);
			_result = 31 * _result + (t != null ? t.hashCode() : 0);
			_result = 31 * _result + (dt != null ? dt.hashCode() : 0);
			_result = 31 * _result + (z != null ? z.hashCode() : 0);
			_result = 31 * _result + (ds != null ? ds.hashCode() : 0);
			_result = 31 * _result + (stamp != null ? stamp.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C1TimesBuilder {" +
				"d=" + this.d + ", " +
				"t=" + this.t + ", " +
				"dt=" + this.dt + ", " +
				"z=" + this.z + ", " +
				"ds=" + this.ds + ", " +
				"stamp=" + this.stamp +
			'}';
		}
	}
}
