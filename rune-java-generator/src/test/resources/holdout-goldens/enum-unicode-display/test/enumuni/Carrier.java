package test.enumuni;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import test.enumuni.meta.CarrierMeta;

import static java.util.Optional.ofNullable;

/**
 * An attribute of each enum so every enum is reachable from a type.
 * @version 1.0.0
 */
@RosettaDataType(value="Carrier", builder=Carrier.CarrierBuilderImpl.class, version="1.0.0")
@RuneDataType(value="Carrier", model="test", builder=Carrier.CarrierBuilderImpl.class, version="1.0.0")
public interface Carrier extends RosettaModelObject {

	CarrierMeta metaData = new CarrierMeta();

	/*********************** Getter Methods  ***********************/
	ProbeEnum getP();
	MixedEnum getM();
	DefinedEnum getD();
	List<AllDisplayedEnum> getA();

	/*********************** Build Methods  ***********************/
	Carrier build();
	
	Carrier.CarrierBuilder toBuilder();
	
	static Carrier.CarrierBuilder builder() {
		return new Carrier.CarrierBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Carrier> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Carrier> getType() {
		return Carrier.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("p"), ProbeEnum.class, getP(), this);
		processor.processBasic(path.newSubPath("m"), MixedEnum.class, getM(), this);
		processor.processBasic(path.newSubPath("d"), DefinedEnum.class, getD(), this);
		processor.processBasic(path.newSubPath("a"), AllDisplayedEnum.class, getA(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface CarrierBuilder extends Carrier, RosettaModelObjectBuilder {
		Carrier.CarrierBuilder setP(ProbeEnum p);
		Carrier.CarrierBuilder setM(MixedEnum m);
		Carrier.CarrierBuilder setD(DefinedEnum d);
		Carrier.CarrierBuilder addA(AllDisplayedEnum a);
		Carrier.CarrierBuilder addA(AllDisplayedEnum a, int idx);
		Carrier.CarrierBuilder addA(List<AllDisplayedEnum> a);
		Carrier.CarrierBuilder setA(List<AllDisplayedEnum> a);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("p"), ProbeEnum.class, getP(), this);
			processor.processBasic(path.newSubPath("m"), MixedEnum.class, getM(), this);
			processor.processBasic(path.newSubPath("d"), DefinedEnum.class, getD(), this);
			processor.processBasic(path.newSubPath("a"), AllDisplayedEnum.class, getA(), this);
		}
		

		Carrier.CarrierBuilder prune();
	}

	/*********************** Immutable Implementation of Carrier  ***********************/
	class CarrierImpl implements Carrier {
		private final ProbeEnum p;
		private final MixedEnum m;
		private final DefinedEnum d;
		private final List<AllDisplayedEnum> a;
		
		protected CarrierImpl(Carrier.CarrierBuilder builder) {
			this.p = builder.getP();
			this.m = builder.getM();
			this.d = builder.getD();
			this.a = ofNullable(builder.getA()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("p")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("p")
		public ProbeEnum getP() {
			return p;
		}
		
		@Override
		@RosettaAttribute("m")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("m")
		public MixedEnum getM() {
			return m;
		}
		
		@Override
		@RosettaAttribute("d")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("d")
		public DefinedEnum getD() {
			return d;
		}
		
		@Override
		@RosettaAttribute("a")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("a")
		public List<AllDisplayedEnum> getA() {
			return a;
		}
		
		@Override
		public Carrier build() {
			return this;
		}
		
		@Override
		public Carrier.CarrierBuilder toBuilder() {
			Carrier.CarrierBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Carrier.CarrierBuilder builder) {
			ofNullable(getP()).ifPresent(builder::setP);
			ofNullable(getM()).ifPresent(builder::setM);
			ofNullable(getD()).ifPresent(builder::setD);
			ofNullable(getA()).ifPresent(builder::setA);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Carrier _that = getType().cast(o);
		
			if (!Objects.equals(p, _that.getP())) return false;
			if (!Objects.equals(m, _that.getM())) return false;
			if (!Objects.equals(d, _that.getD())) return false;
			if (!ListEquals.listEquals(a, _that.getA())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (p != null ? p.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (m != null ? m.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (d != null ? d.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (a != null ? a.stream().map(Object::getClass).map(Class::getName).mapToInt(String::hashCode).sum() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Carrier {" +
				"p=" + this.p + ", " +
				"m=" + this.m + ", " +
				"d=" + this.d + ", " +
				"a=" + this.a +
			'}';
		}
	}

	/*********************** Builder Implementation of Carrier  ***********************/
	class CarrierBuilderImpl implements Carrier.CarrierBuilder {
	
		protected ProbeEnum p;
		protected MixedEnum m;
		protected DefinedEnum d;
		protected List<AllDisplayedEnum> a = new ArrayList<>();
		
		@Override
		@RosettaAttribute("p")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("p")
		public ProbeEnum getP() {
			return p;
		}
		
		@Override
		@RosettaAttribute("m")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("m")
		public MixedEnum getM() {
			return m;
		}
		
		@Override
		@RosettaAttribute("d")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("d")
		public DefinedEnum getD() {
			return d;
		}
		
		@Override
		@RosettaAttribute("a")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("a")
		public List<AllDisplayedEnum> getA() {
			return a;
		}
		
		@RosettaAttribute("p")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("p")
		@Override
		public Carrier.CarrierBuilder setP(ProbeEnum _p) {
			this.p = _p == null ? null : _p;
			return this;
		}
		
		@RosettaAttribute("m")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("m")
		@Override
		public Carrier.CarrierBuilder setM(MixedEnum _m) {
			this.m = _m == null ? null : _m;
			return this;
		}
		
		@RosettaAttribute("d")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("d")
		@Override
		public Carrier.CarrierBuilder setD(DefinedEnum _d) {
			this.d = _d == null ? null : _d;
			return this;
		}
		
		@RosettaAttribute("a")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("a")
		@Override
		public Carrier.CarrierBuilder addA(AllDisplayedEnum _a) {
			if (_a != null) {
				this.a.add(_a);
			}
			return this;
		}
		
		@Override
		public Carrier.CarrierBuilder addA(AllDisplayedEnum _a, int idx) {
			getIndex(this.a, idx, () -> _a);
			return this;
		}
		
		@Override
		public Carrier.CarrierBuilder addA(List<AllDisplayedEnum> as) {
			if (as != null) {
				for (final AllDisplayedEnum toAdd : as) {
					this.a.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("a")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("a")
		@Override
		public Carrier.CarrierBuilder setA(List<AllDisplayedEnum> as) {
			if (as == null) {
				this.a = new ArrayList<>();
			} else {
				this.a = as.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public Carrier build() {
			return new Carrier.CarrierImpl(this);
		}
		
		@Override
		public Carrier.CarrierBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Carrier.CarrierBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getP()!=null) return true;
			if (getM()!=null) return true;
			if (getD()!=null) return true;
			if (getA()!=null && !getA().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Carrier.CarrierBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Carrier.CarrierBuilder o = (Carrier.CarrierBuilder) other;
			
			
			merger.mergeBasic(getP(), o.getP(), this::setP);
			merger.mergeBasic(getM(), o.getM(), this::setM);
			merger.mergeBasic(getD(), o.getD(), this::setD);
			merger.mergeBasic(getA(), o.getA(), (Consumer<AllDisplayedEnum>) this::addA);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Carrier _that = getType().cast(o);
		
			if (!Objects.equals(p, _that.getP())) return false;
			if (!Objects.equals(m, _that.getM())) return false;
			if (!Objects.equals(d, _that.getD())) return false;
			if (!ListEquals.listEquals(a, _that.getA())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (p != null ? p.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (m != null ? m.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (d != null ? d.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (a != null ? a.stream().map(Object::getClass).map(Class::getName).mapToInt(String::hashCode).sum() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "CarrierBuilder {" +
				"p=" + this.p + ", " +
				"m=" + this.m + ", " +
				"d=" + this.d + ", " +
				"a=" + this.a +
			'}';
		}
	}
}
