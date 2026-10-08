package chaos.s24.a3hub.p2;

import chaos.s24.a3hub.p2.meta.C24OutMeta;
import chaos.s24.a3hub.p2.metafields.ReferenceWithMetaC24Keyed;
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
import com.rosetta.model.metafields.FieldWithMetaVoid;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * The output type: a Void leaf, a Void meta leaf, a Void list leaf, a keyed reference, a plain leaf and a nested carrier for the two-hop seat.
 * @version 1.0.0
 */
@RosettaDataType(value="C24Out", builder=C24Out.C24OutBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C24Out", model="chaos", builder=C24Out.C24OutBuilderImpl.class, version="1.0.0")
public interface C24Out extends RosettaModelObject {

	C24OutMeta metaData = new C24OutMeta();

	/*********************** Getter Methods  ***********************/
	Void getV();
	FieldWithMetaVoid getVm();
	List<Void> getVs();
	ReferenceWithMetaC24Keyed getKref();
	String getS();
	C24Carrier getCar();

	/*********************** Build Methods  ***********************/
	C24Out build();
	
	C24Out.C24OutBuilder toBuilder();
	
	static C24Out.C24OutBuilder builder() {
		return new C24Out.C24OutBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C24Out> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C24Out> getType() {
		return C24Out.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("v"), Void.class, getV(), this);
		processRosetta(path.newSubPath("vm"), processor, FieldWithMetaVoid.class, getVm());
		processor.processBasic(path.newSubPath("vs"), Void.class, getVs(), this);
		processRosetta(path.newSubPath("kref"), processor, ReferenceWithMetaC24Keyed.class, getKref());
		processor.processBasic(path.newSubPath("s"), String.class, getS(), this);
		processRosetta(path.newSubPath("car"), processor, C24Carrier.class, getCar());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C24OutBuilder extends C24Out, RosettaModelObjectBuilder {
		FieldWithMetaVoid.FieldWithMetaVoidBuilder getOrCreateVm();
		@Override
		FieldWithMetaVoid.FieldWithMetaVoidBuilder getVm();
		ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder getOrCreateKref();
		@Override
		ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder getKref();
		C24Carrier.C24CarrierBuilder getOrCreateCar();
		@Override
		C24Carrier.C24CarrierBuilder getCar();
		C24Out.C24OutBuilder setV(Void v);
		C24Out.C24OutBuilder setVm(FieldWithMetaVoid vm);
		C24Out.C24OutBuilder setVmValue(Void vm);
		C24Out.C24OutBuilder addVs(Void vs);
		C24Out.C24OutBuilder addVs(Void vs, int idx);
		C24Out.C24OutBuilder addVs(List<Void> vs);
		C24Out.C24OutBuilder setVs(List<Void> vs);
		C24Out.C24OutBuilder setKref(ReferenceWithMetaC24Keyed kref);
		C24Out.C24OutBuilder setKrefValue(C24Keyed kref);
		C24Out.C24OutBuilder setS(String s);
		C24Out.C24OutBuilder setCar(C24Carrier car);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("v"), Void.class, getV(), this);
			processRosetta(path.newSubPath("vm"), processor, FieldWithMetaVoid.FieldWithMetaVoidBuilder.class, getVm());
			processor.processBasic(path.newSubPath("vs"), Void.class, getVs(), this);
			processRosetta(path.newSubPath("kref"), processor, ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder.class, getKref());
			processor.processBasic(path.newSubPath("s"), String.class, getS(), this);
			processRosetta(path.newSubPath("car"), processor, C24Carrier.C24CarrierBuilder.class, getCar());
		}
		

		C24Out.C24OutBuilder prune();
	}

	/*********************** Immutable Implementation of C24Out  ***********************/
	class C24OutImpl implements C24Out {
		private final Void v;
		private final FieldWithMetaVoid vm;
		private final List<Void> vs;
		private final ReferenceWithMetaC24Keyed kref;
		private final String s;
		private final C24Carrier car;
		
		protected C24OutImpl(C24Out.C24OutBuilder builder) {
			this.v = builder.getV();
			this.vm = ofNullable(builder.getVm()).map(f->f.build()).orElse(null);
			this.vs = ofNullable(builder.getVs()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.kref = ofNullable(builder.getKref()).map(f->f.build()).orElse(null);
			this.s = builder.getS();
			this.car = ofNullable(builder.getCar()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("v")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("v")
		public Void getV() {
			return v;
		}
		
		@Override
		@RosettaAttribute("vm")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("vm")
		public FieldWithMetaVoid getVm() {
			return vm;
		}
		
		@Override
		@RosettaAttribute("vs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("vs")
		public List<Void> getVs() {
			return vs;
		}
		
		@Override
		@RosettaAttribute("kref")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("kref")
		public ReferenceWithMetaC24Keyed getKref() {
			return kref;
		}
		
		@Override
		@RosettaAttribute("s")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("s")
		public String getS() {
			return s;
		}
		
		@Override
		@RosettaAttribute("car")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("car")
		public C24Carrier getCar() {
			return car;
		}
		
		@Override
		public C24Out build() {
			return this;
		}
		
		@Override
		public C24Out.C24OutBuilder toBuilder() {
			C24Out.C24OutBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C24Out.C24OutBuilder builder) {
			ofNullable(getV()).ifPresent(builder::setV);
			ofNullable(getVm()).ifPresent(builder::setVm);
			ofNullable(getVs()).ifPresent(builder::setVs);
			ofNullable(getKref()).ifPresent(builder::setKref);
			ofNullable(getS()).ifPresent(builder::setS);
			ofNullable(getCar()).ifPresent(builder::setCar);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C24Out _that = getType().cast(o);
		
			if (!Objects.equals(v, _that.getV())) return false;
			if (!Objects.equals(vm, _that.getVm())) return false;
			if (!ListEquals.listEquals(vs, _that.getVs())) return false;
			if (!Objects.equals(kref, _that.getKref())) return false;
			if (!Objects.equals(s, _that.getS())) return false;
			if (!Objects.equals(car, _that.getCar())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (v != null ? v.hashCode() : 0);
			_result = 31 * _result + (vm != null ? vm.hashCode() : 0);
			_result = 31 * _result + (vs != null ? vs.hashCode() : 0);
			_result = 31 * _result + (kref != null ? kref.hashCode() : 0);
			_result = 31 * _result + (s != null ? s.hashCode() : 0);
			_result = 31 * _result + (car != null ? car.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C24Out {" +
				"v=" + this.v + ", " +
				"vm=" + this.vm + ", " +
				"vs=" + this.vs + ", " +
				"kref=" + this.kref + ", " +
				"s=" + this.s + ", " +
				"car=" + this.car +
			'}';
		}
	}

	/*********************** Builder Implementation of C24Out  ***********************/
	class C24OutBuilderImpl implements C24Out.C24OutBuilder {
	
		protected Void v;
		protected FieldWithMetaVoid.FieldWithMetaVoidBuilder vm;
		protected List<Void> vs = new ArrayList<>();
		protected ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder kref;
		protected String s;
		protected C24Carrier.C24CarrierBuilder car;
		
		@Override
		@RosettaAttribute("v")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("v")
		public Void getV() {
			return v;
		}
		
		@Override
		@RosettaAttribute("vm")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("vm")
		public FieldWithMetaVoid.FieldWithMetaVoidBuilder getVm() {
			return vm;
		}
		
		@Override
		public FieldWithMetaVoid.FieldWithMetaVoidBuilder getOrCreateVm() {
			FieldWithMetaVoid.FieldWithMetaVoidBuilder result;
			if (vm!=null) {
				result = vm;
			}
			else {
				result = vm = FieldWithMetaVoid.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("vs")
		@Accessor(AccessorType.GETTER)
		@Multi
		@RuneAttribute("vs")
		public List<Void> getVs() {
			return vs;
		}
		
		@Override
		@RosettaAttribute("kref")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("kref")
		public ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder getKref() {
			return kref;
		}
		
		@Override
		public ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder getOrCreateKref() {
			ReferenceWithMetaC24Keyed.ReferenceWithMetaC24KeyedBuilder result;
			if (kref!=null) {
				result = kref;
			}
			else {
				result = kref = ReferenceWithMetaC24Keyed.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("s")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("s")
		public String getS() {
			return s;
		}
		
		@Override
		@RosettaAttribute("car")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("car")
		public C24Carrier.C24CarrierBuilder getCar() {
			return car;
		}
		
		@Override
		public C24Carrier.C24CarrierBuilder getOrCreateCar() {
			C24Carrier.C24CarrierBuilder result;
			if (car!=null) {
				result = car;
			}
			else {
				result = car = C24Carrier.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("v")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("v")
		@Override
		public C24Out.C24OutBuilder setV(Void _v) {
			this.v = _v == null ? null : _v;
			return this;
		}
		
		@RosettaAttribute("vm")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("vm")
		@Override
		public C24Out.C24OutBuilder setVm(FieldWithMetaVoid _vm) {
			this.vm = _vm == null ? null : _vm.toBuilder();
			return this;
		}
		
		@Override
		public C24Out.C24OutBuilder setVmValue(Void _vm) {
			this.getOrCreateVm().setValue(_vm);
			return this;
		}
		
		@RosettaAttribute("vs")
		@Accessor(AccessorType.ADDER)
		@Multi
		@RuneAttribute("vs")
		@Override
		public C24Out.C24OutBuilder addVs(Void _vs) {
			if (_vs != null) {
				this.vs.add(_vs);
			}
			return this;
		}
		
		@Override
		public C24Out.C24OutBuilder addVs(Void _vs, int idx) {
			getIndex(this.vs, idx, () -> _vs);
			return this;
		}
		
		@Override
		public C24Out.C24OutBuilder addVs(List<Void> vss) {
			if (vss != null) {
				for (final Void toAdd : vss) {
					this.vs.add(toAdd);
				}
			}
			return this;
		}
		
		@RosettaAttribute("vs")
		@Accessor(AccessorType.SETTER)
		@Multi
		@RuneAttribute("vs")
		@Override
		public C24Out.C24OutBuilder setVs(List<Void> vss) {
			if (vss == null) {
				this.vs = new ArrayList<>();
			} else {
				this.vs = vss.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@RosettaAttribute("kref")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("kref")
		@Override
		public C24Out.C24OutBuilder setKref(ReferenceWithMetaC24Keyed _kref) {
			this.kref = _kref == null ? null : _kref.toBuilder();
			return this;
		}
		
		@Override
		public C24Out.C24OutBuilder setKrefValue(C24Keyed _kref) {
			this.getOrCreateKref().setValue(_kref);
			return this;
		}
		
		@RosettaAttribute("s")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("s")
		@Override
		public C24Out.C24OutBuilder setS(String _s) {
			this.s = _s == null ? null : _s;
			return this;
		}
		
		@RosettaAttribute("car")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("car")
		@Override
		public C24Out.C24OutBuilder setCar(C24Carrier _car) {
			this.car = _car == null ? null : _car.toBuilder();
			return this;
		}
		
		@Override
		public C24Out build() {
			return new C24Out.C24OutImpl(this);
		}
		
		@Override
		public C24Out.C24OutBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C24Out.C24OutBuilder prune() {
			if (vm!=null && !vm.prune().hasData()) vm = null;
			if (kref!=null && !kref.prune().hasData()) kref = null;
			if (car!=null && !car.prune().hasData()) car = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getV()!=null) return true;
			if (getVm()!=null) return true;
			if (getVs()!=null && !getVs().isEmpty()) return true;
			if (getKref()!=null && getKref().hasData()) return true;
			if (getS()!=null) return true;
			if (getCar()!=null && getCar().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C24Out.C24OutBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C24Out.C24OutBuilder o = (C24Out.C24OutBuilder) other;
			
			merger.mergeRosetta(getVm(), o.getVm(), this::setVm);
			merger.mergeRosetta(getKref(), o.getKref(), this::setKref);
			merger.mergeRosetta(getCar(), o.getCar(), this::setCar);
			
			merger.mergeBasic(getV(), o.getV(), this::setV);
			merger.mergeBasic(getVs(), o.getVs(), (Consumer<Void>) this::addVs);
			merger.mergeBasic(getS(), o.getS(), this::setS);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C24Out _that = getType().cast(o);
		
			if (!Objects.equals(v, _that.getV())) return false;
			if (!Objects.equals(vm, _that.getVm())) return false;
			if (!ListEquals.listEquals(vs, _that.getVs())) return false;
			if (!Objects.equals(kref, _that.getKref())) return false;
			if (!Objects.equals(s, _that.getS())) return false;
			if (!Objects.equals(car, _that.getCar())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (v != null ? v.hashCode() : 0);
			_result = 31 * _result + (vm != null ? vm.hashCode() : 0);
			_result = 31 * _result + (vs != null ? vs.hashCode() : 0);
			_result = 31 * _result + (kref != null ? kref.hashCode() : 0);
			_result = 31 * _result + (s != null ? s.hashCode() : 0);
			_result = 31 * _result + (car != null ? car.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C24OutBuilder {" +
				"v=" + this.v + ", " +
				"vm=" + this.vm + ", " +
				"vs=" + this.vs + ", " +
				"kref=" + this.kref + ", " +
				"s=" + this.s + ", " +
				"car=" + this.car +
			'}';
		}
	}
}
